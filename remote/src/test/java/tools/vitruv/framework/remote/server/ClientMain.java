package tools.vitruv.framework.remote.server;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Scanner;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import tools.vitruv.change.correspondence.CorrespondencePackage;
import tools.vitruv.framework.remote.client.VitruvClient;
import tools.vitruv.framework.remote.client.impl.VitruvRemoteConnection;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewSelector;
import tools.vitruv.framework.views.ViewType;
import tools.vitruv.framework.views.changederivation.DefaultStateBasedChangeResolutionStrategy;
import tools.vitruv.methodologisttemplate.model.model.Component;
import tools.vitruv.methodologisttemplate.model.model.ModelFactory;
import tools.vitruv.methodologisttemplate.model.model.ModelPackage;
import tools.vitruv.methodologisttemplate.model.model.Router;
import tools.vitruv.methodologisttemplate.model.model.System;
import tools.vitruv.methodologisttemplate.model.model2.Model2Package;
import tools.vitruv.methodologisttemplate.model.model2.Root;

/**
 * Interactive manual test client for the Vitruv REST API server.
 * Instead of building a V-SUM in-process, it connects to a running {@link VitruvServer} and uses:
 * <ul>
 *   <li>{@link VitruvRemoteConnection} for model operations (open view, modify, commit view)
 *   <li>Plain HTTP for infrastructure operations (git commit, branch management, versioning)
 * </ul>
 *
 * <p>Usage (server must already be running via {@link ServerMain}):
 * <pre>
 *   mvn exec:java \
 *     -pl remote \
 *     -Dexec.classpathScope=test \
 *     -Dexec.mainClass=tools.vitruv.framework.remote.server.ClientMain \
 *     -Dexec.args="localhost 8080 C:/tmp/vitruv-client-temp"
 * </pre>
 *
 * <p>Arguments:
 * <ol>
 *   <li>{@code host}: server hostname (e.g. {@code localhost})
 *   <li>{@code port}: server port (e.g. {@code 8080})
 *   <li>{@code clientTempDir}: local directory for the JSON mapper's temporary files
 * </ol>
 */
public class ClientMain {

    private static VitruvClient vitruvClient;
    private static HttpClient httpClient;
    private static String baseUrl;
    private static Path clientTempDir;
    private static Scanner scanner;

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            java.lang.System.err.println("Usage: ClientMain <host> <port> <clientTempDir>");
            java.lang.System.err.println("Example: ClientMain localhost 8080 C:/tmp/vitruv-client-temp");
            java.lang.System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        clientTempDir = Paths.get(args[2]).toAbsolutePath();
        baseUrl = "http://" + host + ":" + port;
        scanner = new Scanner(java.lang.System.in);

        // EMF package registration required for standalone (non-OSGi) mode.
        // These are the same packages registered in ServerMain / AbstractServerIntegrationTest.
        CorrespondencePackage.eINSTANCE.eClass();
        ModelPackage.eINSTANCE.eClass();
        Model2Package.eINSTANCE.eClass();

        // Register a catch-all XMI resource factory so that ResourceSet.createResource()
        // succeeds for any file extension (.model, .model2, etc.) in standalone mode.
        // Without this, DefaultStateBasedChangeResolutionStrategy.getChangeSequenceForCreated()
        // calls createResource() and gets null back, causing a NullPointerException.
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put("*", new XMIResourceFactoryImpl());

        Files.createDirectories(clientTempDir);

        // Create the Java HTTP client
        httpClient = HttpClient.newHttpClient();

        // Create the Vitruvius remote connection (used for model view operations).
        // The clientTempDir is where the JsonMapper stores temporary resource files
        // during serialization/deserialization of EMF changes.
        vitruvClient = new VitruvRemoteConnection("http", host, port, clientTempDir);

        java.lang.System.out.println("VITRUVIUS SERVER - INTERACTIVE MANUAL TEST CLIENT");
        java.lang.System.out.println("Server: " + baseUrl);
        java.lang.System.out.println("Client temp: " + clientTempDir);
        java.lang.System.out.println();

        // Verify server is reachable before showing menu.
        checkServerHealth();
        showMainMenu();
    }

    
    // Menu
    private static void showMainMenu() {
        while (true) {
            java.lang.System.out.println("\nMAIN MENU");
            java.lang.System.out.println(" Model operations (via Vitruv view API):");
            java.lang.System.out.println("  1. Create System model");
            java.lang.System.out.println("  2. View current models");
            java.lang.System.out.println("  3. Add component to system");
            java.lang.System.out.println("  4. Add router to system");
            java.lang.System.out.println("  5. Rename component");
            java.lang.System.out.println("  6. Delete component");
            java.lang.System.out.println("  7. Delete all models");
            java.lang.System.out.println(" Infrastructure operations (via REST):");
            java.lang.System.out.println("  8. Commit to git");
            java.lang.System.out.println("  9. List commits");
            java.lang.System.out.println(" 10. List branches");
            java.lang.System.out.println(" 11. Create branch");
            java.lang.System.out.println(" 12. Switch branch");
            java.lang.System.out.println(" 13. Delete branch");
            java.lang.System.out.println(" 14. Create version tag");
            java.lang.System.out.println(" 15. List versions");
            java.lang.System.out.println(" 16. Rollback to version");
            java.lang.System.out.println("  0. Exit");
            java.lang.System.out.print("Choose option: ");

            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1"  -> createModels();
                    case "2"  -> viewModels();
                    case "3"  -> addComponent();
                    case "4"  -> addRouter();
                    case "5"  -> renameComponent();
                    case "6"  -> deleteComponent();
                    case "7"  -> deleteAllModels();
                    case "8"  -> commitToGit();
                    case "9"  -> listCommits();
                    case "10" -> listBranches();
                    case "11" -> createBranch();
                    case "12" -> switchBranch();
                    case "13" -> deleteBranch();
                    case "14" -> createVersion();
                    case "15" -> listVersions();
                    case "16" -> rollbackVersion();
                    case "0"  -> {
                        java.lang.System.out.println("Exiting.");
                        return;
                    }
                    default -> java.lang.System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                java.lang.System.err.println("Error: " + e.getMessage());
                e.printStackTrace(java.lang.System.err);
            }
        }
    }

    
    // Model operations: use VitruvRemoteConnection
    /**
     * Creates a new System model root on the server.
     *
     * <p>Equivalent to option 1 in ManualTest. The System is registered at
     * {@code example.model} on the server's repo root. The Model2Model2 reaction
     * fires automatically on the server and creates {@code example.model2}.
     */
    private static void createModels() {
        java.lang.System.out.println("\nCreating System model via Vitruv view API...");
        View rawView = openView(null);
        if (!rawView.getRootObjects(System.class).isEmpty()) {
            java.lang.System.out.println("System already exists. Delete it first (option 7).");
            return;
        }

        // Ask the server where to persist the model.
        // The URI must be a path that the SERVER can write to (its repo root).
        java.lang.System.out.print("Enter server-side model path (e.g. /tmp/vitruv-manual-test/example.model): ");
        String pathStr = scanner.nextLine().trim();
        if (pathStr.isEmpty()) {
            java.lang.System.out.println("Cancelled.");
            return;
        }

        CommittableView view = rawView.withChangeDerivingTrait(
                new DefaultStateBasedChangeResolutionStrategy());
        System system = ModelFactory.eINSTANCE.createSystem();
        view.registerRoot(system, URI.createFileURI(pathStr));
        view.commitChanges();

        java.lang.System.out.println("System model created successfully.");
        java.lang.System.out.println("Reaction should have created example.model2 automatically.");
    }

    /**
     * Opens a view and prints the current model state (System + Root).
     *
     * <p>Equivalent to option 2 in ManualTest.
     */
    private static void viewModels() {
        java.lang.System.out.println("\nCurrent Model State (via Vitruv view API):");

        View view = openView(null);
        Collection<System> systems = view.getRootObjects(System.class);
        Collection<Root> roots = view.getRootObjects(Root.class);

        if (systems.isEmpty() && roots.isEmpty()) {
            java.lang.System.out.println("No models found. Create them first (option 1).");
            return;
        }

        for (System system : systems) {
            java.lang.System.out.println("\nSystem (example.model):");
            if (system.getComponents().isEmpty()) {
                java.lang.System.out.println("  Components: (none)");
            } else {
                system.getComponents().forEach(c ->
                    java.lang.System.out.println("  - " + c.getName() + " [" + c.eClass().getName() + "]"));
            }
        }

        for (Root root : roots) {
            java.lang.System.out.println("\nRoot (example.model2): created by reaction:");
            if (root.getEntities().isEmpty()) {
                java.lang.System.out.println("  Entities: (none)");
            } else {
                root.getEntities().forEach(e ->
                    java.lang.System.out.println("  - " + e.getName()));
            }
        }
    }

    /**
     * Adds a named Component to the System via the view API.
     *
     * <p>The reaction propagates the new entity to Model2 automatically.
     */
    private static void addComponent() {
        java.lang.System.out.print("\nEnter component name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) return;

        CommittableView view = openView(System.class)
                .withChangeDerivingTrait(new DefaultStateBasedChangeResolutionStrategy());
        Collection<System> systems = view.getRootObjects(System.class);
        if (systems.isEmpty()) {
            java.lang.System.out.println("No system found. Create models first (option 1).");
            return;
        }

        Component component = ModelFactory.eINSTANCE.createComponent();
        component.setName(name);
        systems.iterator().next().getComponents().add(component);
        view.commitChanges();
        java.lang.System.out.println("Component '" + name + "' added. Reaction should update Model2.");
    }

    /**
     * Adds a named Router (a subtype of Component) to the System.
     */
    private static void addRouter() {
        java.lang.System.out.print("\nEnter router name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) return;

        CommittableView view = openView(System.class)
                .withChangeDerivingTrait(new DefaultStateBasedChangeResolutionStrategy());
        Collection<System> systems = view.getRootObjects(System.class);
        if (systems.isEmpty()) {
            java.lang.System.out.println("No system found. Create models first.");
            return;
        }

        Router router = ModelFactory.eINSTANCE.createRouter();
        router.setName(name);
        systems.iterator().next().getComponents().add(router);
        view.commitChanges();
        java.lang.System.out.println("Router '" + name + "' added.");
    }

    /**
     * Renames a component. First displays current components, then applies
     * the rename through a fresh committable view.
     */
    private static void renameComponent() {
        View readView = openView(System.class);
        Collection<System> systems = readView.getRootObjects(System.class);
        if (systems.isEmpty() || systems.iterator().next().getComponents().isEmpty()) {
            java.lang.System.out.println("No components found.");
            return;
        }
        System system = systems.iterator().next();
        java.lang.System.out.println("\nComponents:");
        for (int i = 0; i < system.getComponents().size(); i++) {
            java.lang.System.out.println("  " + (i + 1) + ". " + system.getComponents().get(i).getName());
        }
        java.lang.System.out.print("Select component number: ");
        int index = Integer.parseInt(scanner.nextLine().trim()) - 1;
        java.lang.System.out.print("New name: ");
        String newName = scanner.nextLine().trim();

        // Open a fresh committable view for the modification (the read view above
        // is a snapshot; modifying it would cause inconsistency).
        CommittableView writeView = openView(System.class)
                .withChangeDerivingTrait(new DefaultStateBasedChangeResolutionStrategy());
        writeView.getRootObjects(System.class).iterator().next()
                .getComponents().get(index).setName(newName);
        writeView.commitChanges();
        java.lang.System.out.println("Renamed to '" + newName + "'.");
    }

    /**
     * Deletes one component from the System by index.
     */
    private static void deleteComponent() {
        View readView = openView(System.class);
        Collection<System> systems = readView.getRootObjects(System.class);
        if (systems.isEmpty() || systems.iterator().next().getComponents().isEmpty()) {
            java.lang.System.out.println("No components found.");
            return;
        }
        System system = systems.iterator().next();
        java.lang.System.out.println("\nComponents:");
        for (int i = 0; i < system.getComponents().size(); i++) {
            java.lang.System.out.println("  " + (i + 1) + ". " + system.getComponents().get(i).getName());
        }
        java.lang.System.out.print("Select component number to delete: ");
        int index = Integer.parseInt(scanner.nextLine().trim()) - 1;
        String name = system.getComponents().get(index).getName();

        CommittableView writeView = openView(System.class)
                .withChangeDerivingTrait(new DefaultStateBasedChangeResolutionStrategy());
        writeView.getRootObjects(System.class).iterator().next().getComponents().remove(index);
        writeView.commitChanges();
        java.lang.System.out.println("Deleted '" + name + "'.");
    }

    /**
     * Deletes all model elements. Two-step: clear components first, then delete root.
     */
    private static void deleteAllModels() {
        java.lang.System.out.print("\nAre you sure? (yes/no): ");
        if (!scanner.nextLine().trim().equalsIgnoreCase("yes")) {
            java.lang.System.out.println("Cancelled.");
            return;
        }
        // Step 1: clear all components
        CommittableView view1 = openView(System.class)
                .withChangeDerivingTrait(new DefaultStateBasedChangeResolutionStrategy());
        Collection<System> systems = view1.getRootObjects(System.class);
        if (systems.isEmpty()) {
            java.lang.System.out.println("No models to delete.");
            return;
        }
        systems.forEach(s -> s.getComponents().clear());
        view1.commitChanges();

        // Step 2: delete the System root
        CommittableView view2 = openView(System.class)
                .withChangeDerivingTrait(new DefaultStateBasedChangeResolutionStrategy());
        view2.getRootObjects(System.class).forEach(s ->
                org.eclipse.emf.ecore.util.EcoreUtil.delete(s, true));
        view2.commitChanges();
        java.lang.System.out.println("All models deleted.");
    }

    
    // Infrastructure operations: plain HTTP REST calls
    /**
     * Commits all staged model files via {@code POST /vsum/commit}.
     *
     * <p>After making model changes through the view API (options 1–7), call
     * this to persist them to git so they survive branch switches.
     */
    private static void commitToGit() {
        java.lang.System.out.print("\nCommit message: ");
        String message = scanner.nextLine().trim();
        if (message.isEmpty()) {
            java.lang.System.out.println("Cancelled.");
            return;
        }
        String body = "{\"message\": \"" + escapeJson(message) + "\"}";
        String response = httpPost("/vsum/commit", body);
        java.lang.System.out.println("Commit result:\n" + prettyPrint(response));
    }

    /** Lists the last commits via {@code GET /vsum/commit}. */
    private static void listCommits() {
        String response = httpGet("/vsum/commit");
        java.lang.System.out.println("Commits:\n" + prettyPrint(response));
    }

    /** Lists all branches via {@code GET /vsum/branch}. */
    private static void listBranches() {
        String response = httpGet("/vsum/branch");
        java.lang.System.out.println("Branches:\n" + prettyPrint(response));
    }

    /**
     * Creates a new branch via {@code POST /vsum/branch}.
     *
     * <p>The new branch is forked from the current HEAD on the given parent branch.
     * Always commit your model changes before creating a branch so the new branch
     * starts from a clean, persisted state.
     */
    private static void createBranch() {
        java.lang.System.out.print("\nNew branch name: ");
        String name = scanner.nextLine().trim();
        java.lang.System.out.print("Fork from branch (e.g. master): ");
        String from = scanner.nextLine().trim();
        if (name.isEmpty() || from.isEmpty()) {
            java.lang.System.out.println("Cancelled.");
            return;
        }
        String body = "{\"name\": \"" + escapeJson(name) + "\", \"fromBranch\": \"" + escapeJson(from) + "\"}";
        String response = httpPost("/vsum/branch", body);
        java.lang.System.out.println("Branch created:\n" + prettyPrint(response));
    }

    /**
     * Switches the active branch via {@code POST /vsum/branch/switch}.
     *
     * <p>The server performs a git checkout AND reloads the in-memory V-SUM
     * from the checked-out files. After switching, model views will reflect the
     * new branch's state.
     */
    private static void switchBranch() {
        java.lang.System.out.print("\nSwitch to branch: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            java.lang.System.out.println("Cancelled.");
            return;
        }
        String body = "{\"name\": \"" + escapeJson(name) + "\"}";
        String response = httpPost("/vsum/branch/switch", body);
        java.lang.System.out.println("Switched to:\n" + prettyPrint(response));
        java.lang.System.out.println("Tip: open a view now (option 2) to see the reloaded model state.");
    }

    /** Deletes a branch via {@code DELETE /vsum/branch}. */
    private static void deleteBranch() {
        java.lang.System.out.print("\nBranch to delete: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            java.lang.System.out.println("Cancelled.");
            return;
        }
        String response = httpDelete("/vsum/branch", "Branch-Name", name);
        java.lang.System.out.println("Deleted: " + response);
    }

    /**
     * Creates a version tag (annotated git tag) via {@code POST /vsum/version}.
     *
     * <p>Always commit first: a version tags the current HEAD commit.
     */
    private static void createVersion() {
        java.lang.System.out.print("\nVersion ID (e.g. v1.0): ");
        String id = scanner.nextLine().trim();
        java.lang.System.out.print("Description: ");
        String desc = scanner.nextLine().trim();
        if (id.isEmpty()) {
            java.lang.System.out.println("Cancelled.");
            return;
        }
        String body = "{\"versionId\": \"" + escapeJson(id) + "\", \"description\": \"" + escapeJson(desc) + "\"}";
        String response = httpPost("/vsum/version", body);
        java.lang.System.out.println("Version created:\n" + prettyPrint(response));
    }

    /** Lists all versions (lightweight: id + description only). */
    private static void listVersions() {
        String response = httpGet("/vsum/version");
        java.lang.System.out.println("Versions:\n" + prettyPrint(response));
    }

    /**
     * Rolls back to a previously created version via {@code POST /vsum/version/rollback}.
     *
     * <p>The server restores the model files from the tagged commit and reloads the V-SUM.
     */
    private static void rollbackVersion() {
        java.lang.System.out.print("\nVersion ID to rollback to: ");
        String id = scanner.nextLine().trim();
        if (id.isEmpty()) {
            java.lang.System.out.println("Cancelled.");
            return;
        }
        java.lang.System.out.print("Confirm rollback to '" + id + "'? (yes/no): ");
        if (!scanner.nextLine().trim().equalsIgnoreCase("yes")) {
            java.lang.System.out.println("Cancelled.");
            return;
        }
        String response = httpPost("/vsum/version/rollback", null, "Version-Id", id);
        java.lang.System.out.println("Rollback result:\n" + prettyPrint(response));
        java.lang.System.out.println("Tip: open a view now (option 2) to verify the restored state.");
    }

    
    // View helper
    /**
     * Opens a remote view via the server's "default" view type, selecting all
     * elements that are instances of {@code rootTypeFilter} (or all elements
     * if {@code rootTypeFilter} is null).
     */
    @SuppressWarnings("unchecked")
    private static View openView(Class<?> rootTypeFilter) {
        Collection<ViewType<?>> types = vitruvClient.getViewTypes();
        if (types.isEmpty()) {
            throw new IllegalStateException(
                "Server returned no view types. Is the server running and configured correctly?");
        }
        ViewType<ViewSelector> defaultType =
            (ViewType<ViewSelector>) types.stream()
                .filter(t -> "default".equals(t.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("View type 'default' not found on server."));

        ViewSelector selector = vitruvClient.createSelector(defaultType);
        selector.getSelectableElements().forEach(e -> {
            boolean include = rootTypeFilter == null || rootTypeFilter.isInstance(e);
            selector.setSelected(e, include);
        });
        return selector.createView();
    }

    
    // HTTP helpers
    private static void checkServerHealth() {
        try {
            String body = httpGet("/health");
            java.lang.System.out.println("Server health: " + body);
        } catch (Exception e) {
            java.lang.System.err.println("WARNING: Could not reach server at " + baseUrl
                + ". Is it running?\n  " + e.getMessage());
        }
    }

    private static String httpGet(String path, String... headers) {
        try {
            HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + path))
                .GET();
            applyHeaders(req, headers);
            HttpResponse<String> resp = httpClient.send(req.build(),
                HttpResponse.BodyHandlers.ofString());
            return resp.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("GET " + path + " failed: " + e.getMessage(), e);
        }
    }

    private static String httpPost(String path, String body, String... headers) {
        try {
            String payload = body != null ? body : "";
            HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload));
            applyHeaders(req, headers);
            HttpResponse<String> resp = httpClient.send(req.build(),
                HttpResponse.BodyHandlers.ofString());
            return resp.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("POST " + path + " failed: " + e.getMessage(), e);
        }
    }

    private static String httpDelete(String path, String... headers) {
        try {
            HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + path))
                .DELETE();
            applyHeaders(req, headers);
            HttpResponse<String> resp = httpClient.send(req.build(),
                HttpResponse.BodyHandlers.ofString());
            return resp.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("DELETE " + path + " failed: " + e.getMessage(), e);
        }
    }

    private static void applyHeaders(HttpRequest.Builder req, String[] headers) {
        for (int i = 0; i + 1 < headers.length; i += 2) {
            req.header(headers[i], headers[i + 1]);
        }
    }

    
    // Formatting helpers
    /** Very simple JSON pretty-printer: one key-value pair per line. */
    private static String prettyPrint(String json) {
        if (json == null || json.isBlank()) return "(empty response)";
        // Already readable: just indent slightly
        return "  " + json.replace(",", ",\n  ").replace("{", "{\n  ").replace("}", "\n}");
    }

    /** Escapes double quotes and backslashes for embedding in a JSON string literal. */
    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

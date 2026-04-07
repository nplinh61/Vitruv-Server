package tools.vitruv.framework.remote.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import tools.vitruv.change.correspondence.CorrespondencePackage;
import tools.vitruv.change.interaction.UserInteractionFactory;
import tools.vitruv.framework.vsum.VirtualModelBuilder;
import tools.vitruv.framework.vsum.branch.BranchAwareVirtualModel;
import tools.vitruv.framework.vsum.branch.BranchManager;
import tools.vitruv.framework.vsum.internal.InternalVirtualModel;

/**
 * Launches a local Vitruv server for development and testing.
 *
 * <p>Starts the server on port 8080 backed by a {@link BranchAwareVirtualModel}
 * with no consistency specs (plain VSUM, sufficient for REST API testing).
 *
 * <p>Usage:
 * <pre>
 *   mvn exec:java \
 *     -pl remote \
 *     -Dexec.classpathScope=test \
 *     -Dexec.mainClass=tools.vitruv.framework.remote.server.ServerMain \
 *     -Dexec.args="<absolute-path-to-git-repo>"
 * </pre>
 *
 * <p>The path must point to an existing Git repository (contains a {@code .git} directory).
 */
public class ServerMain {

  public static void main(String[] args) throws IOException, InterruptedException {
    if (args.length < 1) {
      System.err.println("Usage: ServerMain <repo-root-path>");
      System.err.println("Example: ServerMain C:/Users/user/my-vitruvius-repo");
      System.exit(1);
    }

    Path repoRoot = Paths.get(args[0]).toAbsolutePath();

    if (!Files.isDirectory(repoRoot.resolve(".git"))) {
      System.err.println("Error: not a Git repository: " + repoRoot);
      System.exit(1);
    }

    // Required for standalone (non-OSGi) execution so EMF can resolve
    // correspondence model URIs when loading the V-SUM from disk.
    CorrespondencePackage.eINSTANCE.eClass();

    System.out.println("Initializing V-SUM at: " + repoRoot);

    InternalVirtualModel innerModel = new VirtualModelBuilder()
        .withStorageFolder(repoRoot)
        .withUserInteractorForResultProvider(
            UserInteractionFactory.instance.createPredefinedInteractionResultProvider(null))
        .buildAndInitialize();

    BranchAwareVirtualModel branchModel = new BranchAwareVirtualModel(repoRoot, innerModel);
    System.out.println("Active branch: " + branchModel.getActiveBranch());

    BranchManager branchManager = new BranchManager(repoRoot);

    VitruvServer server = new VitruvServer(() -> branchModel, branchManager);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("\nShutting down...");
      server.stop();
      branchModel.dispose();
    }));

    server.start();
    System.out.println("Vitruv Server running on http://localhost:8080");
    System.out.println("Press Ctrl+C to stop.");

    // Keep the main thread alive
    Thread.currentThread().join();
  }
}

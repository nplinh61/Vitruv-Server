package tools.vitruv.framework.remote.server.rest.endpoints.branch;

/**
 * Request body DTO for switching the active branch.
 *
 * <p>Expected JSON:
 * <pre>
 * {
 *   "name": "feature/my-feature"
 * }
 * </pre>
 */
public record SwitchBranchRequest(String name) {}

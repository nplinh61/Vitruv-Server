package tools.vitruv.framework.remote.server.rest.endpoints.branch;

/**
 * Request body DTO for creating a new branch.
 *
 * <p>Expected JSON:
 * <pre>
 * {
 *   "name": "feature/my-feature",
 *   "fromBranch": "main"
 * }
 * </pre>
 */
public record CreateBranchRequest(String name, String fromBranch) {}

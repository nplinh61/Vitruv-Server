package tools.vitruv.framework.remote.server.rest.endpoints.merge;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body DTO for merging a branch into the current branch.
 *
 * <p>Expected JSON:
 * <pre>
 * {
 *   "sourceBranch": "feature/my-feature",
 *   "deleteAfterMerge": false
 * }
 * </pre>
 *
 * <p>{@code deleteAfterMerge} is optional and defaults to {@code false}.
 */
public record MergeRequest(
    @JsonProperty("sourceBranch") String sourceBranch,
    @JsonProperty("deleteAfterMerge") boolean deleteAfterMerge) {}

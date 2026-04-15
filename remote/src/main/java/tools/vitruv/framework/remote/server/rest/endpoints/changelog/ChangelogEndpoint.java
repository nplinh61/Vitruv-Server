package tools.vitruv.framework.remote.server.rest.endpoints.changelog;

import com.fasterxml.jackson.core.JsonProcessingException;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.GetEndpoint;
import tools.vitruv.framework.vsum.branch.CommitManager;
import tools.vitruv.framework.vsum.branch.exception.BranchOperationException;
import tools.vitruv.framework.vsum.branch.storage.SemanticChangelogManager;

/**
 * {@code GET /vsum/changelog}
 *
 * <p>Returns the full semantic changelog document for a specific commit on a branch.
 * The branch name is passed via the {@code Branch-Name} header and the 7-character
 * short SHA via the {@code Commit-Sha} header.
 *
 * <p>Example request:
 * <pre>
 *   GET /vsum/changelog
 *   Branch-Name: feature/my-feature
 *   Commit-Sha: a1b2c3d
 * </pre>
 *
 * <p>Returns the full {@code ChangelogDocument} JSON including {@code fileChanges} with
 * per-element {@code SemanticChangeEntry} records and the {@code summary} block.
 *
 * <p>Returns {@code 405} if no changelog file was found for the given branch and SHA.
 */
public class ChangelogEndpoint implements GetEndpoint {

  private final CommitManager commitManager;
  private final JsonMapper mapper;

  /**
   * Creates a new {@link ChangelogEndpoint}.
   *
   * @param commitManager the commit manager used to read the changelog.
   * @param mapper the JSON mapper used to serialize the response.
   */
  public ChangelogEndpoint(CommitManager commitManager, JsonMapper mapper) {
    this.commitManager = commitManager;
    this.mapper = mapper;
  }

  @Override
  public String process(HttpWrapper wrapper) throws ServerHaltingException {
    String branch = wrapper.getRequestHeader(Header.BRANCH_NAME);
    String sha = wrapper.getRequestHeader(Header.COMMIT_SHA);
    if (branch == null || branch.isBlank()) {
      throw badRequest("Missing required header: " + Header.BRANCH_NAME);
    }
    if (sha == null || sha.isBlank()) {
      throw badRequest("Missing required header: " + Header.COMMIT_SHA);
    }
    // readChangelog() expects a 7-char short SHA; truncate if caller passes a full SHA.
    String shortSha = sha.length() > 7 ? sha.substring(0, 7) : sha;
    try {
      SemanticChangelogManager.ChangelogDocument doc = commitManager.readChangelog(branch, shortSha);
      if (doc == null) {
        throw notFound(
            "No changelog found for branch '" + branch + "', commit '" + shortSha + "'");
      }
      wrapper.setContentType(ContentType.APPLICATION_JSON);
      return mapper.serialize(doc);
    } catch (BranchOperationException | JsonProcessingException e) {
      throw internalServerError(e.getMessage());
    }
  }
}

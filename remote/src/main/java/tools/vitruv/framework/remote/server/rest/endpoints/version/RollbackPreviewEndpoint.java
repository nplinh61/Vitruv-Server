package tools.vitruv.framework.remote.server.rest.endpoints.version;

import com.fasterxml.jackson.core.JsonProcessingException;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.PostEndpoint;
import tools.vitruv.framework.vsum.versioning.VersioningException;
import tools.vitruv.framework.vsum.versioning.VersioningService;

/**
 * {@code POST /vsum/version/rollback/preview}
 *
 * <p>Previews a rollback to the given version without executing it. Returns the list of commits
 * that will be abandoned, files that will change, and whether uncommitted changes are present.
 * The version ID is passed via the {@code Version-Id} header.
 *
 * <p>This is step 1 of a two-step rollback. Call
 * {@code POST /vsum/version/rollback/confirm} to execute after reviewing the preview.
 *
 * <p>Example request:
 * <pre>
 *   POST /vsum/version/rollback/preview
 *   Version-Id: v1.0
 * </pre>
 *
 * <p>Returns {@code 405} if the version does not exist.
 */
public class RollbackPreviewEndpoint implements PostEndpoint {

  private final VersioningService versioningService;
  private final JsonMapper mapper;

  /**
   * Creates a new {@link RollbackPreviewEndpoint}.
   *
   * @param versioningService the versioning service used to compute the rollback preview.
   * @param mapper the JSON mapper used to serialize the response.
   */
  public RollbackPreviewEndpoint(VersioningService versioningService, JsonMapper mapper) {
    this.versioningService = versioningService;
    this.mapper = mapper;
  }

  @Override
  public String process(HttpWrapper wrapper) throws ServerHaltingException {
    String versionId = wrapper.getRequestHeader(Header.VERSION_ID);
    if (versionId == null || versionId.isBlank()) {
      throw badRequest("Missing required header: " + Header.VERSION_ID);
    }
    try {
      RollbackPreviewResponse response = RollbackPreviewResponse.from(
          versioningService.previewRollback(versionId));
      wrapper.setContentType(ContentType.APPLICATION_JSON);
      return mapper.serialize(response);
    } catch (VersioningException e) {
      throw notFound(e.getMessage());
    } catch (JsonProcessingException e) {
      throw internalServerError(e.getMessage());
    }
  }
}

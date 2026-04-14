package tools.vitruv.framework.remote.server.rest.endpoints.version;

import tools.vitruv.framework.remote.common.rest.constants.Header;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.DeleteEndpoint;
import tools.vitruv.framework.vsum.versioning.VersioningException;
import tools.vitruv.framework.vsum.versioning.VersioningService;

/**
 * {@code DELETE /vsum/version/detail}
 *
 * <p>Deletes an existing version: removes the Git tag and the metadata file.
 * The version ID is passed via the {@code Version-Id} request header.
 *
 * <p>Example request:
 * <pre>
 *   DELETE /vsum/version/detail
 *   Version-Id: v1.0
 * </pre>
 *
 * <p>Returns {@code 200 OK} with no body on success.
 * Returns {@code 405} if the version does not exist.
 */
public class DeleteVersionEndpoint implements DeleteEndpoint {

  private final VersioningService versioningService;

  /**
   * Creates a new {@link DeleteVersionEndpoint}.
   *
   * @param versioningService the versioning service used to delete the version.
   */
  public DeleteVersionEndpoint(VersioningService versioningService) {
    this.versioningService = versioningService;
  }

  @Override
  public String process(HttpWrapper wrapper) throws ServerHaltingException {
    String versionId = wrapper.getRequestHeader(Header.VERSION_ID);
    if (versionId == null || versionId.isBlank()) {
      throw badRequest("Missing required header: " + Header.VERSION_ID);
    }
    try {
      versioningService.deleteVersion(versionId);
      return null;
    } catch (VersioningException e) {
      throw notFound(e.getMessage());
    }
  }
}

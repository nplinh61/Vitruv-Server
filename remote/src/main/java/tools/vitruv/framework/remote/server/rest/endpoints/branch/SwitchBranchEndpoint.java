package tools.vitruv.framework.remote.server.rest.endpoints.branch;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.ContentType;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.PostEndpoint;
import tools.vitruv.framework.vsum.branch.BranchManager;
import tools.vitruv.framework.vsum.branch.exception.BranchOperationException;


/**
 * {@code POST /vsum/branch/switch}
 *
 * <p>Switches the active Git branch and reloads the V-SUM in place so that the in-memory
 * model state reflects the content of the newly checked-out branch.
 *
 * <p>Expected request body:
 * <pre>
 * {
 *   "name": "feature/my-feature"
 * }
 * </pre>
 *
 * <p>Returns the newly active branch as a JSON object:
 * <pre>
 * {
 *   "name": "feature/my-feature",
 *   "state": "ACTIVE",
 *   "parentBranch": "master",
 *   "createdAt": "2026-04-07T12:00:00",
 *   "lastModified": "2026-04-07T12:05:00"
 * }
 * </pre>
 */
public class SwitchBranchEndpoint implements PostEndpoint {

  private final BranchManager branchManager;
  private final JsonMapper mapper;

  /**
   * Creates a new {@link SwitchBranchEndpoint}.
   *
   * @param branchManager the branch manager used to perform the branch switch and reload the V-SUM.
   * @param mapper the JSON mapper used to deserialize the request and serialize the response.
   */
  public SwitchBranchEndpoint(BranchManager branchManager, JsonMapper mapper) {
    this.branchManager = branchManager;
    this.mapper = mapper;
  }

  /** Builds a diagnostic message from the full exception cause chain. */
  private static String buildCauseChain(Throwable t) {
    StringBuilder sb = new StringBuilder(t.getMessage());
    Throwable cause = t.getCause();
    while (cause != null) {
      sb.append(" | caused by: ").append(cause.getClass().getSimpleName())
          .append(": ").append(cause.getMessage());
      cause = cause.getCause();
    }
    return sb.toString();
  }

  @Override
  public String process(HttpWrapper wrapper) throws ServerHaltingException {
    try {
      String body = wrapper.getRequestBodyAsString();
      SwitchBranchRequest request = mapper.deserialize(body, SwitchBranchRequest.class);
      branchManager.switchBranch(request.name());
      BranchResponse response = BranchResponse.from(
          branchManager.listBranches().stream()
              .filter(b -> b.getName().equals(request.name()))
              .findFirst()
              .orElseThrow(() -> new BranchOperationException(
                  "Branch not found after switch: " + request.name())));
      wrapper.setContentType(ContentType.APPLICATION_JSON);
      return mapper.serialize(response);
    } catch (BranchOperationException | JsonProcessingException e) {
      throw internalServerError(buildCauseChain(e));
    } catch (IOException e) {
      throw internalServerError(buildCauseChain(e));
    }
  }
}

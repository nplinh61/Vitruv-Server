package tools.vitruv.framework.remote.server.rest.endpoints;

import java.util.ArrayList;
import java.util.List;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.common.rest.constants.EndpointPath;
import tools.vitruv.framework.remote.server.exception.ServerHaltingException;
import tools.vitruv.framework.remote.server.http.HttpWrapper;
import tools.vitruv.framework.remote.server.rest.DeleteEndpoint;
import tools.vitruv.framework.remote.server.rest.GetEndpoint;
import tools.vitruv.framework.remote.server.rest.PatchEndpoint;
import tools.vitruv.framework.remote.server.rest.PathEndointCollector;
import tools.vitruv.framework.remote.server.rest.PostEndpoint;
import tools.vitruv.framework.remote.server.rest.PutEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.branch.BranchStateEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.changelog.ChangelogEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.commit.CommitEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.commit.ListCommitsEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.merge.MergeEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.branch.BranchTopologyEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.branch.CreateBranchEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.branch.DeleteBranchEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.branch.ListBranchesEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.branch.SwitchBranchEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.version.CreateVersionBranchEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.version.CreateVersionEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.version.DeleteVersionEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.version.GetVersionEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.version.ListVersionsEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.version.RollbackConfirmEndpoint;
import tools.vitruv.framework.remote.server.rest.endpoints.version.RollbackPreviewEndpoint;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.branch.BranchManager;
import tools.vitruv.framework.vsum.branch.CommitManager;
import tools.vitruv.framework.vsum.branch.MergeManager;
import tools.vitruv.framework.vsum.versioning.VersioningService;

/** Provides all REST endpoints for the Vitruv server. */
public class EndpointsProvider {

  /**
   * Creates and returns all REST endpoints for the Vitruv server.
   *
   * @param virtualModel The virtual model to use.
   * @param mapper The JSON mapper to use.
   * @return A list of all REST endpoints.
   */
  public static List<PathEndointCollector> getAllEndpoints(
      VirtualModel virtualModel, JsonMapper mapper) {
    var defaultEndpoints = getDefaultEndpoints();

    List<PathEndointCollector> result = new ArrayList<>();
    result.add(
        new PathEndointCollector(
            EndpointPath.HEALTH,
            new HealthEndpoint(),
            defaultEndpoints.postEndpoint(),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));
    result.add(
        new PathEndointCollector(
            EndpointPath.IS_VIEW_CLOSED,
            new IsViewClosedEndpoint(),
            defaultEndpoints.postEndpoint(),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));
    result.add(
        new PathEndointCollector(
            EndpointPath.IS_VIEW_OUTDATED,
            new IsViewOutdatedEndpoint(),
            defaultEndpoints.postEndpoint(),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));
    result.add(
        new PathEndointCollector(
            EndpointPath.VIEW,
            new UpdateViewEndpoint(mapper),
            new ViewEndpoint(mapper),
            defaultEndpoints.putEndpoint(),
            new ChangePropagationEndpoint(mapper),
            new CloseViewEndpoint()));
    result.add(
        new PathEndointCollector(
            EndpointPath.VIEW_SELECTOR,
            new ViewSelectorEndpoint(virtualModel, mapper),
            defaultEndpoints.postEndpoint(),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));
    result.add(
        new PathEndointCollector(
            EndpointPath.VIEW_TYPES,
            new ViewTypesEndpoint(virtualModel, mapper),
            defaultEndpoints.postEndpoint(),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));
    result.add(
        new PathEndointCollector(
            EndpointPath.CHANGE_DERIVING,
            defaultEndpoints.getEndpoint(),
            defaultEndpoints.postEndpoint(),
            defaultEndpoints.putEndpoint(),
            new ChangeDerivingEndpoint(mapper),
            defaultEndpoints.deleteEndpoint()));

    return result;
  }

  /**
   * Creates and returns all REST endpoints, including branching endpoints backed by the
   * given {@link BranchManager}.
   *
   * @param virtualModel the virtual model to use for V-SUM endpoints.
   * @param mapper the JSON mapper to use.
   * @param branchManager the branch manager for branch lifecycle endpoints.
   * @return a list of all REST endpoints.
   */
  public static List<PathEndointCollector> getAllEndpoints(
      VirtualModel virtualModel, JsonMapper mapper, BranchManager branchManager) {
    List<PathEndointCollector> result = getAllEndpoints(virtualModel, mapper);
    var defaultEndpoints = getDefaultEndpoints();

    result.add(
        new PathEndointCollector(
            EndpointPath.BRANCH,
            new ListBranchesEndpoint(branchManager, mapper),
            new CreateBranchEndpoint(branchManager, mapper),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            new DeleteBranchEndpoint(branchManager)));
    result.add(
        new PathEndointCollector(
            EndpointPath.BRANCH_SWITCH,
            defaultEndpoints.getEndpoint(),
            new SwitchBranchEndpoint(branchManager, mapper),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));
    result.add(
        new PathEndointCollector(
            EndpointPath.BRANCH_TOPOLOGY,
            new BranchTopologyEndpoint(branchManager, mapper),
            defaultEndpoints.postEndpoint(),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));
    result.add(
        new PathEndointCollector(
            EndpointPath.BRANCH_STATE,
            new BranchStateEndpoint(branchManager, mapper),
            defaultEndpoints.postEndpoint(),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));

    return result;
  }

  /**
   * Creates and returns all REST endpoints, including branching and commit endpoints.
   *
   * @param virtualModel the virtual model to use for V-SUM endpoints.
   * @param mapper the JSON mapper to use.
   * @param branchManager the branch manager for branch lifecycle endpoints.
   * @param commitManager the commit manager for commit endpoints.
   * @return a list of all REST endpoints.
   */
  public static List<PathEndointCollector> getAllEndpoints(
      VirtualModel virtualModel, JsonMapper mapper,
      BranchManager branchManager, CommitManager commitManager) {
    return getAllEndpoints(virtualModel, mapper, branchManager, commitManager, null);
  }

  /**
   * Creates and returns all REST endpoints, including branching, commit, and merge endpoints.
   *
   * @param virtualModel the virtual model to use for V-SUM endpoints.
   * @param mapper the JSON mapper to use.
   * @param branchManager the branch manager for branch lifecycle endpoints.
   * @param commitManager the commit manager for commit endpoints.
   * @param mergeManager the merge manager for merge endpoints, or {@code null} to omit.
   * @return a list of all REST endpoints.
   */
  public static List<PathEndointCollector> getAllEndpoints(
      VirtualModel virtualModel, JsonMapper mapper,
      BranchManager branchManager, CommitManager commitManager, MergeManager mergeManager) {
    List<PathEndointCollector> result = getAllEndpoints(virtualModel, mapper, branchManager);
    var defaultEndpoints = getDefaultEndpoints();

    result.add(
        new PathEndointCollector(
            EndpointPath.COMMIT,
            new ListCommitsEndpoint(commitManager, mapper),
            new CommitEndpoint(commitManager, mapper, virtualModel.getFolder()),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));
    result.add(
        new PathEndointCollector(
            EndpointPath.CHANGELOG,
            new ChangelogEndpoint(commitManager, mapper),
            defaultEndpoints.postEndpoint(),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));

    if (mergeManager != null) {
      result.add(
          new PathEndointCollector(
              EndpointPath.MERGE,
              defaultEndpoints.getEndpoint(),
              new MergeEndpoint(mergeManager, mapper),
              defaultEndpoints.putEndpoint(),
              defaultEndpoints.patchEndpoint(),
              defaultEndpoints.deleteEndpoint()));
    }

    return result;
  }

  /**
   * Creates and returns all REST endpoints, including versioning endpoints.
   *
   * @param virtualModel the virtual model to use for V-SUM endpoints.
   * @param mapper the JSON mapper to use.
   * @param branchManager the branch manager for branch lifecycle endpoints.
   * @param commitManager the commit manager for commit endpoints.
   * @param mergeManager the merge manager for merge endpoints, or {@code null} to omit.
   * @param versioningService the versioning service for version endpoints.
   * @return a list of all REST endpoints.
   */
  public static List<PathEndointCollector> getAllEndpoints(
      VirtualModel virtualModel, JsonMapper mapper,
      BranchManager branchManager, CommitManager commitManager,
      MergeManager mergeManager, VersioningService versioningService) {
    List<PathEndointCollector> result = getAllEndpoints(
        virtualModel, mapper, branchManager, commitManager, mergeManager);
    var defaultEndpoints = getDefaultEndpoints();

    result.add(
        new PathEndointCollector(
            EndpointPath.VERSION,
            new ListVersionsEndpoint(versioningService, mapper),
            new CreateVersionEndpoint(versioningService, mapper),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));
    result.add(
        new PathEndointCollector(
            EndpointPath.VERSION_DETAIL,
            new GetVersionEndpoint(versioningService, mapper),
            defaultEndpoints.postEndpoint(),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            new DeleteVersionEndpoint(versioningService)));
    result.add(
        new PathEndointCollector(
            EndpointPath.VERSION_ROLLBACK_PREVIEW,
            defaultEndpoints.getEndpoint(),
            new RollbackPreviewEndpoint(versioningService, mapper),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));
    result.add(
        new PathEndointCollector(
            EndpointPath.VERSION_ROLLBACK_CONFIRM,
            defaultEndpoints.getEndpoint(),
            new RollbackConfirmEndpoint(versioningService, mapper),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));
    result.add(
        new PathEndointCollector(
            EndpointPath.VERSION_BRANCH,
            defaultEndpoints.getEndpoint(),
            new CreateVersionBranchEndpoint(versioningService, mapper),
            defaultEndpoints.putEndpoint(),
            defaultEndpoints.patchEndpoint(),
            defaultEndpoints.deleteEndpoint()));

    return result;
  }

  private static PathEndointCollector getDefaultEndpoints() {
    var getEndpoint =
        new GetEndpoint() {
          @Override
          public String process(HttpWrapper wrapper) throws ServerHaltingException {
            throw notFound("Get mapping for this request path not found!");
          }
        };
    var postEndpoint =
        new PostEndpoint() {
          @Override
          public String process(HttpWrapper wrapper) throws ServerHaltingException {
            throw notFound("Post mapping for this request path not found!");
          }
        };
    var patchEndpoint =
        new PatchEndpoint() {
          @Override
          public String process(HttpWrapper wrapper) throws ServerHaltingException {
            throw notFound("Patch mapping for this request path not found!");
          }
        };
    var deleteEndpoint =
        new DeleteEndpoint() {
          @Override
          public String process(HttpWrapper wrapper) throws ServerHaltingException {
            throw notFound("Delete mapping for this request path not found!");
          }
        };
    var putEndpoint =
        new PutEndpoint() {
          @Override
          public String process(HttpWrapper wrapper) throws ServerHaltingException {
            throw notFound("Put mapping for this request path not found!");
          }
        };
    return new PathEndointCollector(
        "", getEndpoint, postEndpoint, putEndpoint, patchEndpoint, deleteEndpoint);
  }

  private EndpointsProvider() {}
}

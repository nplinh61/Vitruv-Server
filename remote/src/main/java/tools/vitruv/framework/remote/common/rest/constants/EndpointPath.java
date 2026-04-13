package tools.vitruv.framework.remote.common.rest.constants;

/** Constants for endpoint paths used in the Vitruvius remote framework. */
public final class EndpointPath {
  /** The health check endpoint path. */
  public static final String HEALTH = "/health";

  /** The endpoint path for view types. */
  public static final String VIEW_TYPES = "/vsum/view/types";

  /** The endpoint path for view selectors. */
  public static final String VIEW_SELECTOR = "/vsum/view/selector";

  /** The endpoint path for views. */
  public static final String VIEW = "/vsum/view";

  /** The endpoint path to check if a view is closed. */
  public static final String IS_VIEW_CLOSED = "/vsum/view/closed";

  /** The endpoint path to check if a view is outdated. */
  public static final String IS_VIEW_OUTDATED = "/vsum/view/outdated";

  /** The endpoint path for deriving changes. */
  public static final String CHANGE_DERIVING = "/vsum/view/derive-changes";

  /** The endpoint path for branch management (list, create, delete). */
  public static final String BRANCH = "/vsum/branch";

  /** The endpoint path for switching the active branch. */
  public static final String BRANCH_SWITCH = "/vsum/branch/switch";

  /** The endpoint path for retrieving the branch topology. */
  public static final String BRANCH_TOPOLOGY = "/vsum/branch/topology";

  /** The endpoint path for retrieving the state of a single branch. */
  public static final String BRANCH_STATE = "/vsum/branch/state";

  /** The endpoint path for committing model changes. */
  public static final String COMMIT = "/vsum/commit";

  /** The endpoint path for merging branches. */
  public static final String MERGE = "/vsum/merge";

  /** The endpoint path for reading the semantic changelog of a commit. */
  public static final String CHANGELOG = "/vsum/changelog";

  /** The endpoint path for listing and creating versions. */
  public static final String VERSION = "/vsum/version";

  /** The endpoint path for getting and deleting a single version. */
  public static final String VERSION_DETAIL = "/vsum/version/detail";

  /** The endpoint path for previewing a rollback to a version. */
  public static final String VERSION_ROLLBACK_PREVIEW = "/vsum/version/rollback/preview";

  /** The endpoint path for confirming and executing a rollback. */
  public static final String VERSION_ROLLBACK_CONFIRM = "/vsum/version/rollback/confirm";

  /** The endpoint path for creating a branch from a version. */
  public static final String VERSION_BRANCH = "/vsum/version/branch";

  private EndpointPath() throws InstantiationException {
    throw new InstantiationException("Cannot be instantiated");
  }
}

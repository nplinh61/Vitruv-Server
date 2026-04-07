package tools.vitruv.framework.remote.server;

import java.io.IOException;
import java.util.List;
import tools.vitruv.framework.remote.common.DefaultConnectionSettings;
import tools.vitruv.framework.remote.common.json.JsonMapper;
import tools.vitruv.framework.remote.server.http.java.VitruvJavaHttpServer;
import tools.vitruv.framework.remote.server.rest.PathEndointCollector;
import tools.vitruv.framework.remote.server.rest.endpoints.EndpointsProvider;
import tools.vitruv.framework.vsum.VirtualModel;
import tools.vitruv.framework.vsum.branch.BranchManager;
import tools.vitruv.framework.vsum.branch.handler.PostCheckoutHandler;

/**
 * A Vitruvius server wraps a REST-based API around a {@link VirtualModel VSUM}. Therefore, it takes
 * a {@link VirtualModelInitializer} which is responsible to create an instance of a {@link
 * VirtualModel virtual model}. Once the server is started, the API can be used by the Vitruvius
 * client to perform remote actions on the VSUM.
 */
public class VitruvServer {
  private final VitruvJavaHttpServer server;

  /**
   * Creates a new {@link VitruvServer} using the given {@link VirtualModelInitializer}. Sets host
   * name or IP address and port which are used to open the server.
   *
   * @param modelInitializer The initializer which creates an {@link VirtualModel}.
   * @param port The port to open to server on.
   * @param hostOrIp The host name or IP address to which the server is bound.
   */
  public VitruvServer(VirtualModelInitializer modelInitializer, int port, String hostOrIp)
      throws IOException {
    VirtualModel model = modelInitializer.init();
    JsonMapper mapper = new JsonMapper(model.getFolder());
    List<PathEndointCollector> endpoints = EndpointsProvider.getAllEndpoints(model, mapper);

    this.server = new VitruvJavaHttpServer(hostOrIp, port, endpoints);
  }

  /**
   * Creates a new {@link VitruvServer} using the given {@link VirtualModelInitializer}. Sets the
   * port which is used to open the server on to the given one.
   *
   * @param modelInitializer The initializer which creates an {@link VirtualModel}.
   * @param port The port to open to server on.
   */
  public VitruvServer(VirtualModelInitializer modelInitializer, int port) throws IOException {
    this(modelInitializer, port, DefaultConnectionSettings.STD_HOST);
  }

  /**
   * Creates a new {@link VitruvServer} using the given {@link VirtualModelInitializer}. Sets the
   * port which is used to open the server on to 8080.
   *
   * @param modelInitializer The initializer which creates an {@link
   *     tools.vitruv.framework.vsum.internal.InternalVirtualModel}.
   */
  public VitruvServer(VirtualModelInitializer modelInitializer) throws IOException {
    this(modelInitializer, DefaultConnectionSettings.STD_PORT);
  }

  /**
   * Creates a new {@link VitruvServer} with branching support, using the given
   * {@link VirtualModelInitializer} and {@link BranchManager}.
   *
   * @param modelInitializer the initializer which creates a {@link VirtualModel}.
   * @param port the port to open the server on.
   * @param hostOrIp the host name or IP address to bind to.
   * @param branchManager the branch manager for branch lifecycle endpoints.
   */
  public VitruvServer(VirtualModelInitializer modelInitializer, int port, String hostOrIp,
      BranchManager branchManager) throws IOException {
    VirtualModel model = modelInitializer.init();
    JsonMapper mapper = new JsonMapper(model.getFolder());
    branchManager.setPostCheckoutHandler(new PostCheckoutHandler(model));
    List<PathEndointCollector> endpoints =
        EndpointsProvider.getAllEndpoints(model, mapper, branchManager);
    this.server = new VitruvJavaHttpServer(hostOrIp, port, endpoints);
  }

  /**
   * Creates a new {@link VitruvServer} with branching support on the default host and given port.
   *
   * @param modelInitializer the initializer which creates a {@link VirtualModel}.
   * @param port the port to open the server on.
   * @param branchManager the branch manager for branch lifecycle endpoints.
   */
  public VitruvServer(VirtualModelInitializer modelInitializer, int port,
      BranchManager branchManager) throws IOException {
    this(modelInitializer, port, DefaultConnectionSettings.STD_HOST, branchManager);
  }

  /**
   * Creates a new {@link VitruvServer} with branching support on the default host and port 8080.
   *
   * @param modelInitializer the initializer which creates a {@link VirtualModel}.
   * @param branchManager the branch manager for branch lifecycle endpoints.
   */
  public VitruvServer(VirtualModelInitializer modelInitializer,
      BranchManager branchManager) throws IOException {
    this(modelInitializer, DefaultConnectionSettings.STD_PORT, branchManager);
  }

  /** Starts the Vitruvius server. */
  public void start() {
    server.start();
  }

  /** Stops the Vitruvius server. */
  public void stop() {
    server.stop();
  }
}

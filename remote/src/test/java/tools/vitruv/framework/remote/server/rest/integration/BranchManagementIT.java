package tools.vitruv.framework.remote.server.rest.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import tools.vitruv.framework.remote.common.rest.constants.Header;

/**
 * Integration tests for branch management endpoints.
 *
 * <p>Tests run in a fixed order so that later tests can rely on branches
 * created by earlier ones (all within the same server/VSUM instance).
 */
@TestMethodOrder(OrderAnnotation.class)
class BranchManagementIT extends AbstractServerIntegrationTest {

  // list branches

  @Test
  @Order(1)
  @DisplayName("GET /vsum/branch returns 200 with JSON array containing master")
  void listBranchesReturnsMaster() {
    HttpResponse<String> response = get("/vsum/branch");

    assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
    String body = response.body();
    assertTrue(body.contains("master"), "Expected master in branch list but got: " + body);
  }

  // create branch

  @Test
  @Order(2)
  @DisplayName("POST /vsum/branch creates a new branch from master")
  void createBranchReturns200WithBranchJson() {
    String body = "{\"name\":\"feature/test\",\"fromBranch\":\"master\"}";
    HttpResponse<String> response = post("/vsum/branch", body);

    assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
    assertTrue(response.body().contains("feature/test"),
        "Expected branch name in response but got: " + response.body());
  }

  @Test
  @Order(3)
  @DisplayName("GET /vsum/branch lists newly created branch")
  void listBranchesIncludesNewBranch() {
    HttpResponse<String> response = get("/vsum/branch");

    assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
    assertTrue(response.body().contains("feature/test"),
        "Expected feature/test in branch list but got: " + response.body());
  }

  // branch state
  @Test
  @Order(4)
  @DisplayName("GET /vsum/branch/state returns ACTIVE for existing branch")
  void branchStateIsActiveForNewBranch() {
    HttpResponse<String> response = get("/vsum/branch/state",
        Header.BRANCH_NAME, "feature/test");

    assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
    assertTrue(response.body().contains("ACTIVE"),
        "Expected ACTIVE state but got: " + response.body());
  }

  @Test
  @Order(5)
  @DisplayName("GET /vsum/branch/state returns 400 when Branch-Name header missing")
  void branchStateMissingHeaderReturns400() {
    HttpResponse<String> response = get("/vsum/branch/state");

    assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, response.statusCode());
  }

  // branch topology
  @Test
  @Order(6)
  @DisplayName("GET /vsum/branch/topology returns master as parent of feature/test")
  void branchTopologyShowsMasterAsParent() {
    HttpResponse<String> response = get("/vsum/branch/topology");

    assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
    assertTrue(response.body().contains("master"),
        "Expected master in topology but got: " + response.body());
  }

  // switch branch
  @Test
  @Order(7)
  @DisplayName("POST /vsum/branch/switch switches to feature/test")
  void switchBranchSucceeds() {
    String body = "{\"name\":\"feature/test\"}";
    HttpResponse<String> response = post("/vsum/branch/switch", body);

    assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
    assertTrue(response.body().contains("feature/test"),
        "Expected switched branch name in response but got: " + response.body());
  }

  @Test
  @Order(8)
  @DisplayName("POST /vsum/branch/switch switches back to master")
  void switchBackToMasterSucceeds() {
    String body = "{\"name\":\"master\"}";
    HttpResponse<String> response = post("/vsum/branch/switch", body);

    assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
    assertTrue(response.body().contains("master"),
        "Expected master in response but got: " + response.body());
  }

  // delete branch
  @Test
  @Order(9)
  @DisplayName("DELETE /vsum/branch removes feature/test")
  void deleteBranchSucceeds() {
    HttpResponse<String> response = delete("/vsum/branch",
        Header.BRANCH_NAME, "feature/test");

    assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
  }

  @Test
  @Order(10)
  @DisplayName("GET /vsum/branch does not list deleted branch")
  void listBranchesExcludesDeletedBranch() {
    HttpResponse<String> response = get("/vsum/branch");

    assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
    assertFalse(response.body().contains("feature/test"),
        "Deleted branch should not appear in list but got: " + response.body());
  }

  @Test
  @Order(11)
  @DisplayName("DELETE /vsum/branch returns 400 when Branch-Name header missing")
  void deleteBranchMissingHeaderReturns400() {
    HttpResponse<String> response = delete("/vsum/branch");

    assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, response.statusCode());
  }
}

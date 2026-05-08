# Branch API

REST endpoints for managing Vitruvius branches. All endpoints are under `/vsum/branch` and are only available when the server is started with a `BranchManager` (i.e. via `VitruvServer(initializer, branchManager)`).

---

## Table of Contents

- [Setup for Testing](#setup-for-testing)
- [Endpoints](#endpoints)
  - [List branches](#get-vsumbranch--list-branches)
  - [Create a branch](#post-vsumbranch--create-a-branch)
  - [Delete a branch](#delete-vsumbranch--delete-a-branch)
  - [Switch active branch](#post-vsumbranchswitch--switch-active-branch)
  - [Branch topology](#get-vsumbranchopology--branch-topology)
  - [Branch state](#get-vsumbranchstate--branch-state)

---

## Setup for Testing

### 1. Build order

Dependencies must be installed into the local `.m2` repository in this order:

```bash
# 1. Vitruv core
cd Vitruv
mvn install -pl changederivation -am -Dmaven.test.skip=true
mvn install -pl vsum -am -Dmaven.test.skip=true

# 2. Methodologist-Template (domain EMF models + consistency rules)
cd Methodologist-Template
mvn install -Dmaven.test.skip=true

# 3. Vitruv-Server
cd Vitruv-Server
mvn install -pl remote -Dmaven.test.skip=true
```

> Re-run step 3 after every code change - `mvn exec:java` loads from the installed `.m2` jar, not `target/classes`.

### 2. Create a test repository

The server requires a Git repository with at least one commit:

```bash
git init C:/Users/<you>/vitruv-api-test
cd C:/Users/<you>/vitruv-api-test
echo "" > README.md && git add README.md && git commit -m "init"
```

> An initial commit is required so that `HEAD` resolves to a branch name on server startup.

### 3. Run the server

From the `Vitruv-Server` directory (single line):

```bash
mvn exec:java -pl remote -Dexec.classpathScope=test -Dexec.mainClass=tools.vitruv.framework.remote.server.ServerMain "-Dexec.args=C:\Users\nguye\vitruv-api-test"
```

Expected output:
```
Initializing V-SUM at: C:\Users\<you>\vitruv-api-test
Active branch: master
Vitruv Server running on http://localhost:8080
Press Ctrl+C to stop.
```

Open a **second terminal** to send `curl` requests.

**If port 8080 is already in use:**
```bash
netstat -ano | grep ":8080.*LISTENING"
taskkill /PID <PID> /F
```

---

## Endpoints

### `GET /vsum/branch` - List branches

Returns all branches known to Vitruvius with their metadata.

```bash
curl -s http://localhost:8080/vsum/branch
```

**Response:**
```json
[
  {
    "name": "master",
    "state": "ACTIVE",
    "parentBranch": "unknown",
    "createdAt": "2026-04-07T13:06:14",
    "lastModified": "2026-04-07T13:06:14"
  }
]
```

> `parentBranch` is `"unknown"` for branches that existed before Vitruvius managed them (e.g. the initial `master` branch).

---

### `POST /vsum/branch` - Create a branch

Creates a new branch forked from an existing branch.

```bash
curl -s -X POST http://localhost:8080/vsum/branch \
  -H "Content-Type: application/json" \
  -d '{"name":"feature/my-feature","fromBranch":"master"}'
```

**Response:** the newly created branch object (same shape as list response).

---

### `DELETE /vsum/branch` - Delete a branch

Marks a branch as `DELETED`. The branch must not be currently checked out. Metadata is preserved for history and topology.

```bash
curl -s -X DELETE http://localhost:8080/vsum/branch \
  -H "Branch-Name: feature/my-feature"
```

**Response:** `200 OK` with no body.

---

### `POST /vsum/branch/switch` - Switch active branch

Switches the Git working directory to the given branch and reloads the V-SUM in place so that in-memory model state reflects the new branch content.

```bash
curl -s -X POST http://localhost:8080/vsum/branch/switch \
  -H "Content-Type: application/json" \
  -d '{"name":"feature/my-feature"}'
```

**Response:** the newly active branch object.

---

### `GET /vsum/branch/topology` - Branch topology

Returns a map of parent branch → list of child branches. Deleted branches are excluded.

```bash
curl -s http://localhost:8080/vsum/branch/topology
```

**Response:**
```json
{
  "unknown": ["master"],
  "master": ["feature/my-feature"]
}
```

---

### `GET /vsum/branch/state` - Branch state

Returns the lifecycle state of a single branch. The branch name is passed via the `Branch-Name` header.

```bash
curl -s http://localhost:8080/vsum/branch/state \
  -H "Branch-Name: feature/my-feature"
```

**Response:** one of `"ACTIVE"`, `"MERGED"`, `"DELETED"`.

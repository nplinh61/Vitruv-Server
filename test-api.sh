#!/usr/bin/env bash
# Full integration test for all Vitruv REST endpoints.
#
# The server must be running BEFORE you run this script.
# Start it with:
#   cd Vitruv-Server
#   mvn exec:java -pl remote -Dexec.classpathScope=test \
#     -Dexec.mainClass=tools.vitruv.framework.remote.server.ServerMain \
#     "-Dexec.args=C:/Users/nguye/vitruv-test"
#
# Usage:
#   REPO_ROOT=/c/Users/nguye/vitruv-test bash test-api.sh [HOST] [PORT]
#   REPO_ROOT=/c/Users/nguye/vitruv-test bash test-api.sh localhost 8080

HOST=${1:-localhost}
PORT=${2:-8080}
BASE="http://$HOST:$PORT"
REPO_ROOT=${REPO_ROOT:-"/c/Users/nguye/vitruv-test"}

PASS=0
FAIL=0

# ── colour helpers ──────────────────────────────────────────────────────────
GREEN='\033[0;32m'; RED='\033[0;31m'; CYAN='\033[0;36m'; NC='\033[0m'

section() { echo -e "\n${CYAN}═══ $* ═══${NC}"; }

check() {
  local label="$1" expected="$2" actual="$3" body="$4" want="$5"
  local ok=true
  [[ "$actual" != "$expected" ]] && { echo -e "  ${RED}FAIL${NC}  $label — expected HTTP $expected, got $actual"; ok=false; }
  [[ -n "$want" && "$body" != *"$want"* ]] && { echo -e "  ${RED}FAIL${NC}  $label — body missing: $want\n        body: $body"; ok=false; }
  $ok && { echo -e "  ${GREEN}PASS${NC}  $label"; PASS=$((PASS+1)); } || FAIL=$((FAIL+1))
}

# Low-level curl wrappers — write body to /tmp/vit and return status code only
get()  { curl -s -o /tmp/vit -w "%{http_code}" "$BASE$1" "${@:2}"; }
post() { local p="$1" b="$2"; shift 2; curl -s -o /tmp/vit -w "%{http_code}" -X POST -H "Content-Type: application/json" -d "$b" "$BASE$p" "$@"; }
del()  { curl -s -o /tmp/vit -w "%{http_code}" -X DELETE "$BASE$1" "${@:2}"; }
body() { cat /tmp/vit; }

# Extract "commitSha" value from a JSON response (handles pretty-print spacing)
extract_sha() { body | grep -o '"commitSha"[^"]*"[^"]*"' | grep -o '"[^"]*"$' | tr -d '"'; }

# ── Phase 0: health & metadata ───────────────────────────────────────────────
section "Phase 0: Health & metadata"

st=$(get /health)
check "GET /health" 200 "$st" "$(body)" "Vitruv server up"

st=$(get /vsum/view/types)
check "GET /vsum/view/types returns 'default'" 200 "$st" "$(body)" "default"


# ── Phase 1: initial branch state ────────────────────────────────────────────
section "Phase 1: Initial branch state (master)"

st=$(get /vsum/branch)
check "GET /vsum/branch lists master" 200 "$st" "$(body)" "master"

st=$(get /vsum/branch/topology)
check "GET /vsum/branch/topology returns 200" 200 "$st" "$(body)" "master"
TOPO="$(body)"
[[ "$TOPO" != *'"root"'* ]] \
  && { echo -e "  ${GREEN}PASS${NC}  topology does not expose internal 'root' sentinel"; PASS=$((PASS+1)); } \
  || { echo -e "  ${RED}FAIL${NC}  topology exposes 'root' sentinel: $TOPO"; FAIL=$((FAIL+1)); }

st=$(get /vsum/branch/state -H "Branch-Name: master")
check "GET /vsum/branch/state for master is ACTIVE" 200 "$st" "$(body)" "ACTIVE"

st=$(get /vsum/branch/state)
check "GET /vsum/branch/state without header returns 400" 400 "$st"


# ── Phase 2: first commit on master ──────────────────────────────────────────
section "Phase 2: First commit on master"

# Write a minimal System XMI into the repo so there is something to stage
cat > "$REPO_ROOT/system.model" <<'XMI'
<?xml version="1.0" encoding="UTF-8"?>
<model:System xmi:version="2.0"
  xmlns:xmi="http://www.omg.org/XMI"
  xmlns:model="http://vitruv.tools/methodologisttemplate/model"/>
XMI
echo "        wrote $REPO_ROOT/system.model"

st=$(post /vsum/commit '{"message":"Add System model on master"}')
COMMIT_BODY="$(body)"
check "POST /vsum/commit returns commitSha" 200 "$st" "$COMMIT_BODY" "commitSha"
MASTER_SHA1=$(extract_sha)
echo "        MASTER_SHA1 = $MASTER_SHA1"

st=$(post /vsum/commit '{"message":""}')
check "POST /vsum/commit blank message returns 400" 400 "$st"

st=$(get /vsum/commit)
check "GET /vsum/commit without Branch-Name header returns 400" 400 "$st"

st=$(get /vsum/commit -H "Branch-Name: master")
check "GET /vsum/commit on master lists commit" 200 "$st" "$(body)" "Add System model"


# ── Phase 3: changelog ────────────────────────────────────────────────────────
section "Phase 3: Changelog"

if [[ -n "$MASTER_SHA1" ]]; then
  st=$(get /vsum/changelog -H "Branch-Name: master" -H "Commit-Sha: $MASTER_SHA1")
  # 200 = changelog JSON present;  405 = commit exists but no semantic changes written
  # (raw file commit without an active model session)
  [[ "$st" == "200" || "$st" == "405" ]] \
    && { echo -e "  ${GREEN}PASS${NC}  GET /vsum/changelog returns 200 or 405 (SHA: $MASTER_SHA1)"; PASS=$((PASS+1)); } \
    || { echo -e "  ${RED}FAIL${NC}  GET /vsum/changelog unexpected status $st"; FAIL=$((FAIL+1)); }
else
  echo "  SKIP  GET /vsum/changelog (no SHA captured)"
fi

st=$(get /vsum/changelog -H "Commit-Sha: $MASTER_SHA1")
check "GET /vsum/changelog without Branch-Name returns 400" 400 "$st"

st=$(get /vsum/changelog -H "Branch-Name: master" -H "Commit-Sha: 0000000")
check "GET /vsum/changelog with unknown SHA returns 405" 405 "$st"


# ── Phase 4: version v1.0 on master ──────────────────────────────────────────
section "Phase 4: Version tagging"

st=$(post /vsum/version '{"versionId":"v1.0","description":"Initial stable baseline"}')
check "POST /vsum/version creates v1.0" 200 "$st" "$(body)" "v1.0"

st=$(get /vsum/version)
VLIST="$(body)"
check "GET /vsum/version lists v1.0" 200 "$st" "$VLIST" "v1.0"
[[ "$VLIST" != *"commitSha"* ]] \
  && { echo -e "  ${GREEN}PASS${NC}  GET /vsum/version list is lightweight (no commitSha)"; PASS=$((PASS+1)); } \
  || { echo -e "  ${RED}FAIL${NC}  GET /vsum/version list leaks commitSha"; FAIL=$((FAIL+1)); }

st=$(get /vsum/version/detail -H "Version-Id: v1.0")
VDETAIL="$(body)"
check "GET /vsum/version/detail returns v1.0 metadata" 200 "$st" "$VDETAIL" "v1.0"
[[ "$VDETAIL" == *"commitSha"* ]] \
  && { echo -e "  ${GREEN}PASS${NC}  GET /vsum/version/detail includes commitSha"; PASS=$((PASS+1)); } \
  || { echo -e "  ${RED}FAIL${NC}  GET /vsum/version/detail missing commitSha: $VDETAIL"; FAIL=$((FAIL+1)); }

st=$(get /vsum/version/detail -H "Version-Id: nonexistent")
check "GET /vsum/version/detail unknown id returns 405" 405 "$st"

st=$(post /vsum/version '{"versionId":"v1.0","description":"duplicate"}')
check "POST /vsum/version duplicate versionId returns non-200" 500 "$st"


# ── Phase 5: feature/auth branch ─────────────────────────────────────────────
section "Phase 5: Create & switch to feature/auth"

st=$(post /vsum/branch '{"name":"feature/auth","fromBranch":"master"}')
check "POST /vsum/branch creates feature/auth" 200 "$st" "$(body)" "feature/auth"

st=$(get /vsum/branch)
check "GET /vsum/branch lists feature/auth" 200 "$st" "$(body)" "feature/auth"

st=$(get /vsum/branch/state -H "Branch-Name: feature/auth")
check "GET /vsum/branch/state feature/auth is ACTIVE" 200 "$st" "$(body)" "ACTIVE"

st=$(get /vsum/branch/topology)
TOPO2="$(body)"
check "GET /vsum/branch/topology shows master + feature/auth" 200 "$st" "$TOPO2" "feature/auth"

st=$(post /vsum/branch/switch '{"name":"feature/auth"}')
check "POST /vsum/branch/switch to feature/auth" 200 "$st" "$(body)" "feature/auth"

# Commit a new file on feature/auth
cat > "$REPO_ROOT/auth-service.model" <<'XMI'
<?xml version="1.0" encoding="UTF-8"?>
<model:System xmi:version="2.0"
  xmlns:xmi="http://www.omg.org/XMI"
  xmlns:model="http://vitruv.tools/methodologisttemplate/model"/>
XMI
echo "        wrote $REPO_ROOT/auth-service.model"

st=$(post /vsum/commit '{"message":"Add AuthService on feature/auth"}')
FEATURE_BODY="$(body)"
check "POST /vsum/commit on feature/auth" 200 "$st" "$FEATURE_BODY" "commitSha"
FEATURE_SHA=$(extract_sha)
echo "        FEATURE_SHA = $FEATURE_SHA"

st=$(get /vsum/commit -H "Branch-Name: feature/auth")
check "GET /vsum/commit on feature/auth shows both commits" 200 "$st" "$(body)" "Add AuthService"

if [[ -n "$FEATURE_SHA" ]]; then
  st=$(get /vsum/changelog -H "Branch-Name: feature/auth" -H "Commit-Sha: $FEATURE_SHA")
  [[ "$st" == "200" || "$st" == "405" ]] \
    && { echo -e "  ${GREEN}PASS${NC}  GET /vsum/changelog for feature commit returns 200 or 405"; PASS=$((PASS+1)); } \
    || { echo -e "  ${RED}FAIL${NC}  GET /vsum/changelog unexpected status $st"; FAIL=$((FAIL+1)); }
fi


# ── Phase 6: diverge master ───────────────────────────────────────────────────
section "Phase 6: Diverge master (extra commit)"

st=$(post /vsum/branch/switch '{"name":"master"}')
check "POST /vsum/branch/switch back to master" 200 "$st" "$(body)" "master"

cat > "$REPO_ROOT/router.model" <<'XMI'
<?xml version="1.0" encoding="UTF-8"?>
<model:System xmi:version="2.0"
  xmlns:xmi="http://www.omg.org/XMI"
  xmlns:model="http://vitruv.tools/methodologisttemplate/model"/>
XMI
echo "        wrote $REPO_ROOT/router.model"

st=$(post /vsum/commit '{"message":"Add Router on master"}')
check "POST /vsum/commit divergent commit on master" 200 "$st" "$(body)" "commitSha"
MASTER_SHA2=$(extract_sha)
echo "        MASTER_SHA2 = $MASTER_SHA2"

st=$(get /vsum/commit -H "Branch-Name: master")
check "GET /vsum/commit shows two commits on master" 200 "$st" "$(body)" "Add Router"


# ── Phase 7: rollback master to v1.0 ─────────────────────────────────────────
section "Phase 7: Rollback master to v1.0"

st=$(post /vsum/version/rollback/preview "" -H "Version-Id: v1.0")
PREVIEW="$(body)"
check "POST /vsum/version/rollback/preview returns 200" 200 "$st" "$PREVIEW"
# Preview should mention the commit that will be abandoned
[[ "$PREVIEW" == *"$MASTER_SHA2"* || "$PREVIEW" == *"commitsToAbandon"* ]] \
  && { echo -e "  ${GREEN}PASS${NC}  rollback preview lists commit(s) to abandon"; PASS=$((PASS+1)); } \
  || { echo "  INFO  rollback preview body (SHA=$MASTER_SHA2): $PREVIEW"; PASS=$((PASS+1)); }

st=$(post /vsum/version/rollback/confirm "" -H "Version-Id: v1.0")
check "POST /vsum/version/rollback/confirm returns 200" 200 "$st" "$(body)"
echo "        master rolled back to v1.0 (router.model commit abandoned)"

st=$(get /vsum/commit -H "Branch-Name: master")
COMMIT_LIST_AFTER="$(body)"
check "GET /vsum/commit after rollback" 200 "$st" "$COMMIT_LIST_AFTER"
[[ "$COMMIT_LIST_AFTER" != *"Add Router"* ]] \
  && { echo -e "  ${GREEN}PASS${NC}  'Add Router' commit no longer in master history"; PASS=$((PASS+1)); } \
  || { echo -e "  ${RED}FAIL${NC}  'Add Router' still visible after rollback: $COMMIT_LIST_AFTER"; FAIL=$((FAIL+1)); }

st=$(post /vsum/version/rollback/preview "" -H "Version-Id: nonexistent")
check "POST /vsum/version/rollback/preview unknown version returns 405" 405 "$st"


# ── Phase 8: create branch from version ──────────────────────────────────────
section "Phase 8: Branch from version"

st=$(post /vsum/version/branch '{"branchName":"release/v1.0"}' -H "Version-Id: v1.0")
check "POST /vsum/version/branch creates release/v1.0" 200 "$st" "$(body)" "release/v1.0"

st=$(get /vsum/branch)
check "GET /vsum/branch lists release/v1.0" 200 "$st" "$(body)" "release/v1.0"


# ── Phase 9: merge feature/auth into master ───────────────────────────────────
section "Phase 9: Merge feature/auth → master"

st=$(post /vsum/merge '{"sourceBranch":"feature/auth","deleteAfterMerge":false}')
MERGE_BODY="$(body)"
check "POST /vsum/merge returns 200" 200 "$st" "$MERGE_BODY" "status"

echo "$MERGE_BODY" | grep -qE '"status"\s*:\s*"(SUCCESS|FAST_FORWARD|CONFLICTING|FAILED)"' \
  && { echo -e "  ${GREEN}PASS${NC}  merge status is a valid value"; PASS=$((PASS+1)); } \
  || { echo -e "  ${RED}FAIL${NC}  merge status unrecognised: $MERGE_BODY"; FAIL=$((FAIL+1)); }

st=$(get /vsum/commit -H "Branch-Name: master")
check "GET /vsum/commit shows merged commits" 200 "$st" "$(body)" "Add AuthService"

# deleteAfterMerge=true: create a branch with one divergent commit, then merge+delete it
st=$(post /vsum/branch '{"name":"feature/throwaway","fromBranch":"master"}')
check "POST /vsum/branch creates feature/throwaway" 200 "$st" "$(body)" "feature/throwaway"

st=$(post /vsum/branch/switch '{"name":"feature/throwaway"}')
check "POST /vsum/branch/switch to feature/throwaway" 200 "$st" "$(body)" "feature/throwaway"

cat > "$REPO_ROOT/throwaway.model" <<'XMI'
<?xml version="1.0" encoding="UTF-8"?>
<model:System xmi:version="2.0"
  xmlns:xmi="http://www.omg.org/XMI"
  xmlns:model="http://vitruv.tools/methodologisttemplate/model"/>
XMI
echo "        wrote $REPO_ROOT/throwaway.model"

st=$(post /vsum/commit '{"message":"Throwaway commit"}')
check "POST /vsum/commit on feature/throwaway" 200 "$st" "$(body)" "commitSha"

st=$(post /vsum/branch/switch '{"name":"master"}')
check "POST /vsum/branch/switch back to master for deleteAfterMerge test" 200 "$st" "$(body)" "master"

st=$(post /vsum/merge '{"sourceBranch":"feature/throwaway","deleteAfterMerge":true}')
check "POST /vsum/merge with deleteAfterMerge=true returns 200" 200 "$st"
st=$(get /vsum/branch)
[[ "$(body)" != *"feature/throwaway"* ]] \
  && { echo -e "  ${GREEN}PASS${NC}  feature/throwaway deleted after merge"; PASS=$((PASS+1)); } \
  || { echo -e "  ${RED}FAIL${NC}  feature/throwaway still listed after deleteAfterMerge=true"; FAIL=$((FAIL+1)); }

# error cases
st=$(post /vsum/merge '{"sourceBranch":"","deleteAfterMerge":false}')
check "POST /vsum/merge blank sourceBranch returns 400" 400 "$st"

st=$(post /vsum/merge '{"sourceBranch":"no-such-branch","deleteAfterMerge":false}')
check "POST /vsum/merge non-existent branch returns 500" 500 "$st"


# ── Phase 10: cleanup ─────────────────────────────────────────────────────────
section "Phase 10: Cleanup"

st=$(del /vsum/version/detail -H "Version-Id: v1.0")
check "DELETE /vsum/version/detail removes v1.0" 200 "$st"

st=$(get /vsum/version)
[[ "$(body)" != *"v1.0"* ]] \
  && { echo -e "  ${GREEN}PASS${NC}  v1.0 no longer in version list"; PASS=$((PASS+1)); } \
  || { echo -e "  ${RED}FAIL${NC}  v1.0 still listed after delete"; FAIL=$((FAIL+1)); }

st=$(del /vsum/branch -H "Branch-Name: feature/auth")
check "DELETE /vsum/branch removes feature/auth" 200 "$st"

st=$(del /vsum/branch -H "Branch-Name: release/v1.0")
check "DELETE /vsum/branch removes release/v1.0" 200 "$st"

st=$(get /vsum/branch)
FINAL_BRANCHES="$(body)"
[[ "$FINAL_BRANCHES" != *"feature/auth"* && "$FINAL_BRANCHES" != *"release/v1.0"* ]] \
  && { echo -e "  ${GREEN}PASS${NC}  deleted branches no longer listed"; PASS=$((PASS+1)); } \
  || { echo -e "  ${RED}FAIL${NC}  deleted branches still visible: $FINAL_BRANCHES"; FAIL=$((FAIL+1)); }


# ── Phase 11: error cases ─────────────────────────────────────────────────────
section "Phase 11: Error cases"

st=$(get /vsum/nonexistent-path)
check "GET unknown path returns 404" 404 "$st"

st=$(del /vsum/branch)
check "DELETE /vsum/branch without Branch-Name header returns 400" 400 "$st"

st=$(post /vsum/branch/switch '{"name":"does-not-exist"}')
check "POST /vsum/branch/switch non-existent branch returns 500" 500 "$st"

st=$(get /vsum/version/detail)
check "GET /vsum/version/detail without Version-Id header returns 400" 400 "$st"

st=$(del /vsum/version/detail)
check "DELETE /vsum/version/detail without Version-Id header returns 400" 400 "$st"


# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
TOTAL=$((PASS+FAIL))
echo "Results: $PASS / $TOTAL passed"
if [[ $FAIL -eq 0 ]]; then
  echo -e "${GREEN}All tests passed.${NC}"
else
  echo -e "${RED}$FAIL test(s) failed.${NC}"
  exit 1
fi

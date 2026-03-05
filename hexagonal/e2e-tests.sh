#!/usr/bin/env bash
# =============================================================================
# End-to-End Tests for ACME Banking Hexagonal Application
# =============================================================================
#
# Validates the complete banking API by exercising every endpoint in a
# realistic, sequential workflow: create customers, open accounts, deposit
# money, make transfers, issue cards, and verify transaction history.
#
# Usage:
#   ./e2e-tests.sh              # Build, start app, run tests, stop app
#   ./e2e-tests.sh --skip-build # Skip Maven build (app must be already built)
#   ./e2e-tests.sh --no-app     # Skip app lifecycle (app must be running)
#
# Starting the app manually (multi-module Maven project):
#   mvn clean package -DskipTests
#   java -jar banking-app/target/banking-app-0.1.0-SNAPSHOT.jar
#   # Then in another terminal:
#   ./e2e-tests.sh --no-app
#
# Note: "mvn spring-boot:run -pl banking-app" requires a prior "mvn install
# -DskipTests" because the banking-app module depends on sibling modules
# that must be available in the local Maven repository.
#
# Prerequisites: java 21, mvn, curl, jq
# =============================================================================

# Do NOT use set -e: we want assertions to fail without killing the script
set -uo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
BASE_URL="http://localhost:8080"
API="$BASE_URL/api"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_PID=""
SKIP_BUILD=false
NO_APP=false

for arg in "$@"; do
  case "$arg" in
    --skip-build) SKIP_BUILD=true ;;
    --no-app)     NO_APP=true ;;
  esac
done

# ---------------------------------------------------------------------------
# Colors
# ---------------------------------------------------------------------------
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
DIM='\033[2m'
RESET='\033[0m'

PASS=0
FAIL=0
TOTAL=0

# ---------------------------------------------------------------------------
# Prerequisites check
# ---------------------------------------------------------------------------
check_prerequisites() {
  local ok=true

  if ! command -v jq &>/dev/null; then
    echo -e "${RED}Error: jq is required. Install with: brew install jq${RESET}"
    ok=false
  fi

  if ! command -v curl &>/dev/null; then
    echo -e "${RED}Error: curl is required.${RESET}"
    ok=false
  fi

  if [[ "$NO_APP" == false ]]; then
    if [[ -z "${JAVA_HOME:-}" ]]; then
      echo -e "${RED}Error: JAVA_HOME is not set. Set it to a JDK 21 installation.${RESET}"
      ok=false
    elif [[ ! -x "$JAVA_HOME/bin/java" ]]; then
      echo -e "${RED}Error: JAVA_HOME points to $JAVA_HOME but no java executable found there.${RESET}"
      ok=false
    else
      local java_version
      java_version=$("$JAVA_HOME/bin/java" -version 2>&1 | head -1 | sed 's/.*"\([0-9]*\)\..*/\1/')
      if [[ "$java_version" -ne 21 ]]; then
        echo -e "${RED}Error: Java 21 is required, JAVA_HOME points to Java $java_version.${RESET}"
        echo -e "${RED}  JAVA_HOME=$JAVA_HOME${RESET}"
        ok=false
      fi
    fi

    if [[ "$SKIP_BUILD" == false ]]; then
      if ! command -v mvn &>/dev/null; then
        echo -e "${RED}Error: mvn is required for building. Use --skip-build to skip.${RESET}"
        ok=false
      fi
    fi
  fi

  if [[ "$ok" == false ]]; then
    exit 1
  fi
}

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

section() {
  echo ""
  echo -e "${CYAN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
  echo -e "${CYAN}${BOLD}  $1${RESET}"
  echo -e "${CYAN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
}

# Temp file for curl body (created once, reused)
CURL_BODY_FILE=$(mktemp)
trap 'rm -f "$CURL_BODY_FILE"; cleanup' EXIT

# Perform an HTTP request and set BODY + HTTP_CODE.
# Usage: http GET  /api/customers
#        http POST /api/customers '{"name":"x"}'
#        http PUT  /api/customers/1 '{"name":"y"}'
http() {
  local method="$1"
  local url="${BASE_URL}${2}"
  local data="${3:-}"

  if [[ -n "$data" ]]; then
    HTTP_CODE=$(curl -s -o "$CURL_BODY_FILE" -w "%{http_code}" \
      -X "$method" -H "Content-Type: application/json" -d "$data" "$url" 2>/dev/null) || HTTP_CODE="000"
  else
    HTTP_CODE=$(curl -s -o "$CURL_BODY_FILE" -w "%{http_code}" "$url" 2>/dev/null) || HTTP_CODE="000"
  fi

  BODY=$(<"$CURL_BODY_FILE")
}

# ---------------------------------------------------------------------------
# Assertion helpers (return 0 on success, 1 on failure with message)
# ---------------------------------------------------------------------------

# Assert a JSON field equals an expected string value
assert_eq() {
  local field="$1" expected="$2"
  local actual
  actual=$(echo "$BODY" | jq -r "$field")
  if [[ "$actual" == "$expected" ]]; then
    return 0
  fi
  echo -e "    ${RED}Expected $field = \"$expected\", got \"$actual\"${RESET}"
  return 1
}

# Assert a JSON field is not null and not empty
assert_present() {
  local field="$1"
  local actual
  actual=$(echo "$BODY" | jq -r "$field")
  if [[ "$actual" != "null" && -n "$actual" ]]; then
    return 0
  fi
  echo -e "    ${RED}Expected $field to be present, got \"$actual\"${RESET}"
  return 1
}

# Assert a JSON numeric field equals expected value (handles 1000 vs 1000.00)
assert_num() {
  local field="$1" expected="$2"
  local actual
  actual=$(echo "$BODY" | jq "$field")
  if echo "$actual $expected" | awk '{exit ($1+0 == $2+0) ? 0 : 1}'; then
    return 0
  fi
  echo -e "    ${RED}Expected $field = $expected, got $actual${RESET}"
  return 1
}

# Assert the response body is a non-empty JSON array
assert_array_not_empty() {
  local length
  length=$(echo "$BODY" | jq 'if type == "array" then length else 0 end')
  if [[ "$length" -gt 0 ]]; then
    return 0
  fi
  echo -e "    ${RED}Expected non-empty array, got length $length${RESET}"
  return 1
}

# Assert HTTP status code >= 400
assert_http_error() {
  if [[ "$HTTP_CODE" -ge 400 ]]; then
    return 0
  fi
  echo -e "    ${RED}Expected HTTP error (>=400), got $HTTP_CODE${RESET}"
  return 1
}

# ---------------------------------------------------------------------------
# Test runner
# ---------------------------------------------------------------------------

# Run a test: provide a name, description, and then call assertions.
# Usage:
#   test_start "Create customer" "POST /api/customers"
#   http POST /api/customers '...'
#   test_pass  assert_present .id  &&  assert_eq .firstName John
#
# test_pass runs the assertion commands and records pass/fail.
test_start() {
  TOTAL=$((TOTAL + 1))
  echo ""
  echo -e "  ${BOLD}Test #${TOTAL}: $1${RESET}"
  echo -e "  ${DIM}$2${RESET}"
}

test_pass() {
  if "$@"; then
    PASS=$((PASS + 1))
    echo -e "  ${GREEN}PASS${RESET}"
    return 0
  else
    FAIL=$((FAIL + 1))
    echo -e "  ${RED}FAIL${RESET}"
    return 1
  fi
}

# Convenience: run multiple assertions (all must pass)
check() {
  local ok=true
  for cmd in "$@"; do
    if ! eval "$cmd"; then
      ok=false
    fi
  done
  $ok
}

# ---------------------------------------------------------------------------
# Application lifecycle
# ---------------------------------------------------------------------------

cleanup() {
  if [[ -n "$APP_PID" ]]; then
    echo ""
    echo -e "${DIM}Stopping application (PID $APP_PID)...${RESET}"
    kill "$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
    echo -e "${DIM}Application stopped.${RESET}"
  fi
}

start_app() {
  if [[ "$NO_APP" == true ]]; then
    echo -e "${YELLOW}--no-app: assuming application is already running on $BASE_URL${RESET}"
    return
  fi

  if [[ "$SKIP_BUILD" == false ]]; then
    section "Building the application"
    echo -e "  ${DIM}mvn -f $SCRIPT_DIR/pom.xml clean package -DskipTests -q${RESET}"
    if ! mvn -f "$SCRIPT_DIR/pom.xml" clean package -DskipTests -q; then
      echo -e "  ${RED}Build failed.${RESET}"
      exit 1
    fi
    echo -e "  ${GREEN}Build successful.${RESET}"
  fi

  section "Starting the application"
  local jar
  jar=$(find "$SCRIPT_DIR/banking-app/target" -name "*.jar" -not -name "*-sources.jar" | head -1)
  if [[ -z "$jar" ]]; then
    echo -e "  ${RED}No JAR found in banking-app/target/. Run without --skip-build first.${RESET}"
    exit 1
  fi
  local java_cmd="$JAVA_HOME/bin/java"
  echo -e "  ${DIM}$java_cmd -jar $jar${RESET}"
  "$java_cmd" -jar "$jar" </dev/null >/dev/null 2>&1 &
  APP_PID=$!

  echo -e "  Waiting for application to start..."
  local retries=30
  while [[ $retries -gt 0 ]]; do
    local code
    # Check the API is actually responding (not just the server)
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 "$API/customers" 2>/dev/null || echo "000")
    if [[ "$code" != "000" ]]; then
      sleep 1  # Extra second for full initialization
      echo -e "  ${GREEN}Application is ready (PID $APP_PID).${RESET}"
      return
    fi
    # Check the process is still alive
    if ! kill -0 "$APP_PID" 2>/dev/null; then
      echo -e "  ${RED}Application process died unexpectedly.${RESET}"
      APP_PID=""
      exit 1
    fi
    sleep 2
    retries=$((retries - 1))
  done

  echo -e "  ${RED}Application failed to start within 60 seconds.${RESET}"
  exit 1
}

# ---------------------------------------------------------------------------
# Test scenarios
# ---------------------------------------------------------------------------

test_customer_management() {
  section "1. Customer Management"

  # --- Create customer ---
  test_start "Create a customer" \
    "POST /api/customers → expect CustomerResponse with id"
  http POST /api/customers '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "phone": "+33612345678"
  }'
  test_pass check \
    'assert_present .id' \
    'assert_eq .firstName John' \
    'assert_eq .lastName Doe' \
    'assert_eq .email john@example.com'
  CUSTOMER_ID=$(echo "$BODY" | jq -r '.id')

  # --- Get customer by ID ---
  test_start "Retrieve customer by ID" \
    "GET /api/customers/$CUSTOMER_ID"
  http GET "/api/customers/$CUSTOMER_ID"
  test_pass check \
    'assert_eq .id "$CUSTOMER_ID"' \
    'assert_eq .firstName John' \
    'assert_eq .email john@example.com'

  # --- Get customer by email ---
  test_start "Retrieve customer by email" \
    "GET /api/customers/by-email/john@example.com"
  http GET "/api/customers/by-email/john@example.com"
  test_pass check \
    'assert_eq .id "$CUSTOMER_ID"' \
    'assert_eq .firstName John'

  # --- Update customer ---
  test_start "Update customer" \
    "PUT /api/customers/$CUSTOMER_ID with new name and phone"
  http PUT "/api/customers/$CUSTOMER_ID" '{
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "john@example.com",
    "phone": "+33698765432"
  }'
  test_pass check \
    'assert_eq .firstName Jane' \
    'assert_eq .phone "+33698765432"'
}

test_account_management() {
  section "2. Account Management"

  # --- Open account ---
  test_start "Open a CHECKING account" \
    "POST /api/accounts → expect AccountResponse"
  http POST /api/accounts "{
    \"customerId\": $CUSTOMER_ID,
    \"type\": \"CHECKING\",
    \"accountNumber\": \"FR7612345000010000000000001\"
  }"
  test_pass check \
    'assert_present .id' \
    'assert_eq .accountNumber FR7612345000010000000000001' \
    'assert_eq .customerId "$CUSTOMER_ID"'
  ACCOUNT1_ID=$(echo "$BODY" | jq -r '.id')
  ACCOUNT1_NUMBER=$(echo "$BODY" | jq -r '.accountNumber')

  # --- Get account by ID ---
  test_start "Retrieve account by ID" \
    "GET /api/accounts/$ACCOUNT1_ID"
  http GET "/api/accounts/$ACCOUNT1_ID"
  test_pass check \
    'assert_eq .id "$ACCOUNT1_ID"' \
    'assert_eq .accountNumber "$ACCOUNT1_NUMBER"'

  # --- Get account by number ---
  test_start "Retrieve account by number" \
    "GET /api/accounts/by-number/$ACCOUNT1_NUMBER"
  http GET "/api/accounts/by-number/$ACCOUNT1_NUMBER"
  test_pass assert_eq .id "$ACCOUNT1_ID"

  # --- Get accounts by customer ---
  test_start "Retrieve accounts by customer" \
    "GET /api/accounts/by-customer/$CUSTOMER_ID → expect non-empty list"
  http GET "/api/accounts/by-customer/$CUSTOMER_ID"
  test_pass assert_array_not_empty

  # --- Get balance ---
  test_start "Check initial balance" \
    "GET /api/accounts/get-balance/$ACCOUNT1_ID → expect amount=0"
  http GET "/api/accounts/get-balance/$ACCOUNT1_ID"
  test_pass check \
    'assert_num .amount 0' \
    'assert_present .currency'
}

test_deposit_withdraw() {
  section "3. Deposits & Withdrawals"

  # --- Deposit 1000 EUR ---
  test_start "Deposit 1000 EUR" \
    "POST /api/accounts/$ACCOUNT1_ID/deposit → expect balance=1000"
  http POST "/api/accounts/$ACCOUNT1_ID/deposit" "{
    \"accountId\": $ACCOUNT1_ID,
    \"amount\": 1000,
    \"currency\": \"EUR\"
  }"
  test_pass assert_num .balanceAmount 1000

  # --- Deposit 500 EUR ---
  test_start "Deposit 500 EUR more" \
    "POST /api/accounts/$ACCOUNT1_ID/deposit → expect balance=1500"
  http POST "/api/accounts/$ACCOUNT1_ID/deposit" "{
    \"accountId\": $ACCOUNT1_ID,
    \"amount\": 500,
    \"currency\": \"EUR\"
  }"
  test_pass assert_num .balanceAmount 1500

  # --- Withdraw 200 EUR ---
  test_start "Withdraw 200 EUR" \
    "POST /api/accounts/$ACCOUNT1_ID/withdraw → expect balance=1300"
  http POST "/api/accounts/$ACCOUNT1_ID/withdraw" "{
    \"accountId\": $ACCOUNT1_ID,
    \"amount\": 200,
    \"currency\": \"EUR\"
  }"
  test_pass assert_num .balanceAmount 1300

  # --- Withdraw more than balance ---
  test_start "Withdraw more than balance (overdraft)" \
    "POST /api/accounts/$ACCOUNT1_ID/withdraw with amount=9999 → expect HTTP error"
  http POST "/api/accounts/$ACCOUNT1_ID/withdraw" "{
    \"accountId\": $ACCOUNT1_ID,
    \"amount\": 9999,
    \"currency\": \"EUR\"
  }"
  test_pass assert_http_error
}

test_transfers() {
  section "4. Transfers"

  # --- Setup: create 2nd customer + 2nd account + deposit ---
  echo -e "  ${DIM}Setting up second customer and account for transfers...${RESET}"
  http POST /api/customers '{
    "firstName": "Alice",
    "lastName": "Martin",
    "email": "alice@example.com",
    "phone": "+33611111111"
  }'
  CUSTOMER2_ID=$(echo "$BODY" | jq -r '.id')

  http POST /api/accounts "{
    \"customerId\": $CUSTOMER2_ID,
    \"type\": \"CHECKING\",
    \"accountNumber\": \"FR7612345000020000000000002\"
  }"
  ACCOUNT2_ID=$(echo "$BODY" | jq -r '.id')

  http POST "/api/accounts/$ACCOUNT2_ID/deposit" "{
    \"accountId\": $ACCOUNT2_ID,
    \"amount\": 500,
    \"currency\": \"EUR\"
  }"
  echo -e "  ${DIM}Account #$ACCOUNT2_ID funded with 500 EUR.${RESET}"

  # --- Initiate transfer ---
  test_start "Initiate a transfer of 100 EUR" \
    "POST /api/transfers → expect TransferResponse"
  http POST /api/transfers "{
    \"sourceAccountId\": $ACCOUNT1_ID,
    \"targetAccountId\": $ACCOUNT2_ID,
    \"amount\": 100,
    \"currency\": \"EUR\",
    \"reason\": \"Remboursement diner\"
  }"
  test_pass check \
    'assert_present .id' \
    'assert_eq .sourceAccountId "$ACCOUNT1_ID"' \
    'assert_eq .targetAccountId "$ACCOUNT2_ID"' \
    'assert_num .amountAmount 100'
  TRANSFER_ID=$(echo "$BODY" | jq -r '.id')

  # --- Get transfer by ID ---
  test_start "Retrieve transfer by ID" \
    "GET /api/transfers/$TRANSFER_ID"
  http GET "/api/transfers/$TRANSFER_ID"
  test_pass check \
    'assert_eq .id "$TRANSFER_ID"' \
    'assert_eq .reason "Remboursement diner"'

  # --- Execute transfer ---
  test_start "Execute the transfer" \
    "POST /api/transfers/$TRANSFER_ID/execute-transfer"
  http POST "/api/transfers/$TRANSFER_ID/execute-transfer" "{
    \"transferId\": $TRANSFER_ID
  }"
  test_pass assert_eq .id "$TRANSFER_ID"

  # --- Verify source balance ---
  test_start "Verify source balance after transfer" \
    "GET /api/accounts/get-balance/$ACCOUNT1_ID → expect 1200 (1300 - 100)"
  http GET "/api/accounts/get-balance/$ACCOUNT1_ID"
  test_pass assert_num .amount 1200

  # --- Verify target balance ---
  test_start "Verify target balance after transfer" \
    "GET /api/accounts/get-balance/$ACCOUNT2_ID → expect 600 (500 + 100)"
  http GET "/api/accounts/get-balance/$ACCOUNT2_ID"
  test_pass assert_num .amount 600

  # --- Get transfers by account ---
  test_start "Retrieve transfers by account" \
    "GET /api/transfers/by-account/$ACCOUNT1_ID → expect non-empty list"
  http GET "/api/transfers/by-account/$ACCOUNT1_ID"
  test_pass assert_array_not_empty
}

test_cards() {
  section "5. Cards"

  # --- Issue card ---
  test_start "Issue a card" \
    "POST /api/cards"
  http POST "/api/cards" "{\"accountId\": $ACCOUNT1_ID, \"cardNumber\": \"4111111111111111\", \"expiryDate\": \"2028-12-31\", \"cvv\": \"123\", \"amount\": 1000.00, \"currency\": \"USD\"}"
  test_pass check \
    'assert_present .id' \
    'assert_eq .cardNumber 4111111111111111'
  CARD_ID=$(echo "$BODY" | jq -r '.id')

  # --- Get cards by account ---
  test_start "Retrieve cards by account" \
    "GET /api/cards/by-account/$ACCOUNT1_ID"
  http GET "/api/cards/by-account/$ACCOUNT1_ID"
  test_pass assert_array_not_empty

  # --- Block card ---
  test_start "Block the card" \
    "POST /api/cards/$CARD_ID/block-card"
  http POST "/api/cards/$CARD_ID/block-card" "{\"cardId\": $CARD_ID}"
  test_pass assert_eq .id "$CARD_ID"

  # --- Activate card ---
  test_start "Activate the card" \
    "POST /api/cards/$CARD_ID/activate-card"
  http POST "/api/cards/$CARD_ID/activate-card" "{\"cardId\": $CARD_ID}"
  test_pass assert_eq .id "$CARD_ID"
}

test_transactions() {
  section "6. Transaction History"

  test_start "Retrieve transaction history for account" \
    "GET /api/transactions/get-history?accountId=$ACCOUNT1_ID → expect non-empty list"
  http GET "/api/transactions/get-history?accountId=$ACCOUNT1_ID"
  test_pass assert_array_not_empty
}

test_error_handling() {
  section "7. Error Handling"

  # --- Non-existent account ---
  test_start "Get non-existent account" \
    "GET /api/accounts/999 → expect HTTP error"
  http GET "/api/accounts/999"
  test_pass assert_http_error

  # --- Non-existent customer ---
  test_start "Get non-existent customer" \
    "GET /api/customers/999 → expect HTTP error"
  http GET "/api/customers/999"
  test_pass assert_http_error
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

echo ""
echo -e "${BOLD}╔══════════════════════════════════════════════════════════════════════════════╗${RESET}"
echo -e "${BOLD}║            ACME Banking - End-to-End API Tests                              ║${RESET}"
echo -e "${BOLD}║            Hexagonal Architecture with HexaGlue                             ║${RESET}"
echo -e "${BOLD}╚══════════════════════════════════════════════════════════════════════════════╝${RESET}"

check_prerequisites
start_app

test_customer_management
test_account_management
test_deposit_withdraw
test_transfers
test_cards
test_transactions
test_error_handling

# ---------------------------------------------------------------------------
# Final report
# ---------------------------------------------------------------------------
echo ""
echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
echo -e "${BOLD}  Results: ${GREEN}$PASS passed${RESET} ${BOLD}/ ${RED}$FAIL failed${RESET} ${BOLD}/ $TOTAL total${RESET}"
echo -e "${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"

if [[ $FAIL -eq 0 ]]; then
  echo -e "  ${GREEN}${BOLD}All tests passed!${RESET}"
  exit 0
else
  echo -e "  ${RED}${BOLD}Some tests failed.${RESET}"
  exit 1
fi

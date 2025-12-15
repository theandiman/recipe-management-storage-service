#!/bin/bash

# Integration test for recipe sharing functionality
# Tests: Create recipe -> Set to public -> Verify -> Set to private -> Delete

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get service URL and Firebase token from command line
SERVICE_URL="${1:-http://localhost:8081}"

if [ -z "$FIREBASE_TOKEN" ]; then
    echo -e "${RED}Error: FIREBASE_TOKEN environment variable is not set.${NC}" >&2
    echo "Usage: export FIREBASE_TOKEN=<your-token> && $0 [SERVICE_URL]" >&2
    exit 1
fi

echo "=========================================="
echo "Recipe Sharing Integration Test"
echo "=========================================="
echo "Service URL: $SERVICE_URL"

# This RECIPE_ID variable will be populated by the test
RECIPE_ID=""

# Function to clean up the test recipe.
# The trap ensures this runs on any script exit.
cleanup() {
  if [ -n "$RECIPE_ID" ]; then
    echo ""
    echo -e "${YELLOW}Cleaning up test recipe $RECIPE_ID...${NC}"
    # Attempt to delete the recipe. `|| true` prevents the script from failing if cleanup fails.
    curl -s -X DELETE "$SERVICE_URL/api/recipes/$RECIPE_ID" \
      -H "Authorization: Bearer $FIREBASE_TOKEN" -o /dev/null || true
  fi
}
trap cleanup EXIT

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Test 1: Create a recipe
echo -e "${BLUE}Test 1: Creating a test recipe...${NC}"
CREATE_RESPONSE=$(curl -s -X POST "$SERVICE_URL/api/recipes" \
  -H "Authorization: Bearer $FIREBASE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Integration Test Recipe",
    "description": "Testing recipe sharing functionality",
    "ingredients": ["Test ingredient 1", "Test ingredient 2"],
    "instructions": ["Step 1", "Step 2"],
    "prepTime": 10,
    "cookTime": 20,
    "servings": 2,
    "source": "integration-test",
    "isPublic": false
  }')

RECIPE_ID=$(echo "$CREATE_RESPONSE" | jq -r '.id')

if [ -z "$RECIPE_ID" ] || [ "$RECIPE_ID" = "null" ]; then
    echo -e "${RED}✗ FAILED${NC} - Could not create recipe"
    echo "Response: $CREATE_RESPONSE"
    exit 1
fi

IS_PUBLIC=$(echo "$CREATE_RESPONSE" | jq -r '.isPublic')
if [ "$IS_PUBLIC" = "false" ]; then
    echo -e "${GREEN}✓ PASSED${NC} - Recipe created with ID: $RECIPE_ID (isPublic: false)"
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ FAILED${NC} - Recipe isPublic should be false, got: $IS_PUBLIC"
    ((TESTS_FAILED++))
fi

echo ""

# Test 2: Set recipe to public
echo -e "${BLUE}Test 2: Setting recipe to public...${NC}"
SHARE_RESPONSE=$(curl -s -X PATCH "$SERVICE_URL/api/recipes/$RECIPE_ID/sharing" \
  -H "Authorization: Bearer $FIREBASE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"isPublic": true}')

IS_PUBLIC_AFTER_SHARE=$(echo "$SHARE_RESPONSE" | jq -r '.isPublic')
if [ "$IS_PUBLIC_AFTER_SHARE" = "true" ]; then
    echo -e "${GREEN}✓ PASSED${NC} - Recipe set to public (isPublic: true)"
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ FAILED${NC} - Recipe isPublic should be true after sharing, got: $IS_PUBLIC_AFTER_SHARE"
    echo "Response: $SHARE_RESPONSE"
    ((TESTS_FAILED++))
fi

echo ""

# Test 3: Retrieve recipe and verify it's public
echo -e "${BLUE}Test 3: Retrieving recipe to verify public status persists...${NC}"
GET_RESPONSE=$(curl -s -X GET "$SERVICE_URL/api/recipes/$RECIPE_ID" \
  -H "Authorization: Bearer $FIREBASE_TOKEN")

IS_PUBLIC_AFTER_GET=$(echo "$GET_RESPONSE" | jq -r '.isPublic')
if [ "$IS_PUBLIC_AFTER_GET" = "true" ]; then
    echo -e "${GREEN}✓ PASSED${NC} - Recipe isPublic persisted correctly (isPublic: true)"
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ FAILED${NC} - Recipe isPublic should be true after retrieval, got: $IS_PUBLIC_AFTER_GET"
    echo "Response: $GET_RESPONSE"
    ((TESTS_FAILED++))
fi

echo ""

# Test 4: Verify recipe appears in public recipes list
echo -e "${BLUE}Test 4: Verifying recipe appears in public recipes list...${NC}"
PUBLIC_LIST=$(curl -s -X GET "$SERVICE_URL/api/recipes/public")
FOUND_IN_PUBLIC=$(echo "$PUBLIC_LIST" | jq -r --arg id "$RECIPE_ID" '.[] | select(.id == $id) | .id')

if [ "$FOUND_IN_PUBLIC" = "$RECIPE_ID" ]; then
    echo -e "${GREEN}✓ PASSED${NC} - Recipe found in public recipes list"
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ FAILED${NC} - Recipe not found in public recipes list"
    ((TESTS_FAILED++))
fi

echo ""

# Test 5: Set recipe back to private
echo -e "${BLUE}Test 5: Setting recipe back to private...${NC}"
UNSHARE_RESPONSE=$(curl -s -X PATCH "$SERVICE_URL/api/recipes/$RECIPE_ID/sharing" \
  -H "Authorization: Bearer $FIREBASE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"isPublic": false}')

IS_PUBLIC_AFTER_UNSHARE=$(echo "$UNSHARE_RESPONSE" | jq -r '.isPublic')
if [ "$IS_PUBLIC_AFTER_UNSHARE" = "false" ]; then
    echo -e "${GREEN}✓ PASSED${NC} - Recipe set to private (isPublic: false)"
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ FAILED${NC} - Recipe isPublic should be false after unsharing, got: $IS_PUBLIC_AFTER_UNSHARE"
    echo "Response: $UNSHARE_RESPONSE"
    ((TESTS_FAILED++))
fi

echo ""

# Test 6: Retrieve recipe again and verify it's private
echo -e "${BLUE}Test 6: Retrieving recipe to verify private status persists...${NC}"
GET_RESPONSE2=$(curl -s -X GET "$SERVICE_URL/api/recipes/$RECIPE_ID" \
  -H "Authorization: Bearer $FIREBASE_TOKEN")

IS_PUBLIC_FINAL=$(echo "$GET_RESPONSE2" | jq -r '.isPublic')
if [ "$IS_PUBLIC_FINAL" = "false" ]; then
    echo -e "${GREEN}✓ PASSED${NC} - Recipe isPublic persisted correctly (isPublic: false)"
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ FAILED${NC} - Recipe isPublic should be false after retrieval, got: $IS_PUBLIC_FINAL"
    echo "Response: $GET_RESPONSE2"
    ((TESTS_FAILED++))
fi

echo ""

# Test 7: Delete the test recipe
echo -e "${BLUE}Test 7: Deleting test recipe...${NC}"
DELETE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$SERVICE_URL/api/recipes/$RECIPE_ID" \
  -H "Authorization: Bearer $FIREBASE_TOKEN")

if [ "$DELETE_STATUS" = "204" ] || [ "$DELETE_STATUS" = "200" ]; then
    echo -e "${GREEN}✓ PASSED${NC} - Recipe deleted successfully (HTTP $DELETE_STATUS)"
    ((TESTS_PASSED++))
else
    echo -e "${RED}✗ FAILED${NC} - Failed to delete recipe (HTTP $DELETE_STATUS)"
    ((TESTS_FAILED++))
fi

echo ""
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo -e "Tests Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Tests Failed: ${RED}$TESTS_FAILED${NC}"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All integration tests passed!${NC}"
    echo -e "${GREEN}Recipe sharing functionality is working correctly.${NC}"
    exit 0
else
    echo -e "${RED}✗ Some tests failed${NC}"
    echo -e "${RED}Recipe sharing may not be persisting correctly.${NC}"
    exit 1
fi

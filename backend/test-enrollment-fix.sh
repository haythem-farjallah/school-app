#!/bin/bash

# Test script to verify enrollment functionality works after fixes

echo "Testing enrollment functionality..."

# Test 1: Preview auto enrollment (should not cause database errors)
echo "1. Testing preview auto enrollment..."
curl -X GET "http://localhost:8088/api/v1/enrollments/auto-enroll/preview" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo ""

# Test 2: Get enrollments (should work with enum filtering)
echo "2. Testing get enrollments..."
curl -X GET "http://localhost:8088/api/v1/enrollments?page=0&size=5" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo ""

# Test 3: Check if we can query by status (this was failing before)
echo "3. Testing enrollment status filtering..."
curl -X GET "http://localhost:8088/api/v1/enrollments?status=ACTIVE&page=0&size=5" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n" \
  -s

echo ""
echo "Test completed. Check the responses above for any errors."

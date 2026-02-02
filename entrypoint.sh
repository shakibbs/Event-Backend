#!/bin/bash
set -e

# Debug: Print environment variables to verify they're being passed
echo ""
echo "========================================="
echo "=== Environment Variables Debug ==="
echo "========================================="
echo "PORT: ${PORT:-NOT_SET}"
echo "DATABASE_URL: ${DATABASE_URL:-NOT_SET}"
echo "DATABASE_USERNAME: ${DATABASE_USERNAME:-NOT_SET}"
if [ -n "$DATABASE_PASSWORD" ]; then
  echo "DATABASE_PASSWORD: [SET - ${#DATABASE_PASSWORD} chars]"
else
  echo "DATABASE_PASSWORD: NOT_SET"
fi
if [ -n "$JWT_SECRET" ]; then
  echo "JWT_SECRET: [SET - ${#JWT_SECRET} chars]"
else
  echo "JWT_SECRET: NOT_SET"
fi
echo "========================================="
echo ""

# Run Java with environment variables
exec java \
    -Dserver.port=${PORT:-8080} \
    -Dspring.datasource.url=${DATABASE_URL:-jdbc:mysql://localhost:3306/event_management_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC} \
    -Dspring.datasource.username=${DATABASE_USERNAME:-root} \
    -Dspring.datasource.password=${DATABASE_PASSWORD:-765614} \
    -Dapp.jwt.secret=${JWT_SECRET:-your-super-secret-key-minimum-32-characters-change-in-production-1234567890} \
    -jar app.jar

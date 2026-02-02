#!/bin/bash

# Debug: Print environment variables to verify they're being passed
echo "=== Environment Variables ==="
echo "PORT: $PORT"
echo "DATABASE_URL: $DATABASE_URL"
echo "DATABASE_USERNAME: $DATABASE_USERNAME"
echo "DATABASE_PASSWORD: $DATABASE_PASSWORD"
echo "JWT_SECRET: $JWT_SECRET"
echo "=== End Environment Variables ==="

# Run Java with environment variables
exec java \
    -Dserver.port=${PORT:-8080} \
    -Dspring.datasource.url=${DATABASE_URL:-jdbc:mysql://localhost:3306/event_management_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC} \
    -Dspring.datasource.username=${DATABASE_USERNAME:-root} \
    -Dspring.datasource.password=${DATABASE_PASSWORD:-765614} \
    -Dapp.jwt.secret=${JWT_SECRET:-your-super-secret-key-minimum-32-characters-change-in-production-1234567890} \
    -jar app.jar

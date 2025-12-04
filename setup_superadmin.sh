#!/bin/bash
# Database setup script - Creates test superadmin user
# Note: Replace credentials with your MySQL connection details

# MySQL connection details
DB_HOST="localhost"
DB_USER="root"
DB_PASS="765614"
DB_NAME="event_management_db"

# Check if superadmin role exists, if not create it
mysql -h $DB_HOST -u $DB_USER -p$DB_PASS $DB_NAME << EOF
-- 1. Insert SUPERADMIN role if not exists
INSERT IGNORE INTO app_roles (id, name, status, created_at, created_by, updated_at, updated_by)
VALUES (1, 'SUPERADMIN', 'ACTIVE', NOW(), 'SYSTEM', NOW(), 'SYSTEM');

-- 2. Get all permissions and assign to SUPERADMIN
INSERT IGNORE INTO role_permissions (role_id, permission_id, status, created_at, created_by, updated_at, updated_by)
SELECT 1, id, 'ACTIVE', NOW(), 'SYSTEM', NOW(), 'SYSTEM' FROM app_permissions;

-- 3. Insert SUPERADMIN user if not exists
INSERT IGNORE INTO app_users (id, full_name, email, password, role_id, status, created_at, created_by, updated_at, updated_by)
VALUES (
  1,
  'Super Admin',
  'superadmin@example.com',
  '\$2a\$12\$D9Z7k6j8Z.6k8j6Z.6k8j6Z.6k8j6Z.6k8j6Z.6k8j6Z.6k8j6Z.6k8',
  1,
  'ACTIVE',
  NOW(),
  'SYSTEM',
  NOW(),
  'SYSTEM'
);

-- 4. Display created user
SELECT id, full_name, email, role_id, status FROM app_users WHERE email = 'superadmin@example.com';

EOF

echo "Database setup completed!"

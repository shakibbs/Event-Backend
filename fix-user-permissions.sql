-- Fix user permissions for SuperAdmin role
-- This script ensures SuperAdmin has all necessary permissions

-- First, let's check if permissions exist
INSERT IGNORE INTO event_permissions (name, description, created_at, created_by, updated_at, updated_by, deleted) VALUES
('user.manage.all', 'Can manage all users in system', NOW(), 'system', NOW(), 'system', FALSE),
('role.manage.all', 'Can manage all roles in system', NOW(), 'system', NOW(), 'system', FALSE),
('event.manage.all', 'Can manage all events in system', NOW(), 'system', NOW(), 'system', FALSE),
('event.approve', 'Can approve pending events', NOW(), 'system', NOW(), 'system', FALSE),
('event.hold', 'Can hold/pause events', NOW(), 'system', NOW(), 'system', FALSE),
('event.reactivate', 'Can reactivate held events', NOW(), 'system', NOW(), 'system', FALSE),
('system.config', 'Can configure system settings', NOW(), 'system', NOW(), 'system', FALSE),
('history.view.all', 'Can view all users'' history (login, password, activity)', NOW(), 'system', NOW(), 'system', FALSE),
('user.manage.own', 'Can manage own users/team', NOW(), 'system', NOW(), 'system', FALSE),
('event.manage.own', 'Can manage own events', NOW(), 'system', NOW(), 'system', FALSE),
('event.view.all', 'Can view all events', NOW(), 'system', NOW(), 'system', FALSE),
('event.invite', 'Can invite users to events', NOW(), 'system', NOW(), 'system', FALSE),
('event.view.public', 'Can view public events', NOW(), 'system', NOW(), 'system', FALSE),
('event.view.invited', 'Can view invited events', NOW(), 'system', NOW(), 'system', FALSE),
('event.attend', 'Can attend events', NOW(), 'system', NOW(), 'system', FALSE),
('history.view.own', 'Can view own history (login, password, activity)', NOW(), 'system', NOW(), 'system', FALSE);

-- Ensure SuperAdmin role exists
INSERT IGNORE INTO event_roles (name, created_at, created_by, updated_at, updated_by, deleted) VALUES
('SuperAdmin', NOW(), 'system', NOW(), 'system', FALSE),
('Admin', NOW(), 'system', NOW(), 'system', FALSE),
('Attendee', NOW(), 'system', NOW(), 'system', FALSE);

-- Assign all permissions to SuperAdmin role
INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at, created_by, updated_at, updated_by, deleted)
SELECT r.id, p.id, NOW(), 'system', NOW(), 'system', FALSE
FROM event_roles r, event_permissions p
WHERE r.name = 'SuperAdmin';

-- Assign specific permissions to Admin role
INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at, created_by, updated_at, updated_by, deleted)
SELECT r.id, p.id, NOW(), 'system', NOW(), 'system', FALSE
FROM event_roles r, event_permissions p
WHERE r.name = 'Admin' AND p.name IN (
    'user.manage.own', 'event.manage.own', 'event.view.all', 'event.invite',
    'event.approve', 'history.view.own'
);

-- Assign specific permissions to Attendee role
INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at, created_by, updated_at, updated_by, deleted)
SELECT r.id, p.id, NOW(), 'system', NOW(), 'system', FALSE
FROM event_roles r, event_permissions p
WHERE r.name = 'Attendee' AND p.name IN (
    'event.view.public', 'event.view.invited', 'event.attend', 'history.view.own'
);

-- Ensure SuperAdmin user has SuperAdmin role
UPDATE event_users u
SET role_id = (SELECT id FROM event_roles WHERE name = 'SuperAdmin')
WHERE email = 'superadmin@ems.com' OR email = 'admin@example.com';
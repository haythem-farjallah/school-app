-- Seed PERMISSIONS_MANAGE and assign to ADMIN role defaults

-- 1) Insert permission into catalogue if missing
INSERT INTO permissions (code, description)
SELECT 'PERMISSIONS_MANAGE', 'Manage permissions UI and APIs'
WHERE NOT EXISTS (
  SELECT 1 FROM permissions WHERE code = 'PERMISSIONS_MANAGE'
);

-- 2) Assign to ADMIN defaults if not already present
INSERT INTO role_permissions (role, permission_id)
SELECT 'ADMIN', p.id
FROM permissions p
WHERE p.code = 'PERMISSIONS_MANAGE'
AND NOT EXISTS (
  SELECT 1 FROM role_permissions rp WHERE rp.role = 'ADMIN' AND rp.permission_id = p.id
);



START TRANSACTION;

DELETE FROM access_events WHERE user_id IS NOT NULL;
DELETE FROM qr_credentials;
DELETE FROM access_schedules;
DELETE FROM user_door_permissions;
DELETE FROM users;

COMMIT;

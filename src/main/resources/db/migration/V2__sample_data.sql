-- V2__sample_data.sql
-- Insert Sample Users
INSERT INTO users (name, email, role, created_at) VALUES
('Admin User', 'admin@warehouse.com', 'ADMIN', CURRENT_TIMESTAMP),
('John Inspector', 'john.inspector@warehouse.com', 'INSPECTOR', CURRENT_TIMESTAMP),
('Sarah Supervisor', 'sarah.supervisor@warehouse.com', 'SUPERVISOR', CURRENT_TIMESTAMP),
('Mike Inspector', 'mike.inspector@warehouse.com', 'INSPECTOR', CURRENT_TIMESTAMP);

-- Insert Sample Inspections
INSERT INTO inspections (image_path, predicted_class, confidence, severity, status, description, created_at, updated_at, assigned_to, resolved_at) VALUES
('uploads/sample_dent.jpg', 'DENT', 0.94, 'MEDIUM', 'OPEN', 'Dent observed on upper corner of outer packaging', CURRENT_TIMESTAMP - INTERVAL '2 DAYS', CURRENT_TIMESTAMP - INTERVAL '2 DAYS', NULL, NULL),
('uploads/sample_puncture.jpg', 'PUNCTURE', 0.96, 'HIGH', 'ESCALATED', 'Sharp puncture hole on side panel, internal contents potentially affected', CURRENT_TIMESTAMP - INTERVAL '1 DAY', CURRENT_TIMESTAMP - INTERVAL '12 HOURS', 3, NULL),
('uploads/sample_crushed.jpg', 'CRUSHED', 0.89, 'HIGH', 'IN_PROGRESS', 'Bottom corner severely crushed due to heavy stacking', CURRENT_TIMESTAMP - INTERVAL '5 HOURS', CURRENT_TIMESTAMP - INTERVAL '2 HOURS', 2, NULL),
('uploads/sample_nodamage.jpg', 'NO_DAMAGE', 0.98, 'LOW', 'RESOLVED', 'Package passed quality inspection standard', CURRENT_TIMESTAMP - INTERVAL '3 DAYS', CURRENT_TIMESTAMP - INTERVAL '2 DAYS', 2, CURRENT_TIMESTAMP - INTERVAL '2 DAYS');

-- Insert Sample Audit Logs
INSERT INTO audit_logs (inspection_id, action, performed_by, timestamp, description) VALUES
(1, 'CREATED', 'System', CURRENT_TIMESTAMP - INTERVAL '2 DAYS', 'Inspection created from uploaded image with prediction DENT'),
(2, 'CREATED', 'System', CURRENT_TIMESTAMP - INTERVAL '1 DAY', 'Inspection created from uploaded image with prediction PUNCTURE'),
(2, 'ASSIGNED', 'Admin User', CURRENT_TIMESTAMP - INTERVAL '18 HOURS', 'Assigned inspection to Sarah Supervisor'),
(2, 'ESCALATED', 'Sarah Supervisor', CURRENT_TIMESTAMP - INTERVAL '12 HOURS', 'Escalated due to risk of internal contents damage'),
(3, 'CREATED', 'System', CURRENT_TIMESTAMP - INTERVAL '5 HOURS', 'Inspection created from uploaded image with prediction CRUSHED'),
(3, 'ASSIGNED', 'Admin User', CURRENT_TIMESTAMP - INTERVAL '2 HOURS', 'Assigned inspection to John Inspector'),
(4, 'CREATED', 'System', CURRENT_TIMESTAMP - INTERVAL '3 DAYS', 'Inspection created from uploaded image with prediction NO_DAMAGE'),
(4, 'RESOLVED', 'John Inspector', CURRENT_TIMESTAMP - INTERVAL '2 DAYS', 'Verified no damage to package and closed inspection');

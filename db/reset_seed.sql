SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE appointment;
TRUNCATE TABLE doctor;
TRUNCATE TABLE patient;
TRUNCATE TABLE patients;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO patient (name, email, password, phone, title, role) VALUES
('Admin Main', 'admin@gmail.com', '$2a$10$VvupTD7atWf1UHYpayw3g.qSKSV.62CyoAL6lza1h6RSm3HJrgKq.', '9000000001', 'Mr.', 'ADMIN'),
('Riya Desk', 'reception1@smartqueue.local', '$2a$10$VvupTD7atWf1UHYpayw3g.qSKSV.62CyoAL6lza1h6RSm3HJrgKq.', '9000000002', 'Ms.', 'RECEPTIONIST'),
('Arjun Desk', 'reception2@smartqueue.local', '$2a$10$VvupTD7atWf1UHYpayw3g.qSKSV.62CyoAL6lza1h6RSm3HJrgKq.', '9000000003', 'Mr.', 'RECEPTIONIST'),
('Kavya Patient', 'patient1@example.com', '$2a$10$VvupTD7atWf1UHYpayw3g.qSKSV.62CyoAL6lza1h6RSm3HJrgKq.', '9000000004', 'Ms.', 'PATIENT'),
('Rohan Patient', 'patient2@example.com', '$2a$10$VvupTD7atWf1UHYpayw3g.qSKSV.62CyoAL6lza1h6RSm3HJrgKq.', '9000000005', 'Mr.', 'PATIENT');

INSERT INTO doctor (name, email, password, specialization, approved) VALUES
('Dr. Meera Iyer', 'meera.iyer@clinic.com', '$2a$10$VvupTD7atWf1UHYpayw3g.qSKSV.62CyoAL6lza1h6RSm3HJrgKq.', 'General Medicine', b'1'),
('Dr. Vikram Shah', 'vikram.shah@clinic.com', '$2a$10$VvupTD7atWf1UHYpayw3g.qSKSV.62CyoAL6lza1h6RSm3HJrgKq.', 'Orthopedics', b'1'),
('Dr. Nisha Rao', 'nisha.rao@clinic.com', '$2a$10$VvupTD7atWf1UHYpayw3g.qSKSV.62CyoAL6lza1h6RSm3HJrgKq.', 'Dermatology', b'1');

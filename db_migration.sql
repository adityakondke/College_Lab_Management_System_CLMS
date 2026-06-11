-- ============================================================
-- CLMS DATABASE MIGRATION + MERGE WITH TimetableApp
-- Run this on: lab_management_new
-- ============================================================

-- Step 1: Backup old timetable
RENAME TABLE timetable TO timetable_old;

-- Step 2: Create new timetable table (TimetableApp schema merged with CLMS)
CREATE TABLE timetable (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  entry_date  VARCHAR(20)  NOT NULL,
  day_name    VARCHAR(20)  NOT NULL,
  slot        INT          NOT NULL,
  batch       VARCHAR(20),
  course_name VARCHAR(100),
  faculty_code VARCHAR(100),
  lab_name    VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS uploaded_files (
  id INT AUTO_INCREMENT PRIMARY KEY,
  filename VARCHAR(255),
  uploader VARCHAR(100),
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  row_count INT,
  status VARCHAR(50),
  message VARCHAR(255)
);

-- Step 3: Add faculty_confirmed column to issues
ALTER TABLE issues ADD COLUMN faculty_confirmed VARCHAR(10) DEFAULT NULL;

-- Step 4: Add CLASS_TEACHER role support
-- CLASS_TEACHER is a new user role with Excel timetable upload authority
-- Users table already has 'role' column - CLASS_TEACHER is a valid value
-- Role values: 'CLASS_TEACHER', 'TECHNICIAN', 'FACULTY', 'HOD'
-- No schema change needed for users table

-- Step 5: Create test CLASS_TEACHER user (optional - remove if not needed)
-- Email: teacher@clms.edu
-- Password: teacher123
-- Lab Code: ALL (can upload timetables for all labs)
INSERT INTO users (username, email, password, role, lab_code, phone) 
VALUES ('classTeacher1', 'teacher@clms.edu', 'teacher123', 'CLASS_TEACHER', 'ALL', '9876543210')
ON DUPLICATE KEY UPDATE role='CLASS_TEACHER';

-- Step 6: Verify migration completion
-- Run these SELECTs to verify:
-- SELECT COUNT(*) FROM timetable;
-- SELECT * FROM timetable LIMIT 5;
-- SELECT * FROM users WHERE role='CLASS_TEACHER';
-- SELECT * FROM issues WHERE faculty_confirmed IS NOT NULL LIMIT 5;

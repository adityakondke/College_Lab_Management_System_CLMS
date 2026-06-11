-- ============================================================
-- FIX TIMETABLE UPLOAD - CREATE/FIX TABLES
-- Run this on: lab_management_new
-- ============================================================

-- Check if timetable table exists, if not create it
CREATE TABLE IF NOT EXISTS timetable (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  entry_date  VARCHAR(20)  NOT NULL,
  day_name    VARCHAR(20)  NOT NULL,
  slot        INT          NOT NULL,
  batch       VARCHAR(20),
  course_name VARCHAR(100),
  faculty_code VARCHAR(100),
  lab_name    VARCHAR(100),
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create index for faster queries
CREATE INDEX idx_entry_date ON timetable(entry_date);
CREATE INDEX idx_slot ON timetable(slot);
CREATE INDEX idx_batch ON timetable(batch);

-- Check if issues table has faculty_confirmed column
ALTER TABLE issues ADD COLUMN faculty_confirmed VARCHAR(10) DEFAULT NULL;

-- Verify CLASS_TEACHER user exists
INSERT INTO users (username, email, password, role, lab_code, phone) 
VALUES ('classTeacher1', 'teacher@clms.edu', 'teacher123', 'CLASS_TEACHER', 'ALL', '9876543210')
ON DUPLICATE KEY UPDATE role='CLASS_TEACHER';

-- Verify data was created
SELECT 'Timetable table status:' as Status;
SELECT COUNT(*) as timetable_rows FROM timetable;

SELECT 'Users with CLASS_TEACHER role:' as Status;
SELECT id, username, email, role FROM users WHERE role='CLASS_TEACHER';

SELECT 'Sample timetable data:' as Status;
SELECT * FROM timetable LIMIT 3;

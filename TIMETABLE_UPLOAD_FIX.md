# Timetable Upload Fix - Complete Guide

## Problem
- Upload feature not working
- Multiple databases exist (original + new project database)
- Need to migrate/consolidate tables to correct database

## Solution Steps

### Step 1: Identify the Databases
The current configuration uses: `lab_management_new`

You mentioned another database exists with upload/timetable tables. Identify it:
- Check MySQL: What's the name of the other database?
- Likely candidates: `TimetableApp`, `upload_db`, `timetable_db`, etc.

### Step 2: Run Database Setup Script

Execute `fix_timetable_upload.sql` on `lab_management_new`:
```sql
-- This creates the timetable table if it doesn't exist
-- Creates indexes for performance
-- Ensures CLASS_TEACHER user exists
```

**Via Command Line:**
```bash
mysql -u root -h localhost lab_management_new < fix_timetable_upload.sql
```

**Via MySQL Workbench or phpMyAdmin:**
1. Open the database: `lab_management_new`
2. Run the SQL script: `fix_timetable_upload.sql`

### Step 3: Migrate Data from Other Database (If Needed)

If timetable data exists in another database, run this migration script:

```sql
-- Replace 'other_database' with the actual database name
INSERT INTO lab_management_new.timetable 
SELECT * FROM other_database.timetable;
```

### Step 4: Verify Setup

After running the scripts, verify:

```sql
-- Check timetable table exists and has structure
DESCRIBE lab_management_new.timetable;

-- Check table has data or is empty and ready
SELECT COUNT(*) FROM lab_management_new.timetable;

-- Check CLASS_TEACHER user exists
SELECT * FROM lab_management_new.users WHERE role='CLASS_TEACHER';
```

### Step 5: Test Upload

1. Login with CLASS_TEACHER credentials:
   - Email: teacher@clms.edu
   - Password: teacher123

2. Go to "Upload Timetable" section

3. Create Excel file with columns:
   - Entry Date (dd/MM/yyyy format)
   - Day Name (Monday, Tuesday, etc.)
   - Slot (1, 2, or 3)
   - Batch (e.g., A1, B2)
   - Course Name (e.g., OOP, Database)
   - Faculty Code (e.g., F001)
   - Lab Name (e.g., Lab-A)

4. Upload the file

5. You should see success message with count of entries added

## Database Info

**Current Connection:**
- Host: localhost
- Port: 3306
- Database: lab_management_new
- User: root
- Password: (empty)

**File: DBConnection.java**
- Location: `/webapps/CLMS/DBConnection.java`
- Change `URL` if database name changes

## Troubleshooting

**Error: "Database connection failed"**
- Check MySQL is running
- Verify database name in DBConnection.java matches actual database
- Verify user/password credentials

**Error: "Only Excel files allowed"**
- Make sure file is .xlsx or .xls format
- File size under 10MB

**Upload seems to work but no data appears**
- Check timetable table exists: `DESCRIBE lab_management_new.timetable;`
- Check user role: `SELECT * FROM users WHERE role='CLASS_TEACHER';`
- Check Tomcat logs for Java errors

**Multiple databases issue**
- If data in other database, backup first
- Use migration script to move data
- Test with small amount of data first
- Verify data integrity after migration

## Files Modified

1. **UploadTimetableServlet.java**
   - Now returns JSON responses
   - Added proper HTTP status codes
   - Validates file and user role

2. **class-teacher-dashboard.html**
   - JavaScript now parses JSON responses
   - Improved error handling
   - Shows detailed error messages

3. **fix_timetable_upload.sql** (NEW)
   - Creates timetable table if missing
   - Adds required indexes
   - Creates CLASS_TEACHER test user

## Next Steps

1. Identify the other database name
2. Run fix_timetable_upload.sql on lab_management_new
3. If data exists in other database, run migration
4. Test the upload feature
5. Verify data appears in View Timetable section

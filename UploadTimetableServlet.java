import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

@WebServlet("/UploadTimetableServlet")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024)
public class UploadTimetableServlet extends HttpServlet {

    private final DataFormatter dataFormatter = new DataFormatter(Locale.US);

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        // ROLE CHECK - CLASS_TEACHER only
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print(createJsonResponse("error", "Session expired. Please login again."));
            out.flush();
            return;
        }
        String role = session.getAttribute("role").toString();
        if (!role.equalsIgnoreCase("CLASS_TEACHER")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.print(createJsonResponse("error", "Unauthorized: Only Class Teacher can upload timetable."));
            out.flush();
            return;
        }

        Part filePart = request.getPart("timetableFile");
        if (filePart == null || filePart.getSize() == 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(createJsonResponse("error", "No file uploaded."));
            out.flush();
            return;
        }

        String fileName = filePart.getSubmittedFileName();
        String lowerFileName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (fileName == null || (!lowerFileName.endsWith(".xlsx") && !lowerFileName.endsWith(".xls"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(createJsonResponse("error", "Only Excel files (.xlsx or .xls) are allowed."));
            out.flush();
            return;
        }

        try (InputStream is = filePart.getInputStream();
             Workbook workbook = lowerFileName.endsWith(".xlsx")
                    ? new XSSFWorkbook(is)
                    : new HSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Connection con = DBConnection.getConnection();
            
            if (con == null) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print(createJsonResponse("error", "Database connection failed."));
                out.flush();
                return;
            }

            ensureSchema(con);

            String insertSQL =
                "INSERT INTO timetable (entry_date, day_name, slot, batch, course_name, faculty_code, lab_name) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
            boolean entryDateIsDateColumn = isDateLikeColumn(con, "timetable", "entry_date");
            List<Object[]> validRows = new ArrayList<>();
            String currentDate = "";

            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                if (looksLikeHeaderRow(row)) {
                    continue;
                }

                String rowDate = normalizeCellString(getCellString(row.getCell(0)));
                if (!rowDate.isEmpty()) {
                    rowDate = normalizeDate(rowDate, row.getCell(0));
                    if (!rowDate.isEmpty()) {
                        currentDate = rowDate;
                    }
                }

                if (currentDate.isEmpty()) {
                    continue;
                }

                String dayName = getDayName(currentDate);

                addSlotGroup(validRows, row, 1, currentDate, dayName, 2, 3, 4, 5);
                addSlotGroup(validRows, row, 2, currentDate, dayName, 7, 8, 9, 10);
                addSlotGroup(validRows, row, 3, currentDate, dayName, 12, 13, 14, 15);
            }

            if (validRows.isEmpty()) {
                con.close();
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(createJsonResponse("error", "No valid timetable rows were found in the uploaded file."));
                out.flush();
                return;
            }

            con.prepareStatement("DELETE FROM timetable").executeUpdate();
            PreparedStatement ps = con.prepareStatement(insertSQL);
            int count = 0;
            for (Object[] rowData : validRows) {
                if (entryDateIsDateColumn) {
                    ps.setDate(1, toSqlDate((String) rowData[0]));
                } else {
                    ps.setString(1, (String) rowData[0]);
                }
                ps.setString(2, (String) rowData[1]);
                ps.setInt(3, (int) rowData[2]);
                ps.setString(4, (String) rowData[3]);
                ps.setString(5, (String) rowData[4]);
                ps.setString(6, (String) rowData[5]);
                ps.setString(7, (String) rowData[6]);
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
            ps.close();
            recordUpload(con, fileName, session, count);
            con.close();

            response.setStatus(HttpServletResponse.SC_OK);
            out.print(createJsonResponse("success", "Timetable uploaded successfully! " + count + " entries added."));
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(createJsonResponse("error", "Error processing file: " + e.getMessage()));
            out.flush();
        }
    }

    private void ensureSchema(Connection con) throws SQLException {
        try (Statement schemaStmt = con.createStatement()) {
            schemaStmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS timetable (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "entry_date VARCHAR(20) NOT NULL, " +
                "day_name VARCHAR(20) NOT NULL, " +
                "slot INT NOT NULL, " +
                "batch VARCHAR(20), " +
                "course_name VARCHAR(100), " +
                "faculty_code VARCHAR(100), " +
                "lab_name VARCHAR(100)" +
                ")");

            schemaStmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS uploaded_files (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "filename VARCHAR(255), " +
                "uploader VARCHAR(100), " +
                "uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "row_count INT, " +
                "status VARCHAR(50), " +
                "message VARCHAR(255)" +
                ")");

            addColumnIfMissing(con, "uploaded_files", "uploader", "VARCHAR(100)");
            addColumnIfMissing(con, "uploaded_files", "row_count", "INT");
            addColumnIfMissing(con, "uploaded_files", "status", "VARCHAR(50)");
            addColumnIfMissing(con, "uploaded_files", "message", "VARCHAR(255)");
        }
    }

    private void addColumnIfMissing(Connection con, String tableName, String columnName, String columnType) throws SQLException {
        DatabaseMetaData meta = con.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            if (rs.next()) {
                return;
            }
        }
        try (Statement stmt = con.createStatement()) {
            stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
        }
    }

    private boolean isDateLikeColumn(Connection con, String tableName, String columnName) throws SQLException {
        DatabaseMetaData meta = con.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            if (rs.next()) {
                int type = rs.getInt("DATA_TYPE");
                return type == Types.DATE || type == Types.TIMESTAMP || type == Types.TIMESTAMP_WITH_TIMEZONE;
            }
        }
        return false;
    }

    private void recordUpload(Connection con, String fileName, HttpSession session, int count) {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO uploaded_files (filename, uploader, row_count, status, message) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, fileName);
            ps.setString(2, session.getAttribute("user") != null
                    ? session.getAttribute("user").toString()
                    : "CLASS_TEACHER");
            ps.setInt(3, count);
            ps.setString(4, "SUCCESS");
            ps.setString(5, "Timetable upload completed.");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        String formatted = dataFormatter.formatCellValue(cell).trim();
        if (!formatted.isEmpty()) {
            return formatted;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date d = cell.getDateCellValue();
                    return new SimpleDateFormat("dd/MM/yyyy").format(d);
                }
                double numericValue = cell.getNumericCellValue();
                if (numericValue == Math.floor(numericValue)) {
                    return String.valueOf((long) numericValue);
                }
                return String.valueOf(numericValue);
            case FORMULA:
                switch (cell.getCachedFormulaResultType()) {
                    case STRING:
                        return cell.getStringCellValue().trim();
                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            Date d = cell.getDateCellValue();
                            return new SimpleDateFormat("dd/MM/yyyy").format(d);
                        }
                        double formulaNumericValue = cell.getNumericCellValue();
                        if (formulaNumericValue == Math.floor(formulaNumericValue)) {
                            return String.valueOf((long) formulaNumericValue);
                        }
                        return String.valueOf(formulaNumericValue);
                    case BOOLEAN:
                        return String.valueOf(cell.getBooleanCellValue());
                    default:
                        return "";
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private String normalizeDate(String raw, Cell cell) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return new SimpleDateFormat("dd/MM/yyyy").format(cell.getDateCellValue());
        }
        if (raw == null) return "";
        raw = raw.trim();
        try {
            String[] formats = {"d/M/yyyy", "d/MM/yyyy", "dd/M/yyyy", "dd/MM/yyyy",
                    "d/MMyyyy", "dd/MMyyyy", "dMMyyyy", "ddMMyyyy",
                    "d/M/yy", "d-MM-yyyy", "dd-MM-yyyy", "yyyy-MM-dd"};
            for (String format : formats) {
                try {
                    SimpleDateFormat parser = new SimpleDateFormat(format);
                    parser.setLenient(false);
                    Date d = parser.parse(raw);
                    return new SimpleDateFormat("dd/MM/yyyy").format(d);
                } catch (Exception ignored) {
                    // Try the next known timetable date format.
                }
            }
        } catch (Exception e) {
            // fall through to return raw
        }
        return raw;
    }

    private String normalizeCellString(String raw) {
        if (raw == null) return "";
        return raw.replace('\u00A0', ' ').trim();
    }

    private boolean looksLikeHeaderRow(Row row) {
        int labelMatches = 0;
        for (int c = 0; c <= 15; c++) {
            String cellText = normalizeCellString(getCellString(row.getCell(c))).toLowerCase(Locale.ROOT);
            if (cellText.contains("date") || cellText.equals("day") || cellText.contains("slot")) {
                return true;
            }
            if (cellText.equals("batch") || cellText.equals("subject") || cellText.equals("course") ||
                cellText.equals("course name") || cellText.equals("faculty") ||
                cellText.equals("faculty code") || cellText.equals("lab") || cellText.equals("lab name")) {
                labelMatches++;
            }
        }
        return labelMatches >= 2;
    }

    private void addSlotGroup(List<Object[]> validRows, Row row, int slot, String date, String day,
                              int batchCol, int courseCol, int facultyCol, int labCol) {
        String batch = normalizeCellString(getCellString(row.getCell(batchCol)));
        String courseName = normalizeCellString(getCellString(row.getCell(courseCol)));
        String facultyCode = normalizeCellString(getCellString(row.getCell(facultyCol)));
        String labName = normalizeCellString(getCellString(row.getCell(labCol)));

        if (batch.isEmpty() && courseName.isEmpty() && facultyCode.isEmpty() && labName.isEmpty()) {
            return;
        }
        if (batch.isEmpty() || courseName.isEmpty()) {
            return;
        }

        validRows.add(new Object[] { date, day, slot, batch, courseName, facultyCode, labName });
    }

    private String getDayName(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return "";
        }
        try {
            Date d = new SimpleDateFormat("dd/MM/yyyy").parse(dateStr);
            return new SimpleDateFormat("EEEE").format(d);
        } catch (Exception e) {
            return "";
        }
    }

    private java.sql.Date toSqlDate(String dateStr) throws SQLException {
        try {
            SimpleDateFormat parser = new SimpleDateFormat("dd/MM/yyyy");
            parser.setLenient(false);
            Date parsed = parser.parse(dateStr);
            return new java.sql.Date(parsed.getTime());
        } catch (Exception e) {
            throw new SQLException("Invalid timetable date: " + dateStr, e);
        }
    }

    private String createJsonResponse(String status, String message) {
        // Escape special characters in message - order matters!
        // Must escape backslash FIRST before other escapes
        String escapedMessage = message.replace("\\", "\\\\")    // Escape backslash first
                                      .replace("\"", "\\\"")     // Escape quotes
                                      .replace("\n", "\\n")      // Escape newline
                                      .replace("\r", "\\r")      // Escape carriage return
                                      .replace("\t", "\\t");     // Escape tab
        return "{\"status\":\"" + status + "\",\"message\":\"" + escapedMessage + "\"}";
    }
}

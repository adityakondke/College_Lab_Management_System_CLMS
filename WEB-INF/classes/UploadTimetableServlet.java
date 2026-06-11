import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

@WebServlet("/UploadTimetableServlet")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024)
public class UploadTimetableServlet extends HttpServlet {

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
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(createJsonResponse("error", "Only Excel files (.xlsx or .xls) are allowed."));
            out.flush();
            return;
        }

        InputStream is = filePart.getInputStream();

        try {
            Workbook workbook = fileName.endsWith(".xlsx")
                    ? new XSSFWorkbook(is)
                    : new HSSFWorkbook(is);

            Sheet sheet = workbook.getSheetAt(0);
            Connection con = DBConnection.getConnection();
            
            if (con == null) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print(createJsonResponse("error", "Database connection failed."));
                out.flush();
                return;
            }

            try (Statement schemaStmt = con.createStatement()) {
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
            }

            String insertSQL =
                "INSERT INTO timetable (entry_date, day_name, slot, batch, course_name, faculty_code, lab_name) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
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
                workbook.close();
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
                ps.setString(1, (String) rowData[0]);
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
            
            try (PreparedStatement insertUploadInfo = con.prepareStatement(
                    "INSERT INTO uploaded_files (filename, uploader, row_count, status, message) VALUES (?, ?, ?, ?, ?)")) {
                insertUploadInfo.setString(1, fileName);
                insertUploadInfo.setString(2, session.getAttribute("user") != null
                        ? session.getAttribute("user").toString()
                        : role);
                insertUploadInfo.setInt(3, count);
                insertUploadInfo.setString(4, "SUCCESS");
                insertUploadInfo.setString(5, "Timetable upload completed.");
                insertUploadInfo.executeUpdate();
            }

            workbook.close();
            ps.close();
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

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date d = cell.getDateCellValue();
                    return new SimpleDateFormat("dd/MM/yyyy").format(d);
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case FORMULA:
                switch (cell.getCachedFormulaResultType()) {
                    case STRING:
                        return cell.getStringCellValue().trim();
                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            Date d = cell.getDateCellValue();
                            return new SimpleDateFormat("dd/MM/yyyy").format(d);
                        }
                        return String.valueOf((long) cell.getNumericCellValue());
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
            if (raw.matches("\\d{1,2}/\\d{1,2}/\\d{2,4}")) {
                Date d = new SimpleDateFormat("d/M/yyyy").parse(raw);
                return new SimpleDateFormat("dd/MM/yyyy").format(d);
            }
            if (raw.matches("\\d{1,2}-\\d{1,2}-\\d{2,4}")) {
                Date d = new SimpleDateFormat("d-M-yyyy").parse(raw);
                return new SimpleDateFormat("dd/MM/yyyy").format(d);
            }
        } catch (Exception e) {
            // fall through
        }
        return raw;
    }

    private String normalizeCellString(String raw) {
        if (raw == null) return "";
        return raw.replace('\u00A0', ' ').trim();
    }

    private boolean looksLikeHeaderRow(Row row) {
        for (int c = 0; c <= 15; c++) {
            String cellText = getCellString(row.getCell(c)).toLowerCase();
            if (cellText.contains("date") || cellText.contains("day") || cellText.contains("slot") ||
                cellText.contains("batch") || cellText.contains("subject") || cellText.contains("course") ||
                cellText.contains("faculty") || cellText.contains("lab")) {
                return true;
            }
        }
        return false;
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

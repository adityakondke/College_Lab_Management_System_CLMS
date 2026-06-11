import java.io.IOException;
import java.sql.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/BookLabServlet")
public class BookLabServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        String lab = request.getParameter("lab");
        String date = request.getParameter("date");
        String slot = request.getParameter("slot");

        if (lab == null || lab.trim().isEmpty() ||
            date == null || date.trim().isEmpty() ||
            slot == null || slot.trim().isEmpty()) {

            response.getWriter().println(
                "<div style='color:red;font-weight:bold;'>All fields are required</div>"
            );
            return;
        }

        lab = lab.trim();
        date = date.trim();
        slot = slot.trim();

        int timetableSlot = normalizeSlot(slot);
        if (timetableSlot == 0) {
            response.getWriter().println(
                "<div style='color:red;font-weight:bold;'>Invalid slot</div>"
            );
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.getWriter().println("Session expired. Please log in again.");
            return;
        }

        String faculty = session.getAttribute("user").toString().trim().toLowerCase();

        String normalizedLab = normalizeLab(lab);

        try (Connection con = DBConnection.getConnection()) {
            boolean entryDateIsDateColumn = isDateLikeColumn(con, "timetable", "entry_date");

            int facultyCount = countFacultyBookings(con, faculty, date, slot);
            if (facultyCount > 0) {
                response.getWriter().println(
                    "<div style='color:red;font-weight:bold;'>You already have a booking in this slot</div>"
                );
                return;
            }

            int bookingCount = countBookings(con, normalizedLab, date, slot);
            int timetableCount = countTimetableEntries(con, lab, date, timetableSlot, entryDateIsDateColumn);
            int totalUsage = bookingCount + timetableCount;

            if (totalUsage > 0) {
                response.getWriter().println(
                    "<div style='color:red;font-weight:bold;'>This lab is already occupied in this slot</div>"
                );
                return;
            }

            String insertSQL =
                "INSERT INTO lab_booking (faculty_email, lab, booking_date, slot) VALUES (?, ?, ?, ?)";

            try (PreparedStatement insertPs = con.prepareStatement(insertSQL)) {
                insertPs.setString(1, faculty);
                insertPs.setString(2, lab);
                insertPs.setString(3, date);
                insertPs.setString(4, slot);

                int rows = insertPs.executeUpdate();
                if (rows > 0) {
                    response.getWriter().println(
                        "<div style='background:#d4edda;padding:15px;border-left:5px solid green;border-radius:6px;margin-top:15px;'>" +
                        "<h3 style='color:green;'>Lab Booked Successfully</h3>" +
                        "<p><b>Faculty:</b> " + faculty + "</p>" +
                        "<p><b>Lab:</b> " + lab + "</p>" +
                        "<p><b>Date:</b> " + date + "</p>" +
                        "<p><b>Slot:</b> " + slot + "</p>" +
                        "</div>"
                    );
                } else {
                    response.getWriter().println(
                        "<div style='color:red;'>Booking failed. Please try again.</div>"
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println(
                "<div style='color:red;'>Server error: " + e.getMessage() + "</div>"
            );
        }
    }

    private int countFacultyBookings(Connection con, String faculty, String date, String slot) throws SQLException {
        String query =
            "SELECT COUNT(*) FROM lab_booking WHERE faculty_email = ? AND booking_date = ? AND slot = ?";

        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, faculty);
            ps.setString(2, date);
            ps.setString(3, slot);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int countBookings(Connection con, String normalizedLab, String date, String slot) throws SQLException {
        String query =
            "SELECT COUNT(*) FROM lab_booking WHERE " + normalizedLabSql("lab") + " = ? AND booking_date = ? AND slot = ?";

        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, normalizedLab);
            ps.setString(2, date);
            ps.setString(3, slot);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int countTimetableEntries(Connection con, String lab, String date, int slot,
                                      boolean entryDateIsDateColumn) throws SQLException {
        String query = entryDateIsDateColumn
            ? "SELECT COUNT(*) FROM timetable WHERE " + normalizedLabSql("lab_name") + " = ? AND entry_date = ? AND slot = ?"
            : "SELECT COUNT(*) FROM timetable WHERE " + normalizedLabSql("lab_name") + " = ? AND DATE_FORMAT(STR_TO_DATE(entry_date,'%d/%m/%Y'),'%Y-%m-%d') = ? AND slot = ?";

        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, normalizeLab(lab));
            if (entryDateIsDateColumn) {
                ps.setDate(2, java.sql.Date.valueOf(date));
            } else {
                ps.setString(2, date);
            }
            ps.setInt(3, slot);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int normalizeSlot(String slot) {
        String cleanSlot = slot == null ? "" : slot.trim().toUpperCase();
        if (cleanSlot.equals("SLOT1") || cleanSlot.equals("1")) return 1;
        if (cleanSlot.equals("SLOT2") || cleanSlot.equals("2")) return 2;
        if (cleanSlot.equals("SLOT3") || cleanSlot.equals("3")) return 3;
        return 0;
    }

    private String normalizeLab(String lab) {
        if (lab == null) return "";
        return lab.replaceAll("[\\s\\-_]", "").toUpperCase();
    }

    private String normalizedLabSql(String columnName) {
        return "UPPER(REPLACE(REPLACE(REPLACE(" + columnName + ", ' ', ''), '-', ''), '_', ''))";
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
}

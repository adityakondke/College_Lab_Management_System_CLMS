import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/CheckAvailabilityServlet")
public class CheckAvailabilityServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        String date = request.getParameter("date");
        String slot = request.getParameter("slot");

        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        PrintWriter out = response.getWriter();

        if (date == null || date.trim().isEmpty() || slot == null || slot.trim().isEmpty()) {
            out.print("[{\"error\":\"Date and slot are required\"}]");
            return;
        }

        int timetableSlot = normalizeSlot(slot);
        if (timetableSlot == 0) {
            out.print("[{\"error\":\"Invalid slot\"}]");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            boolean entryDateIsDateColumn = isDateLikeColumn(con, "timetable", "entry_date");
            ArrayList<String> labs = new ArrayList<>();

            try (PreparedStatement labPs = con.prepareStatement("SELECT lab_name FROM labs ORDER BY lab_name");
                 ResultSet labRs = labPs.executeQuery()) {
                while (labRs.next()) {
                    labs.add(labRs.getString("lab_name"));
                }
            }

            out.print("[");

            for (int i = 0; i < labs.size(); i++) {
                String lab = labs.get(i);

                String normalizedLab = normalizeLab(lab);
                int bookingCount = countBookings(con, normalizedLab, date, slot);
                int timetableCount = countTimetableEntries(con, lab, date, timetableSlot, entryDateIsDateColumn);

                boolean available = bookingCount + timetableCount == 0;
                String reason = "";
                if (bookingCount > 0 && timetableCount > 0) {
                    reason = "Booked + Timetable";
                } else if (bookingCount > 0) {
                    reason = "Booked";
                } else if (timetableCount > 0) {
                    reason = "Timetable";
                }

                out.print("{\"name\":\"" + esc(lab) + "\",\"available\":" + available +
                    ",\"reason\":\"" + esc(reason) + "\"}");
                if (i < labs.size() - 1) {
                    out.print(",");
                }
            }

            out.print("]");
        } catch (Exception e) {
            e.printStackTrace();
            out.print("[{\"error\":\"" + esc(e.getMessage()) + "\"}]");
        }
    }

    private int countBookings(Connection con, String normalizedLab, String date, String slot) throws SQLException {
        String query =
            "SELECT COUNT(*) FROM lab_booking WHERE " + normalizedLabSql("lab") + "=? AND booking_date=? AND slot=?";

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
            ? "SELECT COUNT(*) FROM timetable WHERE " + normalizedLabSql("lab_name") + "=? AND entry_date=? AND slot=?"
            : "SELECT COUNT(*) FROM timetable WHERE " + normalizedLabSql("lab_name") + "=? AND DATE_FORMAT(STR_TO_DATE(entry_date,'%d/%m/%Y'),'%Y-%m-%d')=? AND slot=?";

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

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

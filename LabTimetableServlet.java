import java.io.*;
import java.sql.*;
import java.util.*;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/LabTimetableServlet")
public class LabTimetableServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        String selectedLab = request.getParameter("lab");
        if (selectedLab == null || selectedLab.trim().isEmpty()) {
            out.println("<p style='color:red;'>No lab selected.</p>");
            return;
        }

        String[] slotTimings = {
            "10:15 AM - 12:15 PM",
            "1:15 PM - 3:15 PM",
            "3:30 PM - 5:30 PM"
        };

        try {
            Connection con = DBConnection.getConnection();
            boolean entryDateIsDateColumn = isDateLikeColumn(con, "timetable", "entry_date");
            String dateSelect = entryDateIsDateColumn
                ? "DATE_FORMAT(entry_date,'%d/%m/%Y') AS entry_date, entry_date AS sort_date"
                : "entry_date, STR_TO_DATE(entry_date,'%d/%m/%Y') AS sort_date";
            String dateOrder = "sort_date";

            // Get all distinct dates for this lab
            PreparedStatement datePs = con.prepareStatement(
                "SELECT DISTINCT " + dateSelect + ", day_name FROM timetable WHERE lab_name=? " +
                "ORDER BY " + dateOrder
            );
            datePs.setString(1, selectedLab);
            ResultSet dateRs = datePs.executeQuery();

            // Build date list
            List<String[]> dates = new ArrayList<>();
            while (dateRs.next()) {
                dates.add(new String[]{
                    dateRs.getString("entry_date"),
                    dateRs.getString("day_name")
                });
            }

            if (dates.isEmpty()) {
                // Show all dates from timetable, mark this lab as FREE
                PreparedStatement allDatePs = con.prepareStatement(
                    "SELECT DISTINCT " + dateSelect + ", day_name FROM timetable " +
                    "ORDER BY " + dateOrder
                );
                ResultSet allDateRs = allDatePs.executeQuery();
                while (allDateRs.next()) {
                    dates.add(new String[]{
                        allDateRs.getString("entry_date"),
                        allDateRs.getString("day_name")
                    });
                }
            }

            if (dates.isEmpty()) {
                out.println("<p style='color:#888;'>No timetable data found. Upload a timetable first.</p>");
                return;
            }

            // Fetch all entries for this lab
            PreparedStatement ps = con.prepareStatement(
                "SELECT " + dateSelect + ", slot, batch, course_name, faculty_code " +
                "FROM timetable WHERE lab_name=?"
            );
            ps.setString(1, selectedLab);
            ResultSet rs = ps.executeQuery();

            // Map: date -> slot -> info
            Map<String, Map<Integer, String>> schedule = new HashMap<>();
            while (rs.next()) {
                String d = rs.getString("entry_date");
                int s    = rs.getInt("slot");
                String info = "<b>" + nvl(rs.getString("course_name")) + "</b><br>" +
                              "Faculty: " + nvl(rs.getString("faculty_code")) + "<br>" +
                              "Batch: " + nvl(rs.getString("batch"));
                schedule.computeIfAbsent(d, k -> new HashMap<>()).put(s, info);
            }

            // Render table
            out.println("<div style='overflow-x:auto;'>");
            out.println("<table style='width:100%;border-collapse:collapse;text-align:center;'>");
            out.println("<tr>");
            out.println("<th style='background:#0b3d6d;color:white;padding:12px;border:1px solid #111;'>Date</th>");
            out.println("<th style='background:#0b3d6d;color:white;padding:12px;border:1px solid #111;'>Day</th>");
            for (String t : slotTimings) {
                out.println("<th style='background:#0b3d6d;color:white;padding:12px;border:1px solid #111;'>" + t + "</th>");
            }
            out.println("</tr>");

            for (String[] dateInfo : dates) {
                String dateStr = dateInfo[0];
                String dayName = dateInfo[1];
                Map<Integer, String> slotMap = schedule.getOrDefault(dateStr, new HashMap<>());

                out.println("<tr>");
                out.println("<td style='padding:10px;border:1px solid #ccc;font-weight:bold;'>" + dateStr + "</td>");
                out.println("<td style='padding:10px;border:1px solid #ccc;'>" + dayName + "</td>");

                for (int s = 1; s <= 3; s++) {
                    if (slotMap.containsKey(s)) {
                        out.println("<td style='padding:10px;border:1px solid #ccc;background:#ffebee;font-size:13px;'>" +
                            "<span style='color:#b71c1c;font-weight:bold;'>BUSY</span><br>" +
                            slotMap.get(s) + "</td>");
                    } else {
                        out.println("<td style='padding:10px;border:1px solid #ccc;background:#e8f5e9;'>" +
                            "<span style='color:green;font-weight:bold;'>FREE</span></td>");
                    }
                }
                out.println("</tr>");
            }
            out.println("</table></div>");

        } catch (Exception e) {
            e.printStackTrace();
            out.println("<p style='color:red;'>Error loading lab timetable: " + e.getMessage() + "</p>");
        }
    }

    private String nvl(String s) { return s == null ? "" : s; }

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

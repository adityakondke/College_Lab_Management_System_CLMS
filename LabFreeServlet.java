import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/LabFreeServlet")
public class LabFreeServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String lab = request.getParameter("lab");
        if (lab == null || lab.trim().isEmpty()) {
            out.println("<div style='color:red;font-weight:bold;'>Select lab first</div>");
            return;
        }

        String[] timings = {
            "10:15 AM - 12:15 PM",
            "1:15 PM - 3:15 PM",
            "3:30 PM - 5:30 PM"
        };

        try (Connection con = DBConnection.getConnection()) {
            boolean entryDateIsDateColumn = isDateLikeColumn(con, "timetable", "entry_date");
            String dateSelect = entryDateIsDateColumn
                ? "DATE_FORMAT(entry_date,'%d/%m/%Y') AS entry_date, entry_date AS sort_date"
                : "entry_date, STR_TO_DATE(entry_date,'%d/%m/%Y') AS sort_date";

            String query =
                "SELECT d.entry_date, d.day_name, s.slot " +
                "FROM (SELECT DISTINCT " + dateSelect + ", day_name FROM timetable) d " +
                "CROSS JOIN (SELECT 1 AS slot UNION ALL SELECT 2 UNION ALL SELECT 3) s " +
                "LEFT JOIN timetable t ON " +
                (entryDateIsDateColumn
                    ? "DATE_FORMAT(t.entry_date,'%d/%m/%Y') = d.entry_date "
                    : "t.entry_date = d.entry_date ") +
                "AND t.slot = s.slot AND t.lab_name = ? " +
                "WHERE t.id IS NULL " +
                "ORDER BY d.sort_date, s.slot";

            try (PreparedStatement ps = con.prepareStatement(query)) {
                ps.setString(1, lab);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean found = false;
                    out.println("<h3 style='color:green;'>Free Slots</h3>");

                    while (rs.next()) {
                        found = true;
                        int slot = rs.getInt("slot");
                        out.println(
                            "<div style='padding:10px;background:#d4edda;margin-bottom:8px;border-radius:6px;'>" +
                            "<b>" + rs.getString("entry_date") + " (" + rs.getString("day_name") + ")</b> : " +
                            timings[slot - 1] +
                            "</div>"
                        );
                    }

                    if (!found) {
                        out.println("<div style='padding:12px;background:#f8d7da;border-left:5px solid red;border-radius:6px;font-weight:bold;'>No free slots found for this lab.</div>");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.println("<div style='color:red;font-weight:bold;'>Error loading free slots: " + e.getMessage() + "</div>");
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
}

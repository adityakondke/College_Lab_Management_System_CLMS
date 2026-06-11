import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/LabBusyServlet")
public class LabBusyServlet extends HttpServlet {

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
            String dateOrder = "sort_date";

            String query =
                "SELECT " + dateSelect + ", day_name, slot, batch, course_name, faculty_code " +
                "FROM timetable WHERE lab_name=? " +
                "ORDER BY " + dateOrder + ", slot";

            try (PreparedStatement ps = con.prepareStatement(query)) {
                ps.setString(1, lab);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean found = false;
                    out.println("<h3 style='color:red;'>Busy Slots</h3>");

                    while (rs.next()) {
                        found = true;
                        int slot = rs.getInt("slot");
                        out.println(
                            "<div style='padding:12px;background:#f8d7da;margin-bottom:10px;border-left:5px solid red;border-radius:6px;'>" +
                            "<b>Date:</b> " + rs.getString("entry_date") +
                            "<br><br><b>Day:</b> " + rs.getString("day_name") +
                            "<br><br><b>Time:</b> " + timings[slot - 1] +
                            "<br><br><b>Subject:</b> " + nvl(rs.getString("course_name")) +
                            "<br><br><b>Faculty:</b> " + nvl(rs.getString("faculty_code")) +
                            "<br><br><b>Batch:</b> " + nvl(rs.getString("batch")) +
                            "</div>"
                        );
                    }

                    if (!found) {
                        out.println("<div style='padding:12px;background:#d4edda;border-left:5px solid green;border-radius:6px;font-weight:bold;'>This lab is completely free</div>");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.println("<div style='color:red;font-weight:bold;'>Error loading busy slots: " + e.getMessage() + "</div>");
        }
    }

    private String nvl(String s) {
        return s == null ? "" : s;
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

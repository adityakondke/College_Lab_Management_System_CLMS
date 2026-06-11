import java.io.*;
import java.sql.*;
import java.util.*;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/ViewTimetableServlet")
public class ViewTimetableServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            out.println("{\"error\":\"Session expired\"}");
            return;
        }

        try {
            Connection con = DBConnection.getConnection();
            boolean entryDateIsDateColumn = isDateLikeColumn(con, "timetable", "entry_date");

            // Fetch all entries ordered by date then slot
            String query = entryDateIsDateColumn
                ? "SELECT id, DATE_FORMAT(entry_date,'%d/%m/%Y') AS entry_date, day_name, slot, batch, course_name, faculty_code, lab_name " +
                  "FROM timetable ORDER BY entry_date, slot"
                : "SELECT id, entry_date, day_name, slot, batch, course_name, faculty_code, lab_name " +
                  "FROM timetable ORDER BY STR_TO_DATE(entry_date,'%d/%m/%Y'), slot";

            PreparedStatement ps = con.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            // Build: LinkedHashMap<dateKey, Map<slot, List<entry>>>
            // dateKey = "dd/MM/yyyy|DayName"
            LinkedHashMap<String, Map<Integer, List<Map<String,String>>>> data = new LinkedHashMap<>();

            while (rs.next()) {
                String dateKey = rs.getString("entry_date") + "|" + rs.getString("day_name");
                int slot = rs.getInt("slot");

                data.computeIfAbsent(dateKey, k -> new LinkedHashMap<>())
                    .computeIfAbsent(slot, k -> new ArrayList<>())
                    .add(Map.of(
                        "id",          String.valueOf(rs.getInt("id")),
                        "batch",       nvl(rs.getString("batch")),
                        "course_name", nvl(rs.getString("course_name")),
                        "faculty_code",nvl(rs.getString("faculty_code")),
                        "lab_name",    nvl(rs.getString("lab_name"))
                    ));
            }

            // Serialize to JSON manually (no external JSON lib needed)
            StringBuilder sb = new StringBuilder();
            sb.append("{\"data\":{");
            boolean firstDate = true;
            for (Map.Entry<String, Map<Integer, List<Map<String,String>>>> dateEntry : data.entrySet()) {
                if (!firstDate) sb.append(",");
                firstDate = false;
                sb.append("\"").append(esc(dateEntry.getKey())).append("\":{");
                boolean firstSlot = true;
                for (Map.Entry<Integer, List<Map<String,String>>> slotEntry : dateEntry.getValue().entrySet()) {
                    if (!firstSlot) sb.append(",");
                    firstSlot = false;
                    sb.append("\"").append(slotEntry.getKey()).append("\":[");
                    boolean firstEntry = true;
                    for (Map<String,String> e : slotEntry.getValue()) {
                        if (!firstEntry) sb.append(",");
                        firstEntry = false;
                        sb.append("{")
                          .append("\"id\":\"").append(esc(e.get("id"))).append("\",")
                          .append("\"batch\":\"").append(esc(e.get("batch"))).append("\",")
                          .append("\"course_name\":\"").append(esc(e.get("course_name"))).append("\",")
                          .append("\"faculty_code\":\"").append(esc(e.get("faculty_code"))).append("\",")
                          .append("\"lab_name\":\"").append(esc(e.get("lab_name"))).append("\"")
                          .append("}");
                    }
                    sb.append("]");
                }
                sb.append("}");
            }
            sb.append("}}");
            out.println(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            out.println("{\"error\":\"" + e.getMessage() + "\"}");
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

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"");
    }
}

import java.io.*;
import java.sql.*;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/EditTimetableEntryServlet")
public class EditTimetableEntryServlet extends HttpServlet {

    // GET - load entry data as JSON
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null ||
            !session.getAttribute("role").toString().equalsIgnoreCase("CLASS_TEACHER")) {
            out.println("{\"error\":\"UNAUTHORIZED\"}");
            return;
        }

        String id = request.getParameter("id");
        try {
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM timetable WHERE id=?"
            );
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                out.println("{" +
                    "\"id\":" + rs.getInt("id") + "," +
                    "\"entry_date\":\"" + esc(rs.getString("entry_date")) + "\"," +
                    "\"day_name\":\"" + esc(rs.getString("day_name")) + "\"," +
                    "\"slot\":" + rs.getInt("slot") + "," +
                    "\"batch\":\"" + esc(rs.getString("batch")) + "\"," +
                    "\"course_name\":\"" + esc(rs.getString("course_name")) + "\"," +
                    "\"faculty_code\":\"" + esc(rs.getString("faculty_code")) + "\"," +
                    "\"lab_name\":\"" + esc(rs.getString("lab_name")) + "\"" +
                "}");
            } else {
                out.println("{\"error\":\"Not found\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.println("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    // POST - save edited entry (only batch, course_name, faculty_code, lab_name can be edited)
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null ||
            !session.getAttribute("role").toString().equalsIgnoreCase("CLASS_TEACHER")) {
            out.println("UNAUTHORIZED");
            return;
        }

        String id          = request.getParameter("id");
        String batch       = request.getParameter("batch");
        String courseName  = request.getParameter("course_name");
        String facultyCode = request.getParameter("faculty_code");
        String labName     = request.getParameter("lab_name");

        try {
            Connection con = DBConnection.getConnection();
            // Only update the editable fields; date, day, slot cannot be changed
            PreparedStatement ps = con.prepareStatement(
                "UPDATE timetable SET batch=?, course_name=?, faculty_code=?, lab_name=? WHERE id=?"
            );
            ps.setString(1, batch);
            ps.setString(2, courseName);
            ps.setString(3, facultyCode);
            ps.setString(4, labName);
            ps.setString(5, id);
            ps.executeUpdate();
            out.println("OK");
        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR: " + e.getMessage());
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"");
    }
}

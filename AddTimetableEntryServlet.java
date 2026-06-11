import java.io.*;
import java.sql.*;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/AddTimetableEntryServlet")
public class AddTimetableEntryServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null) {
            out.println("UNAUTHORIZED");
            return;
        }
        if (!session.getAttribute("role").toString().equalsIgnoreCase("CLASS_TEACHER")) {
            out.println("UNAUTHORIZED");
            return;
        }

        String entryDate   = request.getParameter("entry_date");
        String dayName     = request.getParameter("day_name");
        String slotStr     = request.getParameter("slot");
        String batch       = request.getParameter("batch");
        String courseName  = request.getParameter("course_name");
        String facultyCode = request.getParameter("faculty_code");
        String labName     = request.getParameter("lab_name");

        if (entryDate == null || dayName == null || slotStr == null) {
            out.println("Missing required fields");
            return;
        }

        try {
            int slot = Integer.parseInt(slotStr);
            Connection con = DBConnection.getConnection();

            String sql =
                "INSERT INTO timetable (entry_date, day_name, slot, batch, course_name, faculty_code, lab_name) " +
                "VALUES (?,?,?,?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, entryDate);
            ps.setString(2, dayName);
            ps.setInt(3, slot);
            ps.setString(4, batch);
            ps.setString(5, courseName);
            ps.setString(6, facultyCode);
            ps.setString(7, labName);
            ps.executeUpdate();

            out.println("OK");

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR: " + e.getMessage());
        }
    }
}

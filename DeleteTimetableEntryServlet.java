import java.io.*;
import java.sql.*;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/DeleteTimetableEntryServlet")
public class DeleteTimetableEntryServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {
        doDeleteEntry(request, response);
    }

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws IOException {
        doDeleteEntry(request, response);
    }

    private void doDeleteEntry(HttpServletRequest request,
                               HttpServletResponse response)
            throws IOException {

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null ||
            !session.getAttribute("role").toString().equalsIgnoreCase("CLASS_TEACHER")) {
            out.println("UNAUTHORIZED");
            return;
        }

        String id = request.getParameter("id");
        try {
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM timetable WHERE id=?");
            ps.setString(1, id);
            ps.executeUpdate();
            out.println("Deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            out.println("Error: " + e.getMessage());
        }
    }
}

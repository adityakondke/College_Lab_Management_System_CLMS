import java.io.*;
import java.sql.*;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/FacultyConfirmIssueServlet")
public class FacultyConfirmIssueServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws IOException {

        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            out.println("UNAUTHORIZED");
            return;
        }

        String id        = request.getParameter("id");
        String confirmed = request.getParameter("confirmed"); // "YES" or "NO"

        if (id == null || confirmed == null) {
            out.println("Missing parameters");
            return;
        }

        if (!confirmed.equalsIgnoreCase("YES") && !confirmed.equalsIgnoreCase("NO")) {
            out.println("Invalid value");
            return;
        }

        String faculty = session.getAttribute("user").toString();

        try {
            Connection con = DBConnection.getConnection();

            // Verify this issue belongs to this faculty and is in DONE status
            PreparedStatement check = con.prepareStatement(
                "SELECT id FROM issues WHERE id=? AND faculty_email=? AND status='DONE'"
            );
            check.setString(1, id);
            check.setString(2, faculty);
            ResultSet rs = check.executeQuery();

            if (!rs.next()) {
                out.println("Not allowed or issue not in DONE status");
                return;
            }

            PreparedStatement ps = con.prepareStatement(
                "UPDATE issues SET faculty_confirmed=? WHERE id=?"
            );
            ps.setString(1, confirmed.toUpperCase());
            ps.setString(2, id);
            ps.executeUpdate();

            out.println("OK");

        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR: " + e.getMessage());
        }
    }
}

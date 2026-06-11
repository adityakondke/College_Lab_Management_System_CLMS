import java.io.*;
import java.sql.*;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/MoveEntryServlet")
public class MoveEntryServlet extends HttpServlet {

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

        String id      = request.getParameter("id");
        String slotStr = request.getParameter("slot");

        try {
            int slot = Integer.parseInt(slotStr);
            if (slot < 1 || slot > 3) {
                out.println("Invalid slot");
                return;
            }
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "UPDATE timetable SET slot=? WHERE id=?"
            );
            ps.setInt(1, slot);
            ps.setString(2, id);
            ps.executeUpdate();
            out.println("OK");
        } catch (Exception e) {
            e.printStackTrace();
            out.println("ERROR: " + e.getMessage());
        }
    }
}

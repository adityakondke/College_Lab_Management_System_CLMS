import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/DeleteEntireTimetableServlet")
public class DeleteEntireTimetableServlet extends HttpServlet {

    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");

        try {

            // =========================
            // SESSION CHECK
            // =========================

            HttpSession session =
                    request.getSession(false);

            if (session == null ||
                session.getAttribute("role") == null) {

                response.getWriter().println(
                    "Unauthorized Access"
                );

                return;
            }

            // =========================
            // ROLE CHECK
            // =========================

            String role =
                    session.getAttribute("role")
                    .toString();

            if (!role.equalsIgnoreCase(
                    "CLASS_TEACHER")) {

                response.getWriter().println(

                    "Only Class Teacher can delete timetable"
                );

                return;
            }

            // =========================
            // DATABASE CONNECTION
            // =========================

            Connection con =
                    DBConnection.getConnection();

            // =========================
            // DELETE TIMETABLE DATA
            // =========================

            PreparedStatement ps1 =
                    con.prepareStatement(

                "DELETE FROM timetable"
            );

            ps1.executeUpdate();

            ps1.close();

            // =========================
            // DELETE UPLOADED FILE RECORDS
            // =========================

            PreparedStatement ps2 =
                    con.prepareStatement(

                "DELETE FROM uploaded_files"
            );

            ps2.executeUpdate();

            ps2.close();

            // =========================
            // CLOSE CONNECTION
            // =========================

            con.close();

            // =========================
            // SUCCESS RESPONSE
            // =========================

            response.getWriter().println(

                "Entire timetable deleted successfully"
            );
        }

        catch (Exception e) {

            e.printStackTrace();

            response.getWriter().println(

                "Error deleting timetable: "
                + e.getMessage()
            );
        }
    }
}
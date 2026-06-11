import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/AddFacultyServlet")
public class AddFacultyServlet extends HttpServlet {

    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        // =========================
        // GET FORM DATA
        // =========================

        String name =
                request.getParameter("name");

        String email =
                request.getParameter("email");

        String password =
                request.getParameter("password");

        String department =
                request.getParameter("department");

        String role =
                request.getParameter("role");

        // =========================
        // DEFAULT ROLE
        // =========================

        if (role == null || role.trim().isEmpty()) {

            role = "FACULTY";
        }

        // =========================
        // SECURITY VALIDATION
        // =========================

        if (!role.equalsIgnoreCase("FACULTY")
                &&
            !role.equalsIgnoreCase("CLASS_TEACHER")) {

            response.getWriter().println(
                "Invalid role selected"
            );

            return;
        }

        try {

            // =========================
            // DB CONNECTION
            // =========================

            Connection con =
                    DBConnection.getConnection();

            // =========================
            // INSERT USER
            // =========================

            PreparedStatement ps =
                    con.prepareStatement(

                    "INSERT INTO users " +

                    "(name,email,password,role,department) " +

                    "VALUES(?,?,?,?,?)"
            );

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, password);

            // STORE ROLE IN UPPERCASE

            ps.setString(4,
                    role.toUpperCase());

            ps.setString(5, department);

            int i =
                    ps.executeUpdate();

            // =========================
            // SUCCESS
            // =========================

            if (i > 0) {

                response.sendRedirect(
                    "hod-dashboard.html"
                );

            } else {

                response.getWriter().println(
                    "Failed to add user"
                );
            }

            ps.close();
            con.close();

        }

        catch (Exception e) {

            e.printStackTrace();

            response.getWriter().println(

                "Error: " + e.getMessage()
            );
        }
    }
}
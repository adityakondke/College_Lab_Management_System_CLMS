import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/AddTechnicianServlet")
public class AddTechnicianServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        // 🔥 GET FORM DATA
        String name =
                request.getParameter("name");

        String email =
                request.getParameter("email");

        String password =
                request.getParameter("password");

        String department =
                request.getParameter("department");

        try {

            // 🔥 DB CONNECTION
            Connection con =
                    DBConnection.getConnection();

            // 🔥 INSERT QUERY
            String query =
                    "INSERT INTO users " +
                    "(name,email,password,role,department) " +
                    "VALUES(?,?,?,?,?)";

            PreparedStatement ps =
                    con.prepareStatement(query);

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, password);

            // ✅ FIXED ROLE
            ps.setString(4, "TECHNICIAN");

            ps.setString(5, department);

            int i = ps.executeUpdate();

            // 🔥 SUCCESS
            if (i > 0) {

                response.sendRedirect(
                    "hod-dashboard.html"
                );

            } else {

                response.getWriter().println(
                    "Failed to add technician"
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            response.getWriter().println(
                "Error: " + e.getMessage()
            );
        }
    }
}
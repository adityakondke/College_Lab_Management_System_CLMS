import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {

    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            // =========================
            // GET LOGIN DATA
            // =========================

            String email =
                    request.getParameter("email");

            String password =
                    request.getParameter("password");

            // =========================
            // DB CONNECTION
            // =========================

            con = DBConnection.getConnection();

            // =========================
            // CHECK USER
            // =========================

            ps = con.prepareStatement(

                "SELECT * FROM users " +

                "WHERE email=? AND password=?"
            );

            ps.setString(1, email);
            ps.setString(2, password);

            rs = ps.executeQuery();

            // =========================
            // LOGIN SUCCESS
            // =========================

            if (rs.next()) {

                String role =
                        rs.getString("role");

                String name =
                        rs.getString("name");

                // =========================
                // SESSION
                // =========================

                HttpSession session =
                        request.getSession();

                session.setAttribute(
                        "user",
                        email
                );

                session.setAttribute(
                        "email",
                        email
                );

                session.setAttribute(
                        "name",
                        name
                );

                session.setAttribute(
                        "role",
                        role
                );

                // =========================
                // ROLE BASED REDIRECTION
                // =========================

                if (role.equalsIgnoreCase("HOD")) {

                    response.sendRedirect(

                        "http://localhost:8080/CLMS/hod-dashboard.html"
                    );
                }

                else if (role.equalsIgnoreCase("FACULTY")) {

                    response.sendRedirect(

                        "http://localhost:8080/CLMS/faculty-dashboard.html"
                    );
                }

                else if (role.equalsIgnoreCase("CLASS_TEACHER")) {

                    response.sendRedirect(

                        "http://localhost:8080/CLMS/class-teacher-dashboard.html"
                    );
                }

                else if (role.equalsIgnoreCase("TECHNICIAN")) {

                    response.sendRedirect(

                        "http://localhost:8080/CLMS/technician-dashboard.html"
                    );
                }

                else {

                    response.getWriter().println(

                        "Invalid role found"
                    );
                }
            }

            // =========================
            // LOGIN FAILED
            // =========================

            else {

                response.sendRedirect(

                    "login.html?error=1"
                );
            }
        }

        catch (Exception e) {

            e.printStackTrace();

            response.getWriter().println(

                "ERROR: " + e.getMessage()
            );
        }

        finally {

            try {

                if (rs != null)
                    rs.close();

                if (ps != null)
                    ps.close();

                if (con != null)
                    con.close();

            }

            catch (Exception e) {

                e.printStackTrace();
            }
        }
    }
}
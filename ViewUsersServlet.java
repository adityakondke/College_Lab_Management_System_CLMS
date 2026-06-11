import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/ViewUsersServlet")
public class ViewUsersServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    try {
        Connection con = DBConnection.getConnection();

        String query = "SELECT name, email, role, department FROM users";
        PreparedStatement ps = con.prepareStatement(query);
        ResultSet rs = ps.executeQuery();

        out.println("<table border='1' cellpadding='10' style='width:100%; border-collapse:collapse;'>");
        out.println("<tr style='background:#0b3d6d; color:white;'>");
        out.println("<th>Name</th>");
        out.println("<th>Email</th>");
        out.println("<th>Role</th>");
        out.println("<th>Department</th>");
        out.println("<th>Action</th>");
        out.println("</tr>");

        while (rs.next()) {

            String name = rs.getString("name");
            String email = rs.getString("email");
            String role = rs.getString("role");
            String dept = rs.getString("department");

            out.println("<tr>");
            out.println("<td>" + name + "</td>");
            out.println("<td>" + email + "</td>");
            out.println("<td>" + role + "</td>");
            out.println("<td>" + dept + "</td>");

            out.println("<td>");

            // 🔥 EDIT BUTTON
            out.println("<button "
            + "data-name='" + name + "' "
            + "data-email='" + email + "' "
            + "data-role='" + role + "' "
            + "data-dept='" + dept + "' "
            + "onclick='editUserFromBtn(this)' "
            + "style='background:orange; margin-right:5px;'>Edit</button>");

            // 🔥 DELETE BUTTON
            out.println("<button onclick=\"deleteUser('" + email + "')\" style='background:red;'>Delete</button>");

            out.println("</td>");

            out.println("</tr>");
        }

        out.println("</table>");

    } catch (Exception e) {
        e.printStackTrace();
        out.println("<p style='color:red;'>Error: " + e.getMessage() + "</p>");
    }
}
}
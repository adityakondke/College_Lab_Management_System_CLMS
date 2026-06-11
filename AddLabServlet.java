import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/AddLabServlet")
public class AddLabServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws IOException {

        response.setContentType("text/plain");

        // GET FORM VALUES
        String labName =
                request.getParameter("lab_name");

        String location =
                request.getParameter("location");

        // DEBUG LOG
        System.out.println("LAB NAME = " + labName);
        System.out.println("LOCATION = " + location);

        // VALIDATION
        if (labName == null ||
            labName.trim().isEmpty() ||

            location == null ||
            location.trim().isEmpty()) {

            response.getWriter().println(
                "❌ All fields are required"
            );

            return;
        }

        // REMOVE EXTRA SPACES
        labName = labName.trim();
        location = location.trim();

        try {

            Connection con =
                    DBConnection.getConnection();

            System.out.println(
                "DB = " + con.getCatalog()
            );

            // CHECK DUPLICATE LAB
            String checkQuery =
                    "SELECT COUNT(*) FROM labs WHERE lab_name=?";

            PreparedStatement checkPs =
                    con.prepareStatement(checkQuery);

            checkPs.setString(1, labName);

            java.sql.ResultSet rs =
                    checkPs.executeQuery();

            rs.next();

            int count = rs.getInt(1);

            if (count > 0) {

                response.getWriter().println(
                    "❌ Lab already exists"
                );

                return;
            }

            // INSERT LAB
            String query =
                    "INSERT INTO labs(lab_name, location) VALUES(?, ?)";

            PreparedStatement ps =
                    con.prepareStatement(query);

            ps.setString(1, labName);
            ps.setString(2, location);

            int rows =
                    ps.executeUpdate();

            if (rows > 0) {

                response.getWriter().println(
                    "Lab added successfully!"
                );

            } else {

                response.getWriter().println(
                    "❌ Failed to add lab"
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            response.getWriter().println(
                "❌ " + e.getMessage()
            );
        }
    }
}
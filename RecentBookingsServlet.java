import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/RecentBookingsServlet")
public class RecentBookingsServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {

        response.setContentType("text/html");

        PrintWriter out =
                response.getWriter();

        try {

            Connection con =
                    DBConnection.getConnection();

            String query =
                    "SELECT faculty_email, lab, booking_date, slot " +
                    "FROM lab_booking " +
                    "ORDER BY booking_date DESC " +
                    "LIMIT 5";

            PreparedStatement ps =
                    con.prepareStatement(query);

            ResultSet rs =
                    ps.executeQuery();

            out.println(
                "<table class='quick-table'>"
            );

            out.println(
                "<tr>" +

                "<th>Faculty</th>" +

                "<th>Lab</th>" +

                "<th>Date</th>" +

                "<th>Slot</th>" +

                "</tr>"
            );

            boolean hasData = false;

            while (rs.next()) {

                hasData = true;

                out.println("<tr>");

                out.println(
                    "<td>" +
                    rs.getString("faculty_email") +
                    "</td>"
                );

                out.println(
                    "<td>" +
                    rs.getString("lab") +
                    "</td>"
                );

                out.println(
                    "<td>" +
                    rs.getString("booking_date") +
                    "</td>"
                );

                out.println(
                    "<td>" +
                    rs.getString("slot") +
                    "</td>"
                );

                out.println("</tr>");
            }

            if (!hasData) {

                out.println(
                    "<tr>" +

                    "<td colspan='4'>" +

                    "No recent bookings found"

                    +

                    "</td>" +

                    "</tr>"
                );
            }

            out.println("</table>");

        } catch (Exception e) {

            e.printStackTrace();

            out.println(
                "<p style='color:red;'>Failed to load bookings</p>"
            );
        }
    }
}

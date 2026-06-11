import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/ViewBookingsServlet")
public class ViewBookingsServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");

        PrintWriter out =
                response.getWriter();

        try {

            Connection con =
                    DBConnection.getConnection();

            String query =
                    "SELECT * FROM lab_booking " +
                    "WHERE booking_date >= CURDATE() " +
                    "ORDER BY booking_date, slot";

            PreparedStatement ps =
                    con.prepareStatement(query);

            ResultSet rs =
                    ps.executeQuery();

            boolean found = false;

            while (rs.next()) {

                found = true;

                String faculty =
                        rs.getString("faculty_email");

                String lab =
                        rs.getString("lab");

                String date =
                        rs.getString("booking_date");

                String slot =
                        rs.getString("slot");

                out.println(

                    "<div style='background:#fff3cd;" +
                    "padding:15px;" +
                    "margin-top:12px;" +
                    "border-left:5px solid orange;" +
                    "border-radius:8px;'>"

                    +

                    "<h3 style='margin:0;color:#856404;'>"
                    + "📌 Active Lab Booking"
                    + "</h3>"

                    +

                    "<p><b>Faculty:</b> "
                    + faculty +
                    "</p>"

                    +

                    "<p><b>Lab:</b> "
                    + lab +
                    "</p>"

                    +

                    "<p><b>Date:</b> "
                    + date +
                    "</p>"

                    +

                    "<p><b>Slot:</b> "
                    + slot +
                    "</p>"

                    +

                    "</div>"
                );
            }

            if (!found) {

                out.println(

                    "<div style='color:gray;font-weight:bold;'>"
                    + "No active bookings"
                    + "</div>"
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            out.println(
                "<div style='color:red;'>Error loading bookings</div>"
            );
        }
    }
}
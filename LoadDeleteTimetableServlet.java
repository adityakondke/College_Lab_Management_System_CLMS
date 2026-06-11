import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/LoadDeleteTimetableServlet")
public class LoadDeleteTimetableServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {

        response.setContentType("text/html");

        PrintWriter out =
                response.getWriter();

        try {

            Connection con =
                    DBConnection.getConnection();

            // 🔥 SORT DAY-WISE & SLOT-WISE
            String query =
                    "SELECT * FROM timetable " +
                    "ORDER BY " +

                    "FIELD(day, " +
                    "'Monday'," +
                    "'Tuesday'," +
                    "'Wednesday'," +
                    "'Thursday'," +
                    "'Friday'," +
                    "'Saturday'" +
                    "), " +

                    "FIELD(slot, " +
                    "'SLOT1'," +
                    "'SLOT2'," +
                    "'SLOT3'" +
                    ")";

            PreparedStatement ps =
                    con.prepareStatement(query);

            ResultSet rs =
                    ps.executeQuery();

            // 🔥 TABLE START
            out.println(
                "<table style='width:100%; border-collapse:collapse; text-align:center;'>"
            );

            // 🔥 HEADER
            out.println(
                "<tr style='background:#0b3d6d; color:white;'>"

                +

                "<th style='padding:12px;border:1px solid #ccc;'>Subject</th>"

                +

                "<th style='padding:12px;border:1px solid #ccc;'>Lab</th>"

                +

                "<th style='padding:12px;border:1px solid #ccc;'>Batch</th>"

                +

                "<th style='padding:12px;border:1px solid #ccc;'>Day</th>"

                +

                "<th style='padding:12px;border:1px solid #ccc;'>Slot</th>"

                +

                "<th style='padding:12px;border:1px solid #ccc;'>Action</th>"

                +

                "</tr>"
            );

            // 🔥 ROWS
            while (rs.next()) {

                int id =
                        rs.getInt("id");

                out.println("<tr>");

                out.println(
                    "<td style='padding:10px;border:1px solid #ccc;'>"

                    +

                    rs.getString("subject")

                    +

                    "</td>"
                );

                out.println(
                    "<td style='padding:10px;border:1px solid #ccc;'>"

                    +

                    rs.getString("lab")

                    +

                    "</td>"
                );

                out.println(
                    "<td style='padding:10px;border:1px solid #ccc;'>"

                    +

                    rs.getString("batch")

                    +

                    "</td>"
                );

                out.println(
                    "<td style='padding:10px;border:1px solid #ccc;'>"

                    +

                    rs.getString("day")

                    +

                    "</td>"
                );

                out.println(
                    "<td style='padding:10px;border:1px solid #ccc;'>"

                    +

                    rs.getString("slot")

                    +

                    "</td>"
                );

                out.println(

                    "<td style='padding:10px;border:1px solid #ccc;'>"

                    +

                    "<button "

                    +

                    "style='background:red;color:white;padding:8px 14px;border:none;border-radius:5px;cursor:pointer;' "

                    +

                    "onclick='deleteEntry(" + id + ")'>"

                    +

                    "Delete"

                    +

                    "</button>"

                    +

                    "</td>"
                );

                out.println("</tr>");
            }

            // 🔥 TABLE END
            out.println("</table>");

        } catch (Exception e) {

            e.printStackTrace();

            out.println(
                "<p style='color:red;'>Error loading timetable entries</p>"
            );
        }
    }
}
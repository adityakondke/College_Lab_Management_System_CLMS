import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/RecentIssuesServlet")
public class RecentIssuesServlet extends HttpServlet {

    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        response.setContentType("text/html");

        PrintWriter out =
                response.getWriter();

        try {

            Connection con =
                    DBConnection.getConnection();

            String query =
                    "SELECT * FROM issues " +
                    "ORDER BY id DESC LIMIT 5";

            PreparedStatement ps =
                    con.prepareStatement(query);

            ResultSet rs =
                    ps.executeQuery();

            out.println(
                "<table class='quick-table'>"
            );

            out.println(
                "<tr>" +
                "<th>Lab</th>" +
                "<th>Issue</th>" +
                "<th>Status</th>" +
                "</tr>"
            );

            boolean hasData = false;

            while (rs.next()) {

                hasData = true;

                String status =
                        rs.getString("status");

                String confirmed =
                        rs.getString("faculty_confirmed");

                String displayStatus =
                        status;

                String statusClass =
                        "status-pending";

                // =========================
                // COMPLETED LOGIC
                // =========================

                if("YES".equalsIgnoreCase(confirmed)){

                    displayStatus = "COMPLETED";

                    statusClass = "status-done";
                }

                else if (
                    status.equalsIgnoreCase(
                        "IN PROGRESS"
                    )
                ) {

                    statusClass =
                            "status-progress";
                }

                else if (
                    status.equalsIgnoreCase(
                        "PENDING"
                    )
                ) {

                    statusClass =
                            "status-pending";
                }

                // =========================
                // ISSUE TEXT
                // =========================

                String issue =
                        rs.getString("issue_type");

                if (
                    issue.equalsIgnoreCase("OTHER")
                ) {

                    issue =
                        rs.getString("custom_issue");
                }

                out.println("<tr>");

                out.println(
                    "<td>" +
                    rs.getString("lab") +
                    "</td>"
                );

                out.println(
                    "<td>" +
                    issue +
                    "</td>"
                );

                out.println(
                    "<td class='" +
                    statusClass +
                    "'>" +
                    displayStatus +
                    "</td>"
                );

                out.println("</tr>");
            }

            if (!hasData) {

                out.println(
                    "<tr>" +
                    "<td colspan='3'>" +
                    "No recent issues found" +
                    "</td>" +
                    "</tr>"
                );
            }

            out.println("</table>");

        }

        catch (Exception e) {

            e.printStackTrace();

            out.println(
                "<p style='color:red;'>Failed to load issues</p>"
            );
        }
    }
}

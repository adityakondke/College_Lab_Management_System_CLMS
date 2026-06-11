import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/ViewIssuesServlet")
public class ViewIssuesServlet extends HttpServlet {

    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out =
                response.getWriter();

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            // =========================
            // DB CONNECTION
            // =========================

            con = DBConnection.getConnection();

            ps = con.prepareStatement(

                "SELECT * FROM issues " +

                "ORDER BY id DESC"
            );

            rs = ps.executeQuery();

            // =========================
            // TABLE START
            // =========================

            out.println(

                "<table style='width:100%;border-collapse:collapse;text-align:center;'>"
            );

            out.println(

                "<tr style='background:#0b3d6d;color:white;'>" +

                "<th style='padding:12px;'>Faculty</th>" +

                "<th style='padding:12px;'>Lab</th>" +

                "<th style='padding:12px;'>System No</th>" +

                "<th style='padding:12px;'>Issue</th>" +

                "<th style='padding:12px;'>Status</th>" +

                "<th style='padding:12px;'>Faculty Confirmed</th>" +

                "<th style='padding:12px;'>Action</th>" +

                "</tr>"
            );

            // =========================
            // LOOP
            // =========================

            while (rs.next()) {

                int id =
                        rs.getInt("id");

                String status =
                        rs.getString("status");

                String confirmed =
                        rs.getString(
                        "faculty_confirmed"
                );

                String issueText;

                if ("OTHER".equalsIgnoreCase(
                        rs.getString("issue_type"))) {

                    issueText =
                            rs.getString(
                            "custom_issue"
                    );

                } else {

                    issueText =
                            rs.getString(
                            "issue_type"
                    );
                }

                // =========================
                // STATUS COLOR
                // =========================

                String statusColor =
                        "#ff9800";

                if ("IN PROGRESS"
                        .equalsIgnoreCase(status)) {

                    statusColor = "#1565c0";
                }

                else if ("DONE"
                        .equalsIgnoreCase(status)) {

                    statusColor = "#2e7d32";
                }

                else if ("COMPLETED"
                        .equalsIgnoreCase(status)) {

                    statusColor = "#008000";
                }

                // =========================
                // ROW START
                // =========================

                out.println("<tr>");

                // FACULTY

                out.println(

                    "<td style='padding:10px;border:1px solid #ccc;'>"

                    +

                    rs.getString("faculty_email")

                    +

                    "</td>"
                );

                // LAB

                out.println(

                    "<td style='padding:10px;border:1px solid #ccc;'>"

                    +

                    rs.getString("lab")

                    +

                    "</td>"
                );

                // SYSTEM NO

                out.println(

                    "<td style='padding:10px;border:1px solid #ccc;'>"

                    +

                    rs.getString("system_no")

                    +

                    "</td>"
                );

                // ISSUE

                out.println(

                    "<td style='padding:10px;border:1px solid #ccc;'>"

                    +

                    issueText

                    +

                    "</td>"
                );

                // STATUS

                out.println(

                    "<td style='padding:10px;border:1px solid #ccc;font-weight:bold;color:"
                    +

                    statusColor

                    +

                    ";'>"

                    +

                    status

                    +

                    "</td>"
                );

                // =========================
                // FACULTY CONFIRMATION
                // =========================

                if ("YES".equalsIgnoreCase(
                        confirmed)) {

                    out.println(

                        "<td style='padding:10px;border:1px solid #ccc;color:green;font-weight:bold;'>YES</td>"
                    );
                }

                else {

                    out.println(

                        "<td style='padding:10px;border:1px solid #ccc;color:#666;font-weight:bold;'>Pending</td>"
                    );
                }

                // =========================
                // ACTION COLUMN
                // =========================

                out.println(

                    "<td style='padding:12px;border:1px solid #ccc;'>"
                );

                // =========================
                // PENDING
                // =========================

                if ("PENDING"
                        .equalsIgnoreCase(status)) {

                    out.println(

                        "<button " +

                        "onclick=\"updateIssueStatus("
                        + id +
                        ",'IN PROGRESS')\" " +

                        "style='background:#ff9800;color:white;border:none;padding:8px 18px;border-radius:8px;font-weight:600;cursor:pointer;'>"

                        +

                        "IN PROGRESS"

                        +

                        "</button>"
                    );
                }

                // =========================
                // IN PROGRESS
                // =========================

                else if ("IN PROGRESS"
                        .equalsIgnoreCase(status)) {

                    out.println(

                        "<button " +

                        "onclick=\"updateIssueStatus("
                        + id +
                        ",'DONE')\" " +

                        "style='background:#0a8f08;color:white;border:none;padding:8px 18px;border-radius:8px;font-weight:600;cursor:pointer;'>"

                        +

                        "DONE"

                        +

                        "</button>"
                    );
                }

                // =========================
                // DONE
                // =========================

                else if ("DONE"
                        .equalsIgnoreCase(status)) {

                    out.println(

                        "<span style='color:#1565c0;font-weight:bold;'>"

                        +

                        "WAITING FOR FACULTY"

                        +

                        "</span>"
                    );
                }

                // =========================
                // COMPLETED
                // =========================

                else if ("COMPLETED"
                        .equalsIgnoreCase(status)) {

                    out.println(

                        "<span style='color:green;font-weight:bold;'>"

                        +

                        "COMPLETED"

                        +

                        "</span>"
                    );
                }

                out.println("</td>");

                // =========================
                // ROW END
                // =========================

                out.println("</tr>");
            }

            out.println("</table>");
        }

        catch (Exception e) {

            e.printStackTrace();

            out.println(

                "<div style='color:red;font-weight:bold;'>"

                +

                "Error loading issues"

                +

                "</div>"
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
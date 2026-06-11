import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/ViewFacultyIssuesServlet")
public class ViewFacultyIssuesServlet extends HttpServlet {

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
            // SESSION CHECK
            // =========================

            HttpSession session =
                    request.getSession(false);

            if (session == null) {

                out.println(

                    "<div style='color:red;font-weight:bold;'>"

                    +

                    "Session Expired"

                    +

                    "</div>"
                );

                return;
            }

            String faculty = "";

            if (session.getAttribute("email")
                    != null) {

                faculty =
                        session.getAttribute(
                        "email"
                ).toString();
            }

            else if (session.getAttribute("user")
                    != null) {

                faculty =
                        session.getAttribute(
                        "user"
                ).toString();
            }

            else {

                out.println(

                    "<div style='color:red;font-weight:bold;'>"

                    +

                    "User Session Not Found"

                    +

                    "</div>"
                );

                return;
            }

            // =========================
            // DB CONNECTION
            // =========================

            con = DBConnection.getConnection();

            ps = con.prepareStatement(

                "SELECT * FROM issues " +

                "WHERE faculty_email=? " +

                "ORDER BY reported_at DESC"
            );

            ps.setString(1, faculty);

            rs = ps.executeQuery();

            // =========================
            // TABLE START
            // =========================

            out.println(

                "<table style='width:100%;border-collapse:collapse;text-align:center;margin-top:15px;'>"
            );

            out.println(

                "<tr style='background:#0b3d6d;color:white;'>"

                +

                "<th style='padding:12px;border:1px solid #ccc;'>Lab</th>"

                +

                "<th style='padding:12px;border:1px solid #ccc;'>System No</th>"

                +

                "<th style='padding:12px;border:1px solid #ccc;'>Issue</th>"

                +

                "<th style='padding:12px;border:1px solid #ccc;'>Status</th>"

                +

                "<th style='padding:12px;border:1px solid #ccc;'>Issue Solved?</th>"

                +

                "</tr>"
            );

            boolean found = false;

            // =========================
            // LOOP
            // =========================

            while (rs.next()) {

                found = true;

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
                        rs.getString(
                        "issue_type"))) {

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

                // LAB

                out.println(

                    "<td style='padding:12px;border:1px solid #ccc;'>"

                    +

                    rs.getString("lab")

                    +

                    "</td>"
                );

                // SYSTEM NO

                out.println(

                    "<td style='padding:12px;border:1px solid #ccc;'>"

                    +

                    rs.getString("system_no")

                    +

                    "</td>"
                );

                // ISSUE

                out.println(

                    "<td style='padding:12px;border:1px solid #ccc;'>"

                    +

                    issueText

                    +

                    "</td>"
                );

                // STATUS

                out.println(

                    "<td style='padding:12px;border:1px solid #ccc;font-weight:bold;color:"
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
                // ISSUE SOLVED COLUMN
                // =========================

                out.println(

                    "<td style='padding:12px;border:1px solid #ccc;'>"
                );

                // =========================
                // WAITING FOR TECHNICIAN
                // =========================

                if ("PENDING"
                        .equalsIgnoreCase(status)) {

                    out.println(

                        "<span style='color:#ff9800;font-weight:bold;'>"

                        +

                        "Pending"

                        +

                        "</span>"
                    );
                }

                // =========================
                // TECHNICIAN WORKING
                // =========================

                else if ("IN PROGRESS"
                        .equalsIgnoreCase(status)) {

                    out.println(

                        "<span style='color:#1565c0;font-weight:bold;'>"

                        +

                        "Technician Working"

                        +

                        "</span>"
                    );
                }

                // =========================
                // DONE → SHOW YES/NO
                // =========================

                else if ("DONE"
                        .equalsIgnoreCase(status)) {

                    out.println(

                        "<button " +

                        "onclick=\"confirmIssue("
                        + id +
                        ",'YES')\" " +

                        "style='background:green;color:white;padding:7px 14px;border:none;border-radius:6px;margin-right:8px;cursor:pointer;font-weight:bold;'>"

                        +

                        "YES"

                        +

                        "</button>"

                        +

                        "<button " +

                        "onclick=\"confirmIssue("
                        + id +
                        ",'NO')\" " +

                        "style='background:#e53935;color:white;padding:7px 14px;border:none;border-radius:6px;cursor:pointer;font-weight:bold;'>"

                        +

                        "NO"

                        +

                        "</button>"
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

                out.println("</tr>");
            }

            // =========================
            // EMPTY STATE
            // =========================

            if (!found) {

                out.println(

                    "<tr>"

                    +

                    "<td colspan='5' style='padding:20px;color:#666;font-weight:bold;'>"

                    +

                    "No Reported Issues Found"

                    +

                    "</td>"

                    +

                    "</tr>"
                );
            }

            out.println("</table>");
        }

        catch (Exception e) {

            e.printStackTrace();

            out.println(

                "<div style='color:red;font-weight:bold;'>"

                +

                "Error Loading Issues"

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
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/ReportIssueServlet")
public class ReportIssueServlet extends HttpServlet {

    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        response.setContentType("text/plain");

        Connection con = null;
        PreparedStatement ps = null;

        try {

            HttpSession session =
                    request.getSession(false);

            if(session == null ||
               session.getAttribute("user") == null){

                response.getWriter().println(
                    "Session Expired"
                );

                return;
            }

            String faculty =
                    session.getAttribute("user")
                    .toString();

            String lab =
                    request.getParameter("lab");

            String systemNo =
                    request.getParameter("systemNo");

            String issueType =
                    request.getParameter("issueType");

            String customIssue =
                    request.getParameter("customIssue");

            con = DBConnection.getConnection();

            String query =

                "INSERT INTO issues " +

                "(faculty_email,lab,system_no,issue_type,custom_issue,status,faculty_confirmed) " +

                "VALUES(?,?,?,?,?,?,?)";

            ps = con.prepareStatement(query);

            ps.setString(1, faculty);
            ps.setString(2, lab);
            ps.setString(3, systemNo);
            ps.setString(4, issueType);
            ps.setString(5, customIssue);

            // DEFAULT STATUS

            ps.setString(6, "PENDING");

            // NO CONFIRMATION YET

            ps.setNull(7, java.sql.Types.VARCHAR);

            ps.executeUpdate();

            response.getWriter().println(
                "Issue reported successfully"
            );
        }

        catch(Exception e) {

            e.printStackTrace();

            response.getWriter().println(
                "Error reporting issue"
            );
        }

        finally {

            try {

                if(ps != null)
                    ps.close();

                if(con != null)
                    con.close();

            } catch(Exception e){

                e.printStackTrace();
            }
        }
    }
}
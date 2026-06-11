import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/DashboardStatsServlet")
public class DashboardStatsServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");

        PrintWriter out =
                response.getWriter();

        try {

            Connection con =
                    DBConnection.getConnection();

            // TOTAL FACULTIES
            int facultyCount = 0;

            String facultyQuery =
                    "SELECT COUNT(*) FROM users WHERE role='FACULTY' OR role='CLASS_TEACHER'";

            PreparedStatement facultyPs =
                    con.prepareStatement(facultyQuery);

            ResultSet facultyRs =
                    facultyPs.executeQuery();

            if (facultyRs.next()) {

                facultyCount =
                        facultyRs.getInt(1);
            }

            // TOTAL TECHNICIANS
            int techCount = 0;

            String techQuery =
                    "SELECT COUNT(*) FROM users " +
                    "WHERE role='TECHNICIAN'";

            PreparedStatement techPs =
                    con.prepareStatement(techQuery);

            ResultSet techRs =
                    techPs.executeQuery();

            if (techRs.next()) {

                techCount =
                        techRs.getInt(1);
            }

            // TOTAL LABS
            int labCount = 0;

            String labQuery =
                    "SELECT COUNT(*) FROM labs";

            PreparedStatement labPs =
                    con.prepareStatement(labQuery);

            ResultSet labRs =
                    labPs.executeQuery();

            if (labRs.next()) {

                labCount =
                        labRs.getInt(1);
            }

            // PENDING ISSUES
            int pendingIssues = 0;

            String issueQuery =
                    "SELECT COUNT(*) FROM issues WHERE status='PENDING'";

            PreparedStatement issuePs =
                    con.prepareStatement(issueQuery);

            ResultSet issueRs =
                    issuePs.executeQuery();

            if (issueRs.next()) {

                pendingIssues =
                        issueRs.getInt(1);
            }

            // JSON RESPONSE
            String json =
                    "{"

                    + "\"faculties\":"
                    + facultyCount + ","

                    + "\"technicians\":"
                    + techCount + ","

                    + "\"labs\":"
                    + labCount + ","

                    + "\"issues\":"
                    + pendingIssues

                    + "}";

            out.print(json);

        } catch (Exception e) {

            e.printStackTrace();

            out.print(
                "{\"error\":\"Failed to load stats\"}"
            );
        }
    }
}
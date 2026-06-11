import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/IssueAnalyticsServlet")
public class IssueAnalyticsServlet extends HttpServlet {

    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");

        PrintWriter out =
                response.getWriter();

        Connection con = null;

        try {

            con = DBConnection.getConnection();

            int pending = 0;
            int progress = 0;
            int completed = 0;

            // =========================
            // PENDING COUNT
            // =========================

            PreparedStatement psPending =
                    con.prepareStatement(

                    "SELECT COUNT(*) AS total " +

                    "FROM issues " +

                    "WHERE status='PENDING'"
            );

            ResultSet rsPending =
                    psPending.executeQuery();

            if(rsPending.next()){

                pending =
                        rsPending.getInt("total");
            }

            // =========================
            // IN PROGRESS COUNT
            // =========================

            PreparedStatement psProgress =
                    con.prepareStatement(

                    "SELECT COUNT(*) AS total " +

                    "FROM issues " +

                    "WHERE status='IN PROGRESS'"
            );

            ResultSet rsProgress =
                    psProgress.executeQuery();

            if(rsProgress.next()){

                progress =
                        rsProgress.getInt("total");
            }

            // =========================
            // COMPLETED COUNT
            // =========================

            PreparedStatement psCompleted =
                    con.prepareStatement(

                    "SELECT COUNT(*) AS total " +

                    "FROM issues " +

                    "WHERE faculty_confirmed='YES'"
            );

            ResultSet rsCompleted =
                    psCompleted.executeQuery();

            if(rsCompleted.next()){

                completed =
                        rsCompleted.getInt("total");
            }

            // =========================
            // FINAL JSON
            // =========================

            out.print("{");

            out.print(
                "\"pending\":" + pending + ","
            );

            out.print(
                "\"progress\":" + progress + ","
            );

            out.print(
                "\"done\":" + completed
            );

            out.print("}");

        }

        catch(Exception e){

            e.printStackTrace();

            out.print(
                "{\"pending\":0,\"progress\":0,\"done\":0}"
            );
        }

        finally {

            try {

                if(con != null)
                    con.close();

            } catch(Exception e){

                e.printStackTrace();
            }
        }
    }
}

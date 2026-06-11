import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/ConfirmIssueServlet")
public class ConfirmIssueServlet extends HttpServlet {

    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        response.setContentType("text/plain");

        PrintWriter out =
                response.getWriter();

        Connection con = null;
        PreparedStatement ps = null;

        try {

            int id =
                    Integer.parseInt(
                    request.getParameter("id")
            );

            String status =
                    request.getParameter("status");

            con = DBConnection.getConnection();

            // =========================
            // FACULTY CLICKED YES
            // =========================

            if(status.equalsIgnoreCase("YES")){

                ps = con.prepareStatement(

                    "UPDATE issues " +

                    "SET faculty_confirmed=?, " +

                    "status=? " +

                    "WHERE id=?"
                );

                ps.setString(1, "YES");

                ps.setString(2, "COMPLETED");

                ps.setInt(3, id);
            }

            // =========================
            // FACULTY CLICKED NO
            // =========================

            else {

                ps = con.prepareStatement(

                    "UPDATE issues " +

                    "SET faculty_confirmed=?, " +

                    "status=? " +

                    "WHERE id=?"
                );

                // RESET AGAIN

                ps.setNull(
                    1,
                    java.sql.Types.VARCHAR
                );

                // REOPEN ISSUE

                ps.setString(2, "PENDING");

                ps.setInt(3, id);
            }

            int rows =
                    ps.executeUpdate();

            if(rows > 0){

                out.print(
                    "Issue updated successfully"
                );

            } else {

                out.print(
                    "Issue not found"
                );
            }
        }

        catch(Exception e){

            e.printStackTrace();

            out.print(
                "Error updating issue"
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
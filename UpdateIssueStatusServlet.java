import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/UpdateIssueStatusServlet")
public class UpdateIssueStatusServlet extends HttpServlet {

    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        response.setContentType("text/plain");

        Connection con = null;
        PreparedStatement ps = null;

        try {

            String id =
                    request.getParameter("id");

            String status =
                    request.getParameter("status");

            con = DBConnection.getConnection();

            String query =

                "UPDATE issues " +

                "SET status=? " +

                "WHERE id=?";

            ps = con.prepareStatement(query);

            ps.setString(1, status);
            ps.setString(2, id);

            int rows =
                    ps.executeUpdate();

            if (rows > 0) {

                response.getWriter().println(
                    "Status Updated"
                );

            } else {

                response.getWriter().println(
                    "Failed"
                );
            }
        }

        catch (Exception e) {

            e.printStackTrace();

            response.getWriter().println(
                "Error Updating Status"
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
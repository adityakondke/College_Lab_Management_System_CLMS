import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/GetLabsServlet")
public class GetLabsServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");

        PrintWriter out = response.getWriter();

        try {

            Connection con =
                    DBConnection.getConnection();

            String query =
                    "SELECT lab_name FROM labs";

            PreparedStatement ps =
                    con.prepareStatement(query);

            ResultSet rs =
                    ps.executeQuery();

            out.print("[");

            boolean first = true;

            while (rs.next()) {

                if (!first) {
                    out.print(",");
                }

                out.print(
                    "\"" +
                    rs.getString("lab_name") +
                    "\""
                );

                first = false;
            }

            out.print("]");

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}

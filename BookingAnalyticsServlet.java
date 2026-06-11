import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/BookingAnalyticsServlet")
public class BookingAnalyticsServlet extends HttpServlet {

    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");

        PrintWriter out =
                response.getWriter();

        Connection con = null;

        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;

        ResultSet rs1 = null;
        ResultSet rs2 = null;

        try {

            // =========================
            // DB CONNECTION
            // =========================

            con = DBConnection.getConnection();

            // =========================
            // MAP FOR FINAL COUNTS
            // =========================

            Map<String,Integer> usageMap =
                    new HashMap<>();

            // =========================
            // TIMETABLE USAGE
            // =========================

            String timetableQuery =

                "SELECT lab_name, COUNT(*) AS total " +

                "FROM timetable " +

                "GROUP BY lab_name";

            ps1 = con.prepareStatement(
                    timetableQuery
            );

            rs1 = ps1.executeQuery();

            while(rs1.next()){

                String lab =
                        rs1.getString(
                        "lab_name"
                );

                int total =
                        rs1.getInt(
                        "total"
                );

                usageMap.put(lab, total);
            }

            // =========================
            // EXTRA LAB BOOKINGS
            // =========================

            String bookingQuery =

                "SELECT lab, COUNT(*) AS total " +

                "FROM lab_booking " +

                "GROUP BY lab";

            ps2 = con.prepareStatement(
                    bookingQuery
            );

            rs2 = ps2.executeQuery();

            while(rs2.next()){

                String lab =
                        rs2.getString(
                        "lab"
                );

                int total =
                        rs2.getInt(
                        "total"
                );

                // ADD TO EXISTING COUNT

                if(usageMap.containsKey(lab)){

                    usageMap.put(

                        lab,

                        usageMap.get(lab)
                        + total
                    );

                } else {

                    usageMap.put(lab, total);
                }
            }

            // =========================
            // BUILD JSON
            // =========================

            StringBuilder labels =
                    new StringBuilder();

            StringBuilder values =
                    new StringBuilder();

            for(Map.Entry<String,Integer> entry
                    : usageMap.entrySet()){

                labels.append("\"")
                      .append(entry.getKey())
                      .append("\",");

                values.append(
                        entry.getValue()
                ).append(",");
            }

            // REMOVE LAST COMMA

            if(labels.length() > 0){

                labels.deleteCharAt(
                        labels.length() - 1
                );

                values.deleteCharAt(
                        values.length() - 1
                );
            }

            // =========================
            // FINAL JSON
            // =========================

            out.print("{");

            out.print(

                "\"labels\":["

                +

                labels.toString()

                +

                "],"
            );

            out.print(

                "\"values\":["

                +

                values.toString()

                +

                "]"
            );

            out.print("}");
        }

        catch(Exception e){

            e.printStackTrace();

            out.print(
                "{\"labels\":[],\"values\":[]}"
            );
        }

        finally {

            try {

                if(rs1 != null)
                    rs1.close();

                if(rs2 != null)
                    rs2.close();

                if(ps1 != null)
                    ps1.close();

                if(ps2 != null)
                    ps2.close();

                if(con != null)
                    con.close();

            } catch(Exception e){

                e.printStackTrace();
            }
        }
    }
}


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/AddTimetableServlet")
public class AddTimetableServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 🔥 GET FORM DATA
        String lab = request.getParameter("lab");
        String subject = request.getParameter("subject");
        String batch = request.getParameter("batch");
        String day = request.getParameter("day");
        String slot = request.getParameter("slot");

        // 🔐 SESSION CHECK
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {

            response.sendRedirect("login.html");
            return;
        }

        String faculty =
                ((String) session.getAttribute("user"))
                .trim()
                .toLowerCase();

        try {

            Connection con = DBConnection.getConnection();

            // 🔥 VALIDATION
            if (
                lab == null || lab.isEmpty() ||
                subject == null || subject.isEmpty() ||
                batch == null || batch.isEmpty() ||
                day == null || day.isEmpty() ||
                slot == null || slot.isEmpty()
            ) {

                response.getWriter().println("❌ All fields are required");
                return;
            }

            // 🔥 BATCH CONFLICT CHECK
            // SAME BATCH CANNOT HAVE 2 PRACTICALS AT SAME TIME

            String batchQuery =
                    "SELECT * FROM timetable " +
                    "WHERE day=? AND slot=? AND batch=?";

            PreparedStatement batchPs =
                    con.prepareStatement(batchQuery);

            batchPs.setString(1, day);
            batchPs.setString(2, slot);
            batchPs.setString(3, batch);

            ResultSet batchRs =
                    batchPs.executeQuery();

            if (batchRs.next()) {

                response.getWriter().println(
                    "❌ This batch already has another practical in selected slot"
                );

                return;
            }

            // 🔥 SPECIAL LABS SUPPORT 2 BATCHES

            boolean dualLab =
                    lab.equalsIgnoreCase("Red Hat")
                    || lab.equalsIgnoreCase("Web Tech")
                    || lab.equalsIgnoreCase("CP Lab");

            // 🔥 COUNT EXISTING ENTRIES IN SAME SLOT

            String labQuery =
                    "SELECT COUNT(*) as total " +
                    "FROM timetable " +
                    "WHERE day=? AND slot=? AND lab=?";

            PreparedStatement labPs =
                    con.prepareStatement(labQuery);

            labPs.setString(1, day);
            labPs.setString(2, slot);
            labPs.setString(3, lab);

            ResultSet labRs =
                    labPs.executeQuery();

            if (labRs.next()) {

                int total =
                        labRs.getInt("total");

                // 🔥 NORMAL LABS = ONLY 1 BATCH

                if (!dualLab && total >= 1) {

                    response.getWriter().println(
                        "❌ This lab is already occupied in selected slot"
                    );

                    return;
                }

                // 🔥 SPECIAL LABS = MAX 2 BATCHES

                if (dualLab && total >= 2) {

                    response.getWriter().println(
                        "❌ Maximum 2 batches allowed in this lab for selected slot"
                    );

                    return;
                }
            }

            // 🔥 INSERT TIMETABLE

            String query =
                    "INSERT INTO timetable " +
                    "(faculty_email, subject, lab, batch, day, slot) " +
                    "VALUES(?,?,?,?,?,?)";

            PreparedStatement ps =
                    con.prepareStatement(query);

            ps.setString(1, faculty);
            ps.setString(2, subject);
            ps.setString(3, lab);
            ps.setString(4, batch);
            ps.setString(5, day);
            ps.setString(6, slot);

            ps.executeUpdate();

            // 🔥 SUCCESS

            response.sendRedirect("faculty-dashboard.html");

        } catch (Exception e) {

            e.printStackTrace();

            response.getWriter().println(
                "❌ Error: " + e.getMessage()
            );
        }
    }
}
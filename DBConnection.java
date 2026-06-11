import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/lab_management_new";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() {

        Connection con = null;

        try {
            // Load MySQL Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Create Connection
            con = DriverManager.getConnection(URL, USER, PASSWORD);

            System.out.println("[SUCCESS] Database Connected Successfully!");

        } catch (ClassNotFoundException e) {
            System.out.println("[ERROR] MySQL Driver Not Found!");
            e.printStackTrace();

        } catch (SQLException e) {
            System.out.println("[ERROR] Database Connection Failed!");
            e.printStackTrace();
        }

        return con;
    }
}
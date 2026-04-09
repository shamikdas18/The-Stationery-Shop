import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // ---------------------------------------------------------------
    //  Configuration — update these before running
    // ---------------------------------------------------------------
    private static final String URL      = "jdbc:mysql://localhost:3306/stationery_db?useSSL=false&serverTimezone=Asia/Kolkata";
    private static final String USER     = "root";       // <-- your MySQL username
    private static final String PASSWORD = "Rudra";   // <-- your MySQL password

    // Private constructor — this is a utility class, not meant to be instantiated
    private DBConnection() {}

    /**
     * Returns a new {@link Connection} to stationery_db.
     *
     * @return an open JDBC connection
     * @throws SQLException if the connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/chat_app";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "1234";

    // Method to establish a connection to the database
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // Method to register a new user in the database
    public boolean registerUser(String username, String password, String ip, int port) {
        try (Connection conn = getConnection()) {
            String query = "INSERT INTO users (username, password, ip, port) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.setString(3, ip);
                stmt.setInt(4, port);
                int rowsAffected = stmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isClientRegistered(String username) throws SQLException {
        try (Connection conn = getConnection()) {
            String query = "SELECT COUNT(*) FROM users WHERE username = ?";
            try (PreparedStatement statement = conn.prepareStatement(query)) {
                statement.setString(1, username);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        int count = resultSet.getInt(1);
                        return count > 0;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public int getClientPort(String username) throws SQLException {
        int port = 0;
        try (Connection conn = getConnection()) {
            String query = "SELECT port FROM users WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        port = rs.getInt("port");
                    }
                }
            }
        }
        return port;
    }

    public boolean ajouterAmi(String username1, String username2) {
        try (Connection conn = getConnection()) {
            // Vérifie d'abord si les deux utilisateurs existent dans la base de données
            if (!isClientRegistered(username1) || !isClientRegistered(username2)) {
                System.out.println("One or both users do not exist.");
                return false;
            }

            // Vérifie si les utilisateurs ne sont pas déjà amis
            if (checkIfAmi(username1, username2)) {
                System.out.println("Users are already friends.");
                return false;
            }

            // Ajoute l'amitié dans la table 'amis'
            String query = "INSERT INTO amis (USER1_id, USER2_id) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                int id1 = getClientId(username1);
                int id2 = getClientId(username2);
                stmt.setInt(1, id1);
                stmt.setInt(2, id2);
                int rowsAffected = stmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


        public String getClientIPAddress (String username){
            String ip = "";
            try (Connection conn = getConnection()) {
                String query = "SELECT ip FROM users WHERE username = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, username);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            ip = rs.getString("ip");
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return ip;
        }

    public boolean checkIfAmi(String username1, String username2) {
        try (Connection conn = getConnection()) {
            int id1 = getClientId(username1);
            int id2 = getClientId(username2);
            System.out.println(id1+','+id2);

            if (id1 == 0 || id2 == 0) {
                System.out.println("One or both users do not exist.");
                return false;
            }

            String query = "SELECT COUNT(*) FROM amis WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, id1);
                stmt.setInt(2, id2);
                stmt.setInt(3, id2);
                stmt.setInt(4, id1);
                try (ResultSet resultSet = stmt.executeQuery()) {
                    if (resultSet.next()) {
                        int count = resultSet.getInt(1);
                        return count > 0;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getClientId (String username){
         int id = 0;
        try (Connection conn = getConnection()) {
            String query = "SELECT id FROM users WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        id = rs.getInt("id");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }

    public List<String> getFriends(String username) {
        List<String> friends = new ArrayList<>();
        try (Connection conn = getConnection()) {
            // Get the user's ID
            int userId = getClientId(username);

            // Ensure that the user exists
            if (userId == 0) {
                System.out.println("User does not exist.");
                return friends;
            }

            // Query to retrieve friends of the user
            String query = "SELECT u.username " +
                    "FROM users u " +
                    "JOIN amis a ON (u.id = a.USER2_id OR u.id = a.USER1_id) " +
                    "WHERE (a.USER1_id = ? OR a.USER2_id = ?) AND u.username != ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, userId);
                stmt.setString(3, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String friendUsername = rs.getString("username");
                        friends.add(friendUsername);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return friends;
    }
}

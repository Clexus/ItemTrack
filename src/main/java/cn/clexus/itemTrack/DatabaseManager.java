package cn.clexus.itemTrack;

import org.bukkit.entity.Player;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class DatabaseManager {
    private final Connection connection;

    public List<String> getItemPlayerHistory(String itemUUID) throws SQLException {
        List<String> playerHistory = new ArrayList<>();
        String sql = "SELECT player_uuids, tracking_times, display_name FROM items WHERE uuid = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, itemUUID);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String playerUUIDs = resultSet.getString("player_uuids");
                String trackingTimes = resultSet.getString("tracking_times");
                String displayName = resultSet.getString("display_name");

                String[] uuidArray = playerUUIDs.split(",");
                String[] timeArray = trackingTimes.split(",");
                for (int i = 0; i < uuidArray.length; i++) {
                    String playerUUID = uuidArray[i];
                    String trackingTime = timeArray[i];
                    playerHistory.add("玩家UUID: " + playerUUID + " | 记录时间: " + trackingTime);
                }

                playerHistory.add(0, "物品UUID: " + itemUUID + " | 物品显示名: " + displayName);
            }
        }
        return playerHistory;
    }


    public DatabaseManager(String path) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS items (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "display_name TEXT," +
                    "tracking_times TEXT," +
                    "player_uuids TEXT NOT NULL)");
        }
    }

    public void createItemWithPlayer(String itemUUID, Player player, String displayName) throws SQLException {
        String currentTime = java.time.LocalDateTime.now().toString(); // 格式: 2024-11-14T14:31:31
        String sql = "INSERT INTO items (uuid, display_name, player_uuids, tracking_times) VALUES (?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, itemUUID);
            preparedStatement.setString(2, displayName);
            preparedStatement.setString(3, player.getUniqueId().toString());
            preparedStatement.setString(4, currentTime);
            preparedStatement.executeUpdate();
        }
    }


    public void addPlayerToItem(String itemUUID, Player player) throws SQLException {
        String currentTime = java.time.LocalDateTime.now().toString(); // 当前时间
        String sql = "UPDATE items SET player_uuids = player_uuids || ?, tracking_times = tracking_times || ? WHERE uuid = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, "," + player.getUniqueId().toString());
            preparedStatement.setString(2, "," + currentTime);
            preparedStatement.setString(3, itemUUID);
            preparedStatement.executeUpdate();
        }
    }

    public void removeItem(String itemUUID) throws SQLException {
        String sql = "DELETE FROM items WHERE uuid = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, itemUUID);
        }
    }
    public boolean itemExists(String itemUUID) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT * FROM items WHERE item_uuid = ?")) {
            preparedStatement.setString(1, itemUUID);
            return preparedStatement.executeQuery().next();
        }
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}

package dolozimm.lunaguerra.database;

import dolozimm.lunaguerra.LunaGuerraPlugin;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
    
    private final LunaGuerraPlugin plugin;
    private Connection connection;
    
    public DatabaseManager(LunaGuerraPlugin plugin) {
        this.plugin = plugin;
        initDatabase();
    }
    
    private void initDatabase() {
        try {
            String dbPath = plugin.getDataFolder() + "/lunaguerra.db";
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
        }
    }
    
    private void createTables() throws SQLException {
        String clanWins = "CREATE TABLE IF NOT EXISTS clan_wins (" +
                "clan_tag TEXT PRIMARY KEY," +
                "wins INTEGER DEFAULT 0" +
                ")";
        
        String playerKills = "CREATE TABLE IF NOT EXISTS player_kills (" +
                "player_name TEXT PRIMARY KEY," +
                "kills INTEGER DEFAULT 0" +
                ")";
        
        String clanBans = "CREATE TABLE IF NOT EXISTS clan_bans (" +
                "clan_tag TEXT PRIMARY KEY," +
                "banned_by TEXT NOT NULL," +
                "ban_date TEXT NOT NULL," +
                "reason TEXT" +
                ")";
        
        String playerBans = "CREATE TABLE IF NOT EXISTS player_bans (" +
                "player_name TEXT PRIMARY KEY," +
                "banned_by TEXT NOT NULL," +
                "ban_date TEXT NOT NULL," +
                "reason TEXT" +
                ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(clanWins);
            stmt.execute(playerKills);
            stmt.execute(clanBans);
            stmt.execute(playerBans);
        }
    }
    
    public void addClanWin(String clanTag) {
        if (isClanBanned(clanTag)) return;
        
        String sql = "INSERT OR REPLACE INTO clan_wins (clan_tag, wins) " +
                "VALUES (?, COALESCE((SELECT wins FROM clan_wins WHERE clan_tag = ?), 0) + 1)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, clanTag);
            stmt.setString(2, clanTag);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to add clan win: " + e.getMessage());
        }
    }
    
    public void addPlayerKill(String playerName) {
        if (isPlayerBanned(playerName)) return;
        
        String sql = "INSERT OR REPLACE INTO player_kills (player_name, kills) " +
                "VALUES (?, COALESCE((SELECT kills FROM player_kills WHERE player_name = ?), 0) + 1)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerName);
            stmt.setString(2, playerName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to add player kill: " + e.getMessage());
        }
    }
    
    public List<Map<String, Object>> getClanTop(int limit) {
        List<Map<String, Object>> top = new ArrayList<>();
        String sql = "SELECT clan_tag, wins FROM clan_wins WHERE clan_tag NOT IN " +
                "(SELECT clan_tag FROM clan_bans) ORDER BY wins DESC LIMIT ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("clan_tag", rs.getString("clan_tag"));
                entry.put("wins", rs.getInt("wins"));
                top.add(entry);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get clan top: " + e.getMessage());
        }
        
        return top;
    }
    
    public List<Map<String, Object>> getPlayerTop(int limit) {
        List<Map<String, Object>> top = new ArrayList<>();
        String sql = "SELECT player_name, kills FROM player_kills WHERE player_name NOT IN " +
                "(SELECT player_name FROM player_bans) ORDER BY kills DESC LIMIT ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("player_name", rs.getString("player_name"));
                entry.put("kills", rs.getInt("kills"));
                top.add(entry);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get player top: " + e.getMessage());
        }
        
        return top;
    }
    
    public void banClan(String clanTag, String bannedBy, String reason) {
        String sql = "INSERT OR REPLACE INTO clan_bans (clan_tag, banned_by, ban_date, reason) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, clanTag);
            stmt.setString(2, bannedBy);
            stmt.setString(3, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            stmt.setString(4, reason);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to ban clan: " + e.getMessage());
        }
    }
    
    public void banPlayer(String playerName, String bannedBy, String reason) {
        String sql = "INSERT OR REPLACE INTO player_bans (player_name, banned_by, ban_date, reason) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerName);
            stmt.setString(2, bannedBy);
            stmt.setString(3, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            stmt.setString(4, reason);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to ban player: " + e.getMessage());
        }
    }
    
    public boolean isClanBanned(String clanTag) {
        String sql = "SELECT 1 FROM clan_bans WHERE clan_tag = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, clanTag);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check clan ban: " + e.getMessage());
            return false;
        }
    }
    
    public boolean isPlayerBanned(String playerName) {
        String sql = "SELECT 1 FROM player_bans WHERE player_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerName);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check player ban: " + e.getMessage());
            return false;
        }
    }
    
    public Map<String, String> getBanInfo(String target, boolean isClan) {
        String table = isClan ? "clan_bans" : "player_bans";
        String column = isClan ? "clan_tag" : "player_name";
        String sql = "SELECT banned_by, ban_date, reason FROM " + table + " WHERE " + column + " = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, target);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Map<String, String> info = new HashMap<>();
                info.put("banned_by", rs.getString("banned_by"));
                info.put("ban_date", rs.getString("ban_date"));
                info.put("reason", rs.getString("reason"));
                return info;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get ban info: " + e.getMessage());
        }
        
        return null;
    }
    
    public void unbanClan(String clanTag) {
        String sql = "DELETE FROM clan_bans WHERE clan_tag = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, clanTag);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to unban clan: " + e.getMessage());
        }
    }
    
    public void unbanPlayer(String playerName) {
        String sql = "DELETE FROM player_bans WHERE player_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to unban player: " + e.getMessage());
        }
    }
    
    public List<Map<String, Object>> getClanTopFormatted(int limit) {
        List<Map<String, Object>> top = new ArrayList<>();
        String sql = "SELECT clan_tag, wins FROM clan_wins WHERE clan_tag NOT IN " +
                "(SELECT clan_tag FROM clan_bans) ORDER BY wins DESC LIMIT ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> entry = new HashMap<>();
                String cleanTag = rs.getString("clan_tag");
                String formattedTag = getFormattedClanTag(cleanTag);
                entry.put("clan_tag", formattedTag != null ? formattedTag : cleanTag);
                entry.put("wins", rs.getInt("wins"));
                top.add(entry);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get clan top: " + e.getMessage());
        }
        
        return top;
    }
    
    private String getFormattedClanTag(String cleanTag) {
        try {
            net.sacredlabyrinth.phaed.simpleclans.SimpleClans sc = net.sacredlabyrinth.phaed.simpleclans.SimpleClans.getInstance();
            if (sc != null) {
                return sc.getClanManager().getClans().stream()
                    .filter(clan -> clan.getTag().equals(cleanTag))
                    .map(clan -> clan.getColorTag())
                    .findFirst()
                    .orElse(cleanTag);
            }
        } catch (Exception e) {
        }
        return cleanTag;
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to close database: " + e.getMessage());
        }
    }
}

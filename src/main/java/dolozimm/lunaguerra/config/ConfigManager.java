package dolozimm.lunaguerra.config;

import dolozimm.lunaguerra.LunaGuerraPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final LunaGuerraPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File configFile;
    private File messagesFile;

    public ConfigManager(LunaGuerraPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        createFiles();
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        try {
            validateConfig();
            setDefaults();
            saveConfigs();
        } catch (Exception e) {
            plugin.getLogger().severe("Configuration error: " + e.getMessage());
            throw new RuntimeException("Configuration error: " + e.getMessage());
        }
    }

    private void createFiles() {
        configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        // Save both language files if they don't exist
        if (!new File(plugin.getDataFolder(), "messages_ptBR.yml").exists()) {
            plugin.saveResource("messages_ptBR.yml", false);
        }
        if (!new File(plugin.getDataFolder(), "messages_en.yml").exists()) {
            plugin.saveResource("messages_en.yml", false);
        }

        // Load the correct messages file based on config
        config = YamlConfiguration.loadConfiguration(configFile);
        String messagesFile = config.getString("general.messages", "messages_ptBR.yml");
        this.messagesFile = new File(plugin.getDataFolder(), messagesFile);
        
        if (!this.messagesFile.exists()) {
            plugin.getLogger().warning("Selected messages file " + messagesFile + " not found! Defaulting to messages_ptBR.yml");
            this.messagesFile = new File(plugin.getDataFolder(), "messages_ptBR.yml");
        }
    }

    private void validateConfig() {
        if (!config.contains("general")) {
            throw new RuntimeException("Missing 'general' section in config.yml");
        }
        if (!config.contains("general.messages")) {
            throw new RuntimeException("Missing 'general.messages' setting in config.yml");
        }
        if (!config.contains("custom_messages")) {
            throw new RuntimeException("Missing 'custom_messages' section in config.yml");
        }
        if (!config.contains("arenas")) {
            throw new RuntimeException("Missing 'arenas' section in config.yml");
        }
        if (!config.contains("winners")) {
            throw new RuntimeException("Missing 'winners' section in config.yml");
        }

        if (!config.isInt("general.default_players_per_clan")) {
            throw new RuntimeException("Invalid or missing 'general.default_players_per_clan' setting");
        }
        if (!config.isInt("general.default_wait_seconds")) {
            throw new RuntimeException("Invalid or missing 'general.default_wait_seconds' setting");
        }
        if (!config.isInt("general.winners_history_limit")) {
            throw new RuntimeException("Invalid or missing 'general.winners_history_limit' setting");
        }
        if (config.isConfigurationSection("arenas")) {
            for (String arenaId : config.getConfigurationSection("arenas").getKeys(false)) {
                String arenaPath = "arenas." + arenaId;
                validateArenaConfig(arenaPath);
            }
        }
    }

    private void validateArenaConfig(String arenaPath) {
        if (!config.contains(arenaPath + ".display-name")) {
            throw new RuntimeException("Missing display-name for arena at " + arenaPath);
        }
        if (config.contains(arenaPath + ".players_per_clan") && !config.isInt(arenaPath + ".players_per_clan")) {
            throw new RuntimeException("Invalid players_per_clan value for arena at " + arenaPath);
        }
        if (config.contains(arenaPath + ".wait_seconds") && !config.isInt(arenaPath + ".wait_seconds")) {
            throw new RuntimeException("Invalid wait_seconds value for arena at " + arenaPath);
        }
    }

    public boolean setArenaDisplayName(String arenaId, String displayName) {
        try {
            config.set("arenas." + arenaId + ".display-name", displayName);
            config.save(configFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Error saving arena display name: " + e.getMessage());
            return false;
        }
    }

    public boolean setArenaPlayerLimit(String arenaId, int limit) {
        try {
            config.set("arenas." + arenaId + ".players_per_clan", limit);
            config.save(configFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Error saving arena player limit: " + e.getMessage());
            return false;
        }
    }



    private void setDefaults() {
        if (!config.contains("general.default_players_per_clan")) {
            config.set("general.default_players_per_clan", 5);
        }
        if (!config.contains("general.default_wait_seconds")) {
            config.set("general.default_wait_seconds", 180);
        }
        if (!config.contains("general.winners_history_limit")) {
            config.set("general.winners_history_limit", 20);
        }
        if (!messages.contains("guerra.started")) {
            messages.set("guerra.started", "&aGuerra iniciada na arena %arena%! Use /guerra entrar %arena% para participar.");
        }
        if (!messages.contains("guerra.countdown")) {
            messages.set("guerra.countdown", "&eIniciando guerra em %time% segundos...");
        }
        if (!messages.contains("guerra.joined")) {
            messages.set("guerra.joined", "&aVocê entrou na guerra da arena %arena%!");
        }
        if (!messages.contains("guerra.left")) {
            messages.set("guerra.left", "&cVocê saiu da guerra!");
        }
        if (!messages.contains("guerra.full_clan")) {
            messages.set("guerra.full_clan", "&cSeu clan já atingiu o limite de jogadores nesta guerra!");
        }
        if (!messages.contains("guerra.only_clan_members")) {
            messages.set("guerra.only_clan_members", "&cApenas membros de clans podem participar de guerras!");
        }
        if (!messages.contains("guerra.spectate")) {
            messages.set("guerra.spectate", "&aTeleportado para o camarote!");
        }
        if (!messages.contains("guerra.not_found_arena")) {
            messages.set("guerra.not_found_arena", "&cArena não encontrada!");
        }
        if (!messages.contains("guerra.report_title")) {
            messages.set("guerra.report_title", "&6&l=== Relatório da Guerra - %arena% ===");
        }
        if (!messages.contains("guerra.report_line_kills")) {
            messages.set("guerra.report_line_kills", "&e%position%º lugar - %clan% - %kills% kills");
        }
        if (!messages.contains("guerra.report_line_topkiller")) {
            messages.set("guerra.report_line_topkiller", "&7Top killer do clan: %player% (%kills% kills)");
        }
        if (!messages.contains("guerra.prize_executed")) {
            messages.set("guerra.prize_executed", "&aPrêmio entregue para: %player%");
        }

        saveConfigs();
    }

    public void saveConfigs() {
        try {
            config.save(configFile);
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving configurations: " + e.getMessage());
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getMessage(String key, Map<String, String> replacements) {
        String message = messages.getString(key, "&cMessage not found: " + key);

        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&', getMessage(key, null));
    }

    public void sendMessage(Player player, String messageKey, Map<String, String> replacements) {
        String message = getMessage(messageKey, replacements);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public void broadcastMessage(String messageKey, Map<String, String> replacements) {
        String message = getMessage(messageKey, replacements);
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        plugin.getServer().broadcastMessage(coloredMessage);
    }

    public void addWinner(String clanTag, String playerName) {
        List<Map<?, ?>> winners = config.getMapList("winners");

        Map<String, Object> winner = new HashMap<>();
        winner.put("date", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        winner.put("clan_tag", clanTag);
        winner.put("player_given", playerName);

        winners.add(0, winner);
        int limit = config.getInt("general.winners_history_limit", 20);
        if (winners.size() > limit) {
            winners = winners.subList(0, limit);
        }

        config.set("winners", winners);
        saveConfigs();
    }

    public List<Map<?, ?>> getWinners(int amount) {
        List<Map<?, ?>> winners = config.getMapList("winners");
        return winners.size() > amount ? winners.subList(0, amount) : winners;
    }
}
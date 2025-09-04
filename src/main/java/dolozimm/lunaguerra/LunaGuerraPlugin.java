package dolozimm.lunaguerra;

import dolozimm.lunaguerra.arena.ArenaManager;
import dolozimm.lunaguerra.commands.GuerraCommand;
import dolozimm.lunaguerra.config.ConfigManager;
import dolozimm.lunaguerra.database.DatabaseManager;
import dolozimm.lunaguerra.discord.DiscordWebhook;
import dolozimm.lunaguerra.kit.KitManager;
import dolozimm.lunaguerra.listeners.PlayerDamageListener;
import dolozimm.lunaguerra.listeners.PlayerDeathListener;
import dolozimm.lunaguerra.listeners.PlayerQuitListener;
import dolozimm.lunaguerra.placeholder.LunaGuerraExpansion;
import dolozimm.lunaguerra.simpleclans.SimpleClansBridge;
import org.bukkit.plugin.java.JavaPlugin;

public class LunaGuerraPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private ArenaManager arenaManager;
    private SimpleClansBridge clansBridge;
    private KitManager kitManager;
    private DatabaseManager databaseManager;
    private DiscordWebhook discordWebhook;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("SimpleClans") == null) {
            getLogger().severe("SimpleClans não encontrado! Plugin desabilitado.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.configManager = new ConfigManager(this);
        this.clansBridge = new SimpleClansBridge();
        this.kitManager = new KitManager(this);
        this.databaseManager = new DatabaseManager(this);
        
        configManager.loadConfigs();
        
        String webhookUrl = configManager.getConfig().getString("discord.webhook_url", "");
        if (webhookUrl != null) {
            webhookUrl = webhookUrl.replaceAll("^['\"]|['\"]$", "").trim();
        }
        
        getServer().getScheduler().runTaskAsynchronously(this, this::createWebhookTemplates);
        
        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            this.discordWebhook = new DiscordWebhook(this, webhookUrl);
            getLogger().info("Discord webhook enabled with URL configured");
        } else {
            getLogger().info("Discord webhook disabled - no URL configured");
        }
        
        this.arenaManager = new ArenaManager(this, configManager, clansBridge, databaseManager, discordWebhook);
        arenaManager.loadArenas();

        getCommand("guerra").setExecutor(new GuerraCommand(this, arenaManager, configManager, kitManager, databaseManager));

        getServer().getPluginManager().registerEvents(new PlayerDeathListener(arenaManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(arenaManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDamageListener(arenaManager, clansBridge), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new LunaGuerraExpansion(this, databaseManager).register();
            getLogger().info("PlaceholderAPI hook enabled!");
        }

        getLogger().info("LunaGuerra habilitado com sucesso!");
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) {
            arenaManager.stopAllWars();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("LunaGuerra desabilitado!");
    }
    
    public KitManager getKitManager() {
        return kitManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public SimpleClansBridge getClansBridge() {
        return clansBridge;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }
    
    public void reloadDiscordWebhook() {
        String webhookUrl = configManager.getConfig().getString("discord.webhook_url", "");
        if (webhookUrl != null) {
            webhookUrl = webhookUrl.replaceAll("^['\"]|['\"]$", "").trim();
        }
        
        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            this.discordWebhook = new DiscordWebhook(this, webhookUrl);
            getLogger().info("Discord webhook reloaded and enabled with URL configured");
        } else {
            this.discordWebhook = null;
            getLogger().info("Discord webhook reloaded - disabled (no URL configured)");
        }
        
        if (arenaManager != null) {
            arenaManager.updateDiscordWebhook(this.discordWebhook);
        }
    }
    
    private void createWebhookTemplates() {
        try {
            getLogger().info("Creating Discord webhook templates...");
            
            java.io.File discordFolder = new java.io.File(getDataFolder(), "discord");
            if (!discordFolder.exists()) {
                boolean created = discordFolder.mkdirs();
                if (created) {
                    getLogger().info("Created discord folder at: " + discordFolder.getAbsolutePath());
                } else {
                    getLogger().warning("Failed to create discord folder!");
                    return;
                }
            }
            
            java.io.File warStartedTemplate = new java.io.File(discordFolder, "war_started.json");
            java.io.File warWinnerTemplate = new java.io.File(discordFolder, "war_winner.json");
            
            if (!warStartedTemplate.exists()) {
                getLogger().info("Creating war_started.json template...");
                createTemplateFile(warStartedTemplate, "war_started");
            } else {
                getLogger().info("war_started.json template already exists");
            }
            
            if (!warWinnerTemplate.exists()) {
                getLogger().info("Creating war_winner.json template...");
                createTemplateFile(warWinnerTemplate, "war_winner");
            } else {
                getLogger().info("war_winner.json template already exists");
            }
            
            getLogger().info("Discord webhook template creation completed!");
        } catch (Exception e) {
            getLogger().severe("Failed to create webhook templates: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createTemplateFile(java.io.File templateFile, String templateName) {
        try {
            String defaultContent;
            if (templateName.equals("war_started")) {
                defaultContent = 
                    "{\n" +
                    "  \"embeds\": [\n" +
                    "    {\n" +
                    "      \"title\": \"⚔️ Guerra Iniciada!\",\n" +
                    "      \"description\": \"Uma nova guerra foi iniciada na arena **{event}**!\",\n" +
                    "      \"color\": 16776960,\n" +
                    "      \"timestamp\": \"{timestamp}\",\n" +
                    "      \"thumbnail\": {\n" +
                    "        \"url\": \"https://i.imgur.com/Gsc4XJw.png\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            } else {
                defaultContent = 
                    "{\n" +
                    "  \"embeds\": [\n" +
                    "    {\n" +
                    "      \"title\": \"🏆 Guerra Finalizada!\",\n" +
                    "      \"description\": \"A guerra na arena **{event}** foi vencida pelo clan **{winner_name}**!\",\n" +
                    "      \"color\": 65280,\n" +
                    "      \"fields\": [\n" +
                    "        {\n" +
                    "          \"name\": \"🥇 Clan Vencedor\",\n" +
                    "          \"value\": \"{winner_name}\",\n" +
                    "          \"inline\": true\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"name\": \"⚔️ Total de Kills\",\n" +
                    "          \"value\": \"{winner_value}\",\n" +
                    "          \"inline\": true\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"name\": \"📊 Top Clans\",\n" +
                    "          \"value\": \"{top}\"\n" +
                    "        }\n" +
                    "      ],\n" +
                    "      \"timestamp\": \"{timestamp}\",\n" +
                    "      \"thumbnail\": {\n" +
                    "        \"url\": \"https://i.imgur.com/Gsc4XJw.png\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            }
            
            try (java.io.FileWriter writer = new java.io.FileWriter(templateFile)) {
                writer.write(defaultContent);
                getLogger().info("Successfully created template file: " + templateFile.getName());
            }
        } catch (Exception e) {
            getLogger().severe("Failed to create template file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

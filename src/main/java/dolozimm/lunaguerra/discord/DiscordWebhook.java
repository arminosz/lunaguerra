package dolozimm.lunaguerra.discord;

import dolozimm.lunaguerra.LunaGuerraPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DiscordWebhook {
    
    private final LunaGuerraPlugin plugin;
    private final String webhookUrl;
    
    public DiscordWebhook(LunaGuerraPlugin plugin, String webhookUrl) {
        this.plugin = plugin;
        this.webhookUrl = webhookUrl;
        String maskedUrl = webhookUrl.contains("/api/webhooks/") ? 
            "https://discord.com/api/webhooks/***/***(hidden)" : webhookUrl;
        plugin.getLogger().info("Discord webhook initialized with URL: " + maskedUrl);
    }
    
    public void sendWarStarted(String arenaName) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            plugin.getLogger().info("Discord webhook not configured - skipping war started notification");
            return;
        }
        
        plugin.getLogger().info("[Discord] Sending war started notification for arena: " + arenaName);
        
        Map<String, String> replacements = Map.of(
            "{event}", arenaName,
            "{timestamp}", java.time.Instant.now().toString()
        );
        
        sendMessage("war_started", replacements);
    }
    
    public void sendWarWinner(String arenaName, String winnerClan, int totalKills, String topList) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            plugin.getLogger().info("Discord webhook not configured - skipping war winner notification");
            return;
        }
        
        plugin.getLogger().info("[Discord] Sending war winner notification for arena: " + arenaName + ", winner: " + winnerClan);
        
        String winnerClanName = getClanName(winnerClan);
        
        Map<String, String> replacements = Map.of(
            "{event}", arenaName,
            "{winner_name}", winnerClanName,
            "{winner_value}", String.valueOf(totalKills),
            "{top}", topList,
            "{timestamp}", java.time.Instant.now().toString()
        );
        
        sendMessage("war_winner", replacements);
    }
    
    private String getClanName(String clanTag) {
        try {
            net.sacredlabyrinth.phaed.simpleclans.SimpleClans sc = net.sacredlabyrinth.phaed.simpleclans.SimpleClans.getInstance();
            if (sc != null) {
                return sc.getClanManager().getClans().stream()
                    .filter(clan -> clan.getTag().equals(clanTag.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "")))
                    .map(clan -> clan.getName())
                    .findFirst()
                    .orElse(clanTag);
            }
        } catch (Exception e) {
        }
        return clanTag;
    }
    
    public void createDefaultTemplates() {
        plugin.getLogger().info("Creating Discord webhook templates...");
        
        File discordFolder = new File(plugin.getDataFolder(), "discord");
        if (!discordFolder.exists()) {
            boolean created = discordFolder.mkdirs();
            if (created) {
                plugin.getLogger().info("Created discord folder at: " + discordFolder.getAbsolutePath());
            } else {
                plugin.getLogger().warning("Failed to create discord folder!");
                return;
            }
        }
        
        File warStartedTemplate = new File(discordFolder, "war_started.json");
        File warWinnerTemplate = new File(discordFolder, "war_winner.json");
        
        if (!warStartedTemplate.exists()) {
            plugin.getLogger().info("Creating war_started.json template...");
            createDefaultTemplate(warStartedTemplate, "war_started");
        } else {
            plugin.getLogger().info("war_started.json template already exists");
        }
        
        if (!warWinnerTemplate.exists()) {
            plugin.getLogger().info("Creating war_winner.json template...");
            createDefaultTemplate(warWinnerTemplate, "war_winner");
        } else {
            plugin.getLogger().info("war_winner.json template already exists");
        }
        
        plugin.getLogger().info("Discord webhook template creation completed!");
    }
    
    private void sendMessage(String templateName, Map<String, String> replacements) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    plugin.getLogger().info("Loading template: " + templateName);
                    String template = loadTemplate(templateName);
                    if (template == null || template.trim().isEmpty()) {
                        plugin.getLogger().warning("Template " + templateName + " is null or empty!");
                        return;
                    }
                    
                    plugin.getLogger().info("Processing replacements for template: " + templateName);
                    for (Map.Entry<String, String> entry : replacements.entrySet()) {
                        template = template.replace(entry.getKey(), entry.getValue());
                    }
                    
                    plugin.getLogger().info("Sending webhook to Discord...");
                    URL url = new URL(webhookUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("User-Agent", "LunaGuerra-Plugin/1.0");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    
                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = template.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }
                    
                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200 || responseCode == 204) {
                        plugin.getLogger().info("Discord webhook sent successfully! Response code: " + responseCode);
                    } else {
                        plugin.getLogger().warning("Discord webhook failed with response code: " + responseCode);
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                                responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream()))) {
                            String line;
                            StringBuilder response = new StringBuilder();
                            while ((line = reader.readLine()) != null) {
                                response.append(line);
                            }
                            plugin.getLogger().warning("Discord response: " + response.toString());
                        }
                    }
                    
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to send Discord webhook: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }
    
    private String loadTemplate(String templateName) {
        File discordFolder = new File(plugin.getDataFolder(), "discord");
        if (!discordFolder.exists()) {
            discordFolder.mkdirs();
        }
        
        File templateFile = new File(discordFolder, templateName + ".json");
        
        if (!templateFile.exists()) {
            createDefaultTemplate(templateFile, templateName);
        }
        
        try {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(templateFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }
            return content.toString();
        } catch (IOException e) {
            plugin.getLogger().warning("Erro lendo mensagem de discord: " + e.getMessage());
            return null;
        }
    }
    
    private void createDefaultTemplate(File templateFile, String templateName) {
        try {
            templateFile.getParentFile().mkdirs();
            
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
            
            try (FileWriter writer = new FileWriter(templateFile)) {
                writer.write(defaultContent);
                plugin.getLogger().info("Successfully created template file: " + templateFile.getName());
            } catch (IOException e) {
                plugin.getLogger().warning("Erro ao criar template discord: " + e.getMessage());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro criando arquivo template: " + e.getMessage());
        }
    }
}
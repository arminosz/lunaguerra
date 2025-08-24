package dolozimm.lunaguerra.arena;

import dolozimm.lunaguerra.LunaGuerraPlugin;
import dolozimm.lunaguerra.config.ConfigManager;
import dolozimm.lunaguerra.kit.KitManager;
import dolozimm.lunaguerra.simpleclans.SimpleClansBridge;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ArenaManager {

    private final LunaGuerraPlugin plugin;
    private final ConfigManager configManager;
    private final SimpleClansBridge clansBridge;
    private final Map<String, Arena> arenas;

    private final KitManager kitManager;

    public ArenaManager(LunaGuerraPlugin plugin, ConfigManager configManager, SimpleClansBridge clansBridge) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.clansBridge = clansBridge;
        this.kitManager = plugin.getKitManager();
        this.arenas = new HashMap<>();
    }

    public void loadArenas() {
        arenas.clear();
        ConfigurationSection arenasSection = configManager.getConfig().getConfigurationSection("arenas");

        if (arenasSection == null) {
            plugin.getLogger().info("Nenhuma arena configurada.");
            return;
        }

        for (String arenaId : arenasSection.getKeys(false)) {
            Arena arena = new Arena(arenaId, kitManager);
            ConfigurationSection arenaSection = arenasSection.getConfigurationSection(arenaId);

            if (arenaSection == null) continue;

            arena.setDisplayName(arenaSection.getString("display-name", arenaId));
            arena.setKitManager(kitManager);
            arena.setPlayersPerClan(arenaSection.getInt("players_per_clan",
                    configManager.getConfig().getInt("general.default_players_per_clan", 5)));
            arena.setWaitSeconds(arenaSection.getInt("wait_seconds",
                    configManager.getConfig().getInt("general.default_wait_seconds", 180)));

            arena.setPrizeCommands(arenaSection.getStringList("prize_commands"));
            arena.setCamarote(parseLocation(arenaSection.getString("camarote")));
            arena.setEspera(parseLocation(arenaSection.getString("espera")));
            arena.setInicio(parseLocation(arenaSection.getString("inicio")));
            arena.setSaida(parseLocation(arenaSection.getString("saida")));

            arenas.put(arenaId, arena);
            plugin.getLogger().info("Arena carregada: " + arenaId);
        }
    }

    private Location parseLocation(String locationString) {
        if (locationString == null || locationString.isEmpty()) {
            return null;
        }

        try {
            String[] parts = locationString.split(";");
            if (parts.length < 4) return null;

            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return null;

            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);

            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0;

            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao converter localização: " + locationString);
            return null;
        }
    }

    private String locationToString(Location location) {
        if (location == null) return "";
        return String.format("%s;%.1f;%.1f;%.1f;%.1f;%.1f",
                location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    private void executeCustomCommands(String path, Map<String, String> replacements) {
        if (configManager.getConfig().getBoolean(path + ".enabled", false)) {
            List<String> commands = configManager.getConfig().getStringList(path + ".commands");
            for (String command : commands) {
                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    command = command.replace("%" + entry.getKey() + "%", entry.getValue());
                }
                command = ChatColor.translateAlternateColorCodes('&', command);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }

    public boolean startWar(String arenaId) {
        Arena arena = arenas.get(arenaId);
        if (arena == null || !arena.isConfigured() || arena.getState() != Arena.State.IDLE) {
            return false;
        }

        arena.startPreparation();

        Map<String, String> replacements = new HashMap<>();
        replacements.put("arena", arena.getDisplayName());

        if (!configManager.getConfig().getBoolean("custom_messages.custom_guerrastart.enabled", false)) {
            Bukkit.broadcastMessage(configManager.getMessage("guerra.started", replacements));
        } else {
            executeCustomCommands("custom_messages.custom_guerrastart", replacements);
        }

        startCountdown(arena);
        return true;
    }

    private void startCountdown(Arena arena) {
        BukkitRunnable countdown = new BukkitRunnable() {
            int timeLeft = arena.getWaitSeconds();

            @Override
            public void run() {
                if (arena.getState() != Arena.State.PREPARATION) {
                    cancel();
                    return;
                }

                if (timeLeft <= 0) {
                    cancel();
                    forceStartWar(arena.getId());
                    return;
                }

                // Formatação do tempo para minutos e segundos
                int minutes = timeLeft / 60;
                int seconds = timeLeft % 60;
                String timeFormat = String.format("%02d:%02d", minutes, seconds);

                // Atualizar action bar para todos os participantes
                Map<String, String> replacements = new HashMap<>();
                replacements.put("time", timeFormat);
                String message = configManager.getMessage("guerra.countdown", replacements);

                for (Set<Player> clanPlayers : arena.getParticipants().values()) {
                    for (Player player : clanPlayers) {
                        if (player.isOnline()) {
                            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
                        }
                    }
                }

                // Mensagens especiais em momentos importantes
                if (timeLeft <= 5 || (timeLeft <= 30 && timeLeft % 10 == 0) || (timeLeft <= 60 && timeLeft % 30 == 0)) {
                    String chatMessage = configManager.getMessage("guerra.countdown_chat", replacements);
                    for (Set<Player> clanPlayers : arena.getParticipants().values()) {
                        for (Player player : clanPlayers) {
                            if (player.isOnline()) {
                                player.sendMessage(chatMessage);
                            }
                        }
                    }
                }

                timeLeft--;
            }
        };

        arena.setTaskId(countdown.runTaskTimer(plugin, 0, 20).getTaskId());
    }

    public boolean forceStartWar(String arenaId) {
        Arena arena = arenas.get(arenaId);
        if (arena == null || arena.getState() != Arena.State.PREPARATION) {
            return false;
        }

        if (arena.getTotalParticipants() < 2) {
            // Send not enough players message to all participants
            String message = configManager.getMessage("guerra.not_enough_players");
            for (Set<Player> clanPlayers : arena.getParticipants().values()) {
                for (Player player : clanPlayers) {
                    if (player.isOnline()) {
                        player.sendMessage(message);
                    }
                }
            }
            stopWar(arenaId);
            return false;
        }

        // Send start message to all participants
        String startMessage = configManager.getMessage("guerra.war_started");
        for (Set<Player> clanPlayers : arena.getParticipants().values()) {
            for (Player player : clanPlayers) {
                if (player.isOnline()) {
                    player.sendMessage(startMessage);
                }
            }
        }

        arena.startCombat();
        return true;
    }

    public boolean stopWar(String arenaId) {
        Arena arena = arenas.get(arenaId);
        if (arena == null || arena.getState() == Arena.State.IDLE) {
            return false;
        }

        // Broadcast cancellation message to all participants
        Map<String, String> replacements = new HashMap<>();
        String cancelMessage = configManager.getMessage("guerra.cancelled", replacements);
        for (Set<Player> clanPlayers : arena.getParticipants().values()) {
            for (Player player : clanPlayers) {
                if (player.isOnline()) {
                    player.sendMessage(cancelMessage);
                }
            }
        }

        arena.endWar();
        return true;
    }

    public void stopAllWars() {
        for (Arena arena : arenas.values()) {
            if (arena.getState() != Arena.State.IDLE) {
                arena.endWar();
            }
        }
    }

    public boolean joinWar(Player player, String arenaId) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) return false;

        if (!clansBridge.isInClan(player)) {
            Map<String, String> replacements = new HashMap<>();
            player.sendMessage(configManager.getMessage("guerra.only_clan_members", replacements));
            return false;
        }

        if (arena.getState() != Arena.State.PREPARATION) {
            return false;
        }

        String clanTag = clansBridge.getClanTag(player);
        if (!arena.addPlayer(player, clanTag)) {
            player.sendMessage(configManager.getMessage("guerra.full_clan"));
            return false;
        }

        if (arena.getEspera() != null) {
            player.teleport(arena.getEspera());
        }

        Map<String, String> replacements = new HashMap<>();
        replacements.put("arena", arena.getDisplayName());
        player.sendMessage(configManager.getMessage("guerra.joined", replacements));
        return true;
    }

    public boolean leaveWar(Player player) {
        for (Arena arena : arenas.values()) {
            if (arena.removePlayer(player)) {
                player.sendMessage(configManager.getMessage("guerra.left"));
                return true;
            }
        }
        return false;
    }

    public void handlePlayerDeath(Player player, Player killer) {
        Arena arena = getPlayerArena(player);
        if (arena == null || arena.getState() != Arena.State.COMBAT) {
            return;
        }

        if (killer != null && !clansBridge.areSameClan(player, killer)) {
            arena.addKill(killer);
        }

        arena.removePlayer(player);

        if (arena.getSaida() != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.teleport(arena.getSaida());
                }
            }, 1);
        }

        checkWarEnd(arena);
    }

    public void handlePlayerQuit(Player player) {
        Arena arena = getPlayerArena(player);
        if (arena != null) {
            arena.removePlayer(player);
            if (arena.getState() == Arena.State.COMBAT) {
                checkWarEnd(arena);
            }
        }
    }

    private void checkWarEnd(Arena arena) {
        if (arena.getAliveClansCount() <= 1) {
            endWar(arena);
        }
    }

    private void endWar(Arena arena) {
        String winnerClan = arena.getWinnerClan();
        // Send reports and end war
        sendReports(arena, winnerClan);
        arena.endWar();

        if (winnerClan != null) {
            configManager.addWinner(winnerClan, choosePlayerForPrize(arena, winnerClan).getName());
        }
    }

    private Player choosePlayerForPrize(Arena arena, String winnerClan) {
        // Priority 1: First online leader (random if multiple)
        List<Player> onlineLeaders = clansBridge.getOnlineClanLeaders(winnerClan);
        if (!onlineLeaders.isEmpty()) {
            return onlineLeaders.get(new Random().nextInt(onlineLeaders.size()));
        }

        // Priority 2: Player with most kills
        Player topKiller = arena.getTopKiller(winnerClan);
        if (topKiller != null && topKiller.isOnline()) {
            return topKiller;
        }

        // Priority 3: Any random online clan member
        List<Player> onlineMembers = new ArrayList<>();
        for (Set<Player> clanPlayers : arena.getParticipants().values()) {
            for (Player player : clanPlayers) {
                if (player.isOnline() && clansBridge.getClanTag(player).equals(winnerClan)) {
                    onlineMembers.add(player);
                }
            }
        }

        if (!onlineMembers.isEmpty()) {
            return onlineMembers.get(new Random().nextInt(onlineMembers.size()));
        }

        return null;
    }

    private void sendReports(Arena arena, String winnerClan) {
        Map<String, Integer> clanRanking = arena.getClanRanking();
        List<Map.Entry<String, Integer>> sortedRanking = clanRanking.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .collect(Collectors.toList());

        Map<String, String> globalReplacements = new HashMap<>();
        globalReplacements.put("arena", arena.getDisplayName());
        
        if (winnerClan != null) {
            globalReplacements.put("tagclanvencedor", winnerClan);
            
            // Winner announcement to everyone
            Bukkit.broadcastMessage(configManager.getMessage("guerra.winner_announce", globalReplacements));
            
            // Add prizeholder for custom end commands
            Player prizeHolder = choosePlayerForPrize(arena, winnerClan);
            if (prizeHolder != null) {
                globalReplacements.put("prizeholder", prizeHolder.getName());
                
                // Execute custom end commands if enabled
                if (configManager.getConfig().getBoolean("custom_messages.custom_guerraend.enabled", false)) {
                    executeCustomCommands("custom_messages.custom_guerraend", globalReplacements);
                }
            }
        }

        // Build a map of clan positions
        Map<String, Integer> clanPositions = new HashMap<>();
        for (int i = 0; i < sortedRanking.size(); i++) {
            clanPositions.put(sortedRanking.get(i).getKey(), i + 1);
        }

        // Send clan-specific reports to each clan's online members
        for (String clanTag : arena.getWarParticipants().keySet()) {
            // Get ALL players that participated from this clan
            Set<Player> participants = arena.getWarParticipants().get(clanTag);
            if (participants == null) continue;

            // Get online members of this clan to send the report
            List<Player> onlineMembers = clansBridge.getOnlineClanMembers(clanTag);

            // Get this clan's position
            int position = 0;
            for (int i = 0; i < sortedRanking.size(); i++) {
                if (sortedRanking.get(i).getKey().equals(clanTag)) {
                    position = i + 1;
                    break;
                }
            }

            // Send report to all online clan members
            for (Player member : onlineMembers) {

                // Report header
                member.sendMessage(configManager.getMessage("guerra.report_title", globalReplacements));

                // Show only this clan's position and kills
                Map<String, String> clanReplacements = new HashMap<>();
                clanReplacements.put("position", String.valueOf(position));
                clanReplacements.put("clan", clanTag);
                clanReplacements.put("kills", String.valueOf(clanRanking.getOrDefault(clanTag, 0)));
                member.sendMessage(configManager.getMessage("guerra.report_line_position", clanReplacements));
                member.sendMessage(configManager.getMessage("guerra.report_line_kills", clanReplacements));

                // Show this clan's top killer
                Player topKiller = arena.getTopKiller(clanTag);
                if (topKiller != null) {
                    Map<String, String> killerReplacements = new HashMap<>();
                    killerReplacements.put("player", topKiller.getName());
                    killerReplacements.put("kills", String.valueOf(arena.getKillsPerPlayer().getOrDefault(topKiller, 0)));
                    member.sendMessage(configManager.getMessage("guerra.report_line_topkiller", killerReplacements));
                }

                // Victory status
                if (winnerClan != null && winnerClan.equals(clanTag)) {
                    member.sendMessage(configManager.getMessage("guerra.report_line_prize", globalReplacements));
                } else {
                    member.sendMessage(configManager.getMessage("guerra.report_line_no_prize"));
                }
            }
        }
    }

    public Arena getPlayerArena(Player player) {
        for (Arena arena : arenas.values()) {
            for (Set<Player> clanPlayers : arena.getParticipants().values()) {
                if (clanPlayers.contains(player)) {
                    return arena;
                }
            }
        }
        return null;
    }

    public boolean teleportToSpectate(Player player, String arenaId) {
        Arena arena = arenas.get(arenaId);
        if (arena == null || arena.getCamarote() == null) {
            return false;
        }

        if (arena.getState() == Arena.State.IDLE) {
            return false;
        }

        player.teleport(arena.getCamarote());
        player.sendMessage(configManager.getMessage("guerra.spectate"));
        return true;
    }

    public boolean setLocation(Player player, String arenaId, String locationType) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) return false;

        Location loc = player.getLocation();

        switch (locationType.toLowerCase()) {
            case "camarote":
                arena.setCamarote(loc);
                break;
            case "espera":
                arena.setEspera(loc);
                break;
            case "inicio":
                arena.setInicio(loc);
                break;
            case "saida":
                arena.setSaida(loc);
                break;
            default:
                return false;
        }

        saveArenaLocation(arenaId, locationType, loc);
        return true;
    }

    private void saveArenaLocation(String arenaId, String locationType, Location location) {
        String path = "arenas." + arenaId + "." + locationType;
        configManager.getConfig().set(path, locationToString(location));
        configManager.saveConfigs();
    }

    public boolean createArena(String arenaId) {
        if (arenas.containsKey(arenaId)) {
            return false;
        }

        Arena arena = new Arena(arenaId, kitManager);
        arenas.put(arenaId, arena);

        configManager.getConfig().set("arenas." + arenaId + ".display-name", arenaId);
        configManager.getConfig().set("arenas." + arenaId + ".players_per_clan",
                configManager.getConfig().getInt("general.default_players_per_clan", 5));
        configManager.getConfig().set("arenas." + arenaId + ".wait_seconds",
                configManager.getConfig().getInt("general.default_wait_seconds", 180));
        configManager.saveConfigs();

        return true;
    }

    public boolean deleteArena(String arenaId) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) return false;

        if (arena.getState() != Arena.State.IDLE) {
            arena.endWar();
        }

        arenas.remove(arenaId);
        configManager.getConfig().set("arenas." + arenaId, null);
        configManager.saveConfigs();

        return true;
    }

    public Arena getArena(String arenaId) {
        return arenas.get(arenaId);
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    public void reloadArenas() {
        stopAllWars();
        loadArenas();
    }
}
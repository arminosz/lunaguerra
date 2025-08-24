package dolozimm.lunaguerra.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import dolozimm.lunaguerra.kit.KitManager;
import java.util.*;

public class Arena {

    public enum State {
        IDLE, PREPARATION, COMBAT
    }

    private final String id;
    private String displayName;
    private Location camarote;
    private Location espera;
    private Location inicio;
    private Location saida;
    private int playersPerClan;
    private int waitSeconds;
    private List<String> prizeCommands;
    private State state;

    private Map<String, Set<Player>> participants;
    private Map<Player, Integer> killsPerPlayer;
    private Map<String, Integer> killsPerClan;
    private KitManager kitManager;

    private int taskId = -1;
    private long warStartTime;

    public Arena(String id, KitManager kitManager) {
        this.id = id;
        this.displayName = id;
        this.playersPerClan = 5;
        this.waitSeconds = 180;
        this.prizeCommands = new ArrayList<>();
        this.state = State.IDLE;
        this.participants = new HashMap<>();
        this.killsPerPlayer = new HashMap<>();
        this.killsPerClan = new HashMap<>();
        this.kitManager = kitManager;
    }

    public boolean addPlayer(Player player, String clanTag) {
        if (state != State.PREPARATION) {
            return false;
        }

        participants.computeIfAbsent(clanTag, k -> new HashSet<>());
        Set<Player> clanPlayers = participants.get(clanTag);

        if (clanPlayers.size() >= playersPerClan) {
            return false;
        }

        removePlayer(player);

        clanPlayers.add(player);
        killsPerPlayer.put(player, 0);
        return true;
    }

    public boolean removePlayer(Player player) {
        boolean removed = false;
        for (Set<Player> clanPlayers : participants.values()) {
            if (clanPlayers.remove(player)) {
                removed = true;
                break;
            }
        }
        killsPerPlayer.remove(player);
        return removed;
    }

    public void addKill(Player killer) {
        if (killer == null || !killsPerPlayer.containsKey(killer)) {
            return;
        }

        killsPerPlayer.put(killer, killsPerPlayer.get(killer) + 1);

        for (Map.Entry<String, Set<Player>> entry : participants.entrySet()) {
            if (entry.getValue().contains(killer)) {
                String clanTag = entry.getKey();
                killsPerClan.put(clanTag, killsPerClan.getOrDefault(clanTag, 0) + 1);
                break;
            }
        }
    }

    public void startPreparation() {
        state = State.PREPARATION;
        participants.clear();
        killsPerPlayer.clear();
        killsPerClan.clear();
    }

    public void startCombat() {
        state = State.COMBAT;
        warStartTime = System.currentTimeMillis();

        if (inicio != null) {
            for (Set<Player> clanPlayers : participants.values()) {
                for (Player player : clanPlayers) {
                    if (player.isOnline()) {
                        player.teleport(inicio);
                        if (kitManager != null) {
                            kitManager.applyKitToPlayer(player, id);
                        }
                    }
                }
            }
        }
    }

    private Map<String, Set<Player>> warParticipants;

    public void endWar() {
        state = State.IDLE;
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }

        // Store all participants before clearing
        warParticipants = new HashMap<>();
        for (Map.Entry<String, Set<Player>> entry : participants.entrySet()) {
            warParticipants.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        if (saida != null) {
            for (Set<Player> clanPlayers : participants.values()) {
                for (Player player : clanPlayers) {
                    if (player.isOnline()) {
                        player.teleport(saida);
                    }
                }
            }
        }
    }

    public Map<String, Set<Player>> getWarParticipants() {
        return warParticipants != null ? warParticipants : participants;
    }

    public String getWinnerClan() {
        int alivePlayers = 0;
        String winnerClan = null;

        for (Map.Entry<String, Set<Player>> entry : participants.entrySet()) {
            int clanAlive = 0;
            for (Player player : entry.getValue()) {
                if (player.isOnline()) {
                    clanAlive++;
                }
            }

            if (clanAlive > 0) {
                alivePlayers += clanAlive;
                winnerClan = entry.getKey();
            }
        }

        return alivePlayers > 0 && getAliveClansCount() == 1 ? winnerClan : null;
    }

    public int getAliveClansCount() {
        int aliveClans = 0;
        for (Set<Player> clanPlayers : participants.values()) {
            boolean hasAlive = clanPlayers.stream().anyMatch(Player::isOnline);
            if (hasAlive) {
                aliveClans++;
            }
        }
        return aliveClans;
    }

    public Map<String, Integer> getClanRanking() {
        Map<String, Integer> ranking = new HashMap<>();

        for (String clanTag : participants.keySet()) {
            ranking.put(clanTag, killsPerClan.getOrDefault(clanTag, 0));
        }

        return ranking;
    }

    public Player getTopKiller(String clanTag) {
        Set<Player> clanPlayers = participants.get(clanTag);
        if (clanPlayers == null) return null;

        Player topKiller = null;
        int maxKills = 0;

        for (Player player : clanPlayers) {
            int kills = killsPerPlayer.getOrDefault(player, 0);
            if (kills > maxKills) {
                maxKills = kills;
                topKiller = player;
            }
        }

        return topKiller;
    }

    public int getTotalParticipants() {
        return participants.values().stream().mapToInt(Set::size).sum();
    }

    public boolean hasLocation(String type) {
        switch (type.toLowerCase()) {
            case "camarote": return camarote != null;
            case "espera": return espera != null;
            case "inicio": return inicio != null;
            case "saida": return saida != null;
            default: return false;
        }
    }

    public boolean isConfigured() {
        return camarote != null && espera != null && inicio != null && saida != null;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Location getCamarote() { return camarote; }
    public void setCamarote(Location camarote) { this.camarote = camarote; }

    public Location getEspera() { return espera; }
    public void setEspera(Location espera) { this.espera = espera; }

    public Location getInicio() { return inicio; }
    public void setInicio(Location inicio) { this.inicio = inicio; }

    public Location getSaida() { return saida; }
    public void setSaida(Location saida) { this.saida = saida; }

    public int getPlayersPerClan() { return playersPerClan; }
    public void setPlayersPerClan(int playersPerClan) { this.playersPerClan = playersPerClan; }

    public int getWaitSeconds() { return waitSeconds; }
    public void setWaitSeconds(int waitSeconds) { this.waitSeconds = waitSeconds; }

    public List<String> getPrizeCommands() { return prizeCommands; }
    public void setPrizeCommands(List<String> prizeCommands) { this.prizeCommands = prizeCommands; }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public Map<String, Set<Player>> getParticipants() { return participants; }
    public Map<Player, Integer> getKillsPerPlayer() { return killsPerPlayer; }
    public Map<String, Integer> getKillsPerClan() { return killsPerClan; }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public long getWarStartTime() { return warStartTime; }

    public void setKitManager(KitManager kitManager) { this.kitManager = kitManager; }
    public KitManager getKitManager() { return kitManager; }
}
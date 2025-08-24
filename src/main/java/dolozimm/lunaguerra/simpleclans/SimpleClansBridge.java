package dolozimm.lunaguerra.simpleclans;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SimpleClansBridge {

    private SimpleClans simpleClans;

    public SimpleClansBridge() {
        this.simpleClans = SimpleClans.getInstance();
    }

    public boolean isInClan(Player player) {
        ClanPlayer cp = simpleClans.getClanManager().getClanPlayer(player);
        return cp != null && cp.getClan() != null;
    }

    public String getClanTag(Player player) {
        ClanPlayer cp = simpleClans.getClanManager().getClanPlayer(player);
        if (cp != null && cp.getClan() != null) {
            return cp.getClan().getColorTag();
        }
        return null;
    }

    public String getClanName(Player player) {
        ClanPlayer cp = simpleClans.getClanManager().getClanPlayer(player);
        if (cp != null && cp.getClan() != null) {
            return cp.getClan().getName();
        }
        return null;
    }

    public boolean isLeader(Player player) {
        ClanPlayer cp = simpleClans.getClanManager().getClanPlayer(player);
        return cp != null && cp.isLeader();
    }

    public List<Player> getOnlineClanMembers(String clanTag) {
        String cleanTag = clanTag.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");

        return simpleClans.getClanManager().getClans().stream()
                .filter(clan -> clan.getTag().equals(cleanTag))
                .findFirst()
                .map(clan -> clan.getOnlineMembers().stream()
                        .map(cp -> cp.toPlayer())
                        .collect(Collectors.toList()))
                .orElse(new ArrayList<>());
    }

    public List<Player> getOnlineClanLeaders(String clanTag) {
        String cleanTag = clanTag.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");

        return simpleClans.getClanManager().getClans().stream()
                .filter(clan -> clan.getTag().equals(cleanTag))
                .findFirst()
                .map(clan -> clan.getLeaders().stream()
                        .filter(cp -> cp.toPlayer() != null && cp.toPlayer().isOnline())
                        .map(cp -> cp.toPlayer())
                        .collect(Collectors.toList()))
                .orElse(new ArrayList<>());
    }

    public boolean areSameClan(Player player1, Player player2) {
        ClanPlayer cp1 = simpleClans.getClanManager().getClanPlayer(player1);
        ClanPlayer cp2 = simpleClans.getClanManager().getClanPlayer(player2);

        if (cp1 == null || cp2 == null || cp1.getClan() == null || cp2.getClan() == null) {
            return false;
        }

        return cp1.getClan().equals(cp2.getClan());
    }

    public Clan getClan(Player player) {
        ClanPlayer cp = simpleClans.getClanManager().getClanPlayer(player);
        return cp != null ? cp.getClan() : null;
    }
}
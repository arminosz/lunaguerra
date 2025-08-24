package dolozimm.lunaguerra.listeners;

import dolozimm.lunaguerra.arena.Arena;
import dolozimm.lunaguerra.arena.ArenaManager;
import dolozimm.lunaguerra.simpleclans.SimpleClansBridge;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class PlayerDamageListener implements Listener {

    private final ArenaManager arenaManager;
    private final SimpleClansBridge clansBridge;

    public PlayerDamageListener(ArenaManager arenaManager, SimpleClansBridge clansBridge) {
        this.arenaManager = arenaManager;
        this.clansBridge = clansBridge;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player damaged = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();

        Arena arena = arenaManager.getPlayerArena(damaged);
        if (arena == null || arena.getState() != Arena.State.COMBAT) {
            return;
        }

        if (clansBridge.areSameClan(damaged, damager)) {
            event.setCancelled(true);
        }
    }
}

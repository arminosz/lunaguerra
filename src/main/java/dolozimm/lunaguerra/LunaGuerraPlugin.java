package dolozimm.lunaguerra;

import dolozimm.lunaguerra.arena.ArenaManager;
import dolozimm.lunaguerra.commands.GuerraCommand;
import dolozimm.lunaguerra.config.ConfigManager;
import dolozimm.lunaguerra.listeners.PlayerDamageListener;
import dolozimm.lunaguerra.listeners.PlayerDeathListener;
import dolozimm.lunaguerra.listeners.PlayerQuitListener;
import dolozimm.lunaguerra.kit.KitManager;
import dolozimm.lunaguerra.simpleclans.SimpleClansBridge;
import org.bukkit.plugin.java.JavaPlugin;

public class LunaGuerraPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private ArenaManager arenaManager;
    private SimpleClansBridge clansBridge;
    private KitManager kitManager;

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
        this.arenaManager = new ArenaManager(this, configManager, clansBridge);

        configManager.loadConfigs();
        arenaManager.loadArenas();

        getCommand("guerra").setExecutor(new GuerraCommand(this, arenaManager, configManager, kitManager));

        getServer().getPluginManager().registerEvents(new PlayerDeathListener(arenaManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(arenaManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDamageListener(arenaManager, clansBridge), this);

        getLogger().info("LunaGuerra habilitado com sucesso!");
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) {
            arenaManager.stopAllWars();
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
}
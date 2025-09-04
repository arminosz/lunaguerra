package dolozimm.lunaguerra.kit;

import dolozimm.lunaguerra.LunaGuerraPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;

public class KitManager {
    private final LunaGuerraPlugin plugin;
    private final File kitsFolder;

    public KitManager(LunaGuerraPlugin plugin) {
        this.plugin = plugin;
        this.kitsFolder = new File(plugin.getDataFolder(), "kits");
        if (!kitsFolder.exists()) {
            kitsFolder.mkdirs();
        }
    }

    public void savePlayerKitToArena(Player player, String arena) {
        File kitFile = new File(kitsFolder, "kit_" + arena + ".yml");
        FileConfiguration kitConfig = YamlConfiguration.loadConfiguration(kitFile);

        PlayerInventory inv = player.getInventory();
        
        kitConfig.set("armor.helmet", inv.getHelmet());
        kitConfig.set("armor.chestplate", inv.getChestplate());
        kitConfig.set("armor.leggings", inv.getLeggings());
        kitConfig.set("armor.boots", inv.getBoots());
        
        kitConfig.set("offhand", inv.getItemInOffHand());
        
        for (int i = 0; i < inv.getContents().length; i++) {
            if (inv.getContents()[i] != null) {
                kitConfig.set("inventory." + i, inv.getContents()[i]);
            }
        }

        try {
            kitConfig.save(kitFile);
            player.sendMessage("§aKit salvo com sucesso para a arena " + arena);
        } catch (IOException e) {
            player.sendMessage("§cErro salvando kit " + arena);
            e.printStackTrace();
        }
    }

    public void applyKitToPlayer(Player player, String arena) {
        File kitFile = new File(kitsFolder, "kit_" + arena + ".yml");
        if (!kitFile.exists()) {
            player.sendMessage("§cNenhum kit encontrado para a arena " + arena);
            return;
        }

        FileConfiguration kitConfig = YamlConfiguration.loadConfiguration(kitFile);
        PlayerInventory inv = player.getInventory();
        
        inv.clear();
        inv.setHelmet(null);
        inv.setChestplate(null);
        inv.setLeggings(null);
        inv.setBoots(null);
        inv.setItemInOffHand(null);
        
        inv.setHelmet(kitConfig.getItemStack("armor.helmet"));
        inv.setChestplate(kitConfig.getItemStack("armor.chestplate"));
        inv.setLeggings(kitConfig.getItemStack("armor.leggings"));
        inv.setBoots(kitConfig.getItemStack("armor.boots"));
        
        inv.setItemInOffHand(kitConfig.getItemStack("offhand"));
        if (kitConfig.contains("inventory")) {
            for (String key : kitConfig.getConfigurationSection("inventory").getKeys(false)) {
                int slot = Integer.parseInt(key);
                ItemStack item = kitConfig.getItemStack("inventory." + key);
                inv.setItem(slot, item);
            }
        }
    }

    public boolean hasKit(String arena) {
        File kitFile = new File(kitsFolder, "kit_" + arena + ".yml");
        return kitFile.exists();
    }
}

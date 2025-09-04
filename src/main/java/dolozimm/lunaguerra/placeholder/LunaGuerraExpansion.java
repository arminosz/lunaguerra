package dolozimm.lunaguerra.placeholder;

import dolozimm.lunaguerra.LunaGuerraPlugin;
import dolozimm.lunaguerra.database.DatabaseManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Map;

public class LunaGuerraExpansion extends PlaceholderExpansion {
    
    private final LunaGuerraPlugin plugin;
    private final DatabaseManager databaseManager;
    
    public LunaGuerraExpansion(LunaGuerraPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
    @Override
    public String getIdentifier() {
        return "lunaguerra";
    }
    
    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (params.startsWith("top_name_")) {
            try {
                int rank = Integer.parseInt(params.substring(9));
                List<Map<String, Object>> top = databaseManager.getClanTopFormatted(rank);
                if (rank > 0 && rank <= top.size()) {
                    return (String) top.get(rank - 1).get("clan_tag");
                }
            } catch (NumberFormatException e) {
                return "";
            }
        }
        
        if (params.startsWith("top_value_")) {
            try {
                int rank = Integer.parseInt(params.substring(10));
                List<Map<String, Object>> top = databaseManager.getClanTopFormatted(rank);
                if (rank > 0 && rank <= top.size()) {
                    return String.valueOf(top.get(rank - 1).get("wins"));
                }
            } catch (NumberFormatException e) {
                return "0";
            }
        }
        
        if (params.startsWith("player_top_name_")) {
            try {
                int rank = Integer.parseInt(params.substring(16));
                List<Map<String, Object>> top = databaseManager.getPlayerTop(rank);
                if (rank > 0 && rank <= top.size()) {
                    return (String) top.get(rank - 1).get("player_name");
                }
            } catch (NumberFormatException e) {
                return "";
            }
        }
        
        if (params.startsWith("player_top_value_")) {
            try {
                int rank = Integer.parseInt(params.substring(17));
                List<Map<String, Object>> top = databaseManager.getPlayerTop(rank);
                if (rank > 0 && rank <= top.size()) {
                    return String.valueOf(top.get(rank - 1).get("kills"));
                }
            } catch (NumberFormatException e) {
                return "0";
            }
        }
        
        return null;
    }
}
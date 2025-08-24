package dolozimm.lunaguerra.commands;

import dolozimm.lunaguerra.LunaGuerraPlugin;
import dolozimm.lunaguerra.arena.Arena;
import dolozimm.lunaguerra.arena.ArenaManager;
import dolozimm.lunaguerra.config.ConfigManager;
import dolozimm.lunaguerra.kit.KitManager;

import org.bukkit.inventory.Inventory;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public class GuerraCommand implements CommandExecutor, TabCompleter {

    private final LunaGuerraPlugin plugin;
    private final ArenaManager arenaManager;
    private final ConfigManager configManager;
    private final KitManager kitManager;

    public GuerraCommand(LunaGuerraPlugin plugin, ArenaManager arenaManager, ConfigManager configManager, KitManager kitManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.configManager = configManager;
        this.kitManager = kitManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "entrar":
                return handleJoin(sender, args);
            case "sair":
                return handleLeave(sender, args);
            case "camarote":
                return handleSpectate(sender, args);
            case "start":
                return handleStart(sender, args);
            case "stop":
                return handleStop(sender, args);
            case "forcestart":
                return handleForceStart(sender, args);
            case "set":
                return handleSet(sender, args);
            case "create":
                return handleCreate(sender, args);
            case "delete":
                return handleDelete(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "vencedores":
                return handleWinners(sender, args);
            case "reload":
                return handleReload(sender, args);
            case "displayname":
                return handleDisplayName(sender, args);
            case "limit":
                return handleLimit(sender, args);
            case "kit":
                return handleKit(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar este comando!");
            return true;
        }

        if (!sender.hasPermission("lunaguerra.join")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        Player player = (Player) sender;
        String arenaId;

        if (args.length < 2) {
            List<Arena> openArenas = arenaManager.getArenas().stream()
                .filter(a -> a.getState() == Arena.State.PREPARATION)
                .collect(Collectors.toList());

            if (openArenas.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Não há guerras abertas no momento!");
                return true;
            }

            Arena randomArena = openArenas.get(new Random().nextInt(openArenas.size()));
            arenaId = randomArena.getId();
        } else {
            arenaId = args[1];
        }

        if (arenaManager.joinWar(player, arenaId)) {
            return true;
        }

        Arena arena = arenaManager.getArena(arenaId);
        if (arena == null) {
            player.sendMessage(configManager.getMessage("guerra.not_found_arena"));
        } else if (arena.getState() == Arena.State.IDLE) {
            player.sendMessage(ChatColor.RED + "Não há guerra ativa nesta arena!");
        } else {
            player.sendMessage(ChatColor.RED + "Não foi possível entrar na guerra!");
        }
        return true;
    }

    private boolean handleLeave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar este comando!");
            return true;
        }

        if (!sender.hasPermission("lunaguerra.join")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        Player player = (Player) sender;
        if (!arenaManager.leaveWar(player)) {
            player.sendMessage(ChatColor.RED + "Você não está em nenhuma guerra!");
        }
        return true;
    }

    private boolean handleSpectate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar este comando!");
            return true;
        }

        if (!sender.hasPermission("lunaguerra.spectate")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        Player player = (Player) sender;

        Arena activeArena = null;
        for (Arena arena : arenaManager.getArenas()) {
            if (arena.getState() != Arena.State.IDLE) {
                activeArena = arena;
                break;
            }
        }

        if (activeArena == null) {
            player.sendMessage(ChatColor.RED + "Não há guerra ativa no momento!");
            return true;
        }

        if (!arenaManager.teleportToSpectate(player, activeArena.getId())) {
            player.sendMessage(ChatColor.RED + "Erro ao teleportar para o camarote!");
        }
        return true;
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lunaguerra.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /guerra start <arena>");
            return true;
        }

        String arenaId = args[1];
        Arena arena = arenaManager.getArena(arenaId);

        if (arena == null) {
            sender.sendMessage(configManager.getMessage("guerra.not_found_arena"));
            return true;
        }

        if (!arena.isConfigured()) {
            sender.sendMessage(ChatColor.RED + "Arena não está completamente configurada!");
            return true;
        }

        if (arenaManager.startWar(arenaId)) {
            sender.sendMessage(ChatColor.GREEN + "Guerra iniciada na arena " + arena.getDisplayName() + "!");
        } else {
            sender.sendMessage(ChatColor.RED + "Não foi possível iniciar a guerra!");
        }
        return true;
    }

    private boolean handleStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lunaguerra.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /guerra stop <arena>");
            return true;
        }

        String arenaId = args[1];
        if (arenaManager.stopWar(arenaId)) {
            sender.sendMessage(ChatColor.GREEN + "Guerra cancelada!");
        } else {
            sender.sendMessage(ChatColor.RED + "Arena não encontrada ou não há guerra ativa!");
        }
        return true;
    }

    private boolean handleForceStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lunaguerra.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /guerra forcestart <arena>");
            return true;
        }

        String arenaId = args[1];
        if (arenaManager.forceStartWar(arenaId)) {
            sender.sendMessage(ChatColor.GREEN + "Guerra iniciada imediatamente!");
        } else {
            sender.sendMessage(ChatColor.RED + "Não foi possível forçar o início da guerra!");
        }
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar este comando!");
            return true;
        }

        if (!sender.hasPermission("lunaguerra.set")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /guerra set <arena> <camarote|espera|inicio|saida>");
            return true;
        }

        Player player = (Player) sender;
        String arenaId = args[1];
        String locationType = args[2].toLowerCase();

        if (!Arrays.asList("camarote", "espera", "inicio", "saida").contains(locationType)) {
            sender.sendMessage(ChatColor.RED + "Tipo de localização inválida! Use: camarote, espera, inicio ou saida");
            return true;
        }

        if (arenaManager.setLocation(player, arenaId, locationType)) {
            sender.sendMessage(ChatColor.GREEN + "Localização " + locationType + " definida para a arena " + arenaId + "!");
        } else {
            sender.sendMessage(ChatColor.RED + "Arena não encontrada!");
        }
        return true;
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lunaguerra.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /guerra create <arena>");
            return true;
        }

        String arenaId = args[1];
        if (arenaManager.createArena(arenaId)) {
            sender.sendMessage(ChatColor.GREEN + "Arena " + arenaId + " criada com sucesso!");
        } else {
            sender.sendMessage(ChatColor.RED + "Arena já existe!");
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lunaguerra.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /guerra delete <arena>");
            return true;
        }

        String arenaId = args[1];
        if (arenaManager.deleteArena(arenaId)) {
            sender.sendMessage(ChatColor.GREEN + "Arena " + arenaId + " removida com sucesso!");
        } else {
            sender.sendMessage(ChatColor.RED + "Arena não encontrada!");
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lunaguerra.info")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /guerra info <arena>");
            return true;
        }

        String arenaId = args[1];
        Arena arena = arenaManager.getArena(arenaId);

        if (arena == null) {
            sender.sendMessage(configManager.getMessage("guerra.not_found_arena"));
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Informações da Arena " + arena.getDisplayName() + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Estado: " + getStateDisplay(arena.getState()));
        sender.sendMessage(ChatColor.YELLOW + "Participantes: " + arena.getTotalParticipants());
        sender.sendMessage(ChatColor.YELLOW + "Limite por clan: " + arena.getPlayersPerClan());

        if (arena.getState() != Arena.State.IDLE) {
            sender.sendMessage(ChatColor.YELLOW + "Clans participantes:");
            for (Map.Entry<String, Set<Player>> entry : arena.getParticipants().entrySet()) {
                sender.sendMessage(ChatColor.GRAY + "  " + entry.getKey() + ": " + entry.getValue().size() + " jogadores");
            }
        }

        sender.sendMessage(ChatColor.YELLOW + "Configurada: " + (arena.isConfigured() ? "Sim" : "Não"));
        return true;
    }

    private String getStateDisplay(Arena.State state) {
        switch (state) {
            case IDLE:
                return ChatColor.GRAY + "Inativa";
            case PREPARATION:
                return ChatColor.YELLOW + "Preparação";
            case COMBAT:
                return ChatColor.RED + "Combate";
            default:
                return ChatColor.GRAY + "Desconhecido";
        }
    }

    private boolean handleWinners(CommandSender sender, String[] args) {

        int amount = 5;
        if (args.length > 1) {
            try {
                amount = Integer.parseInt(args[1]);
                amount = Math.max(1, Math.min(amount, 20));
            } catch (NumberFormatException e) {
                amount = 5;
            }
        }

        List<Map<?, ?>> winners = configManager.getWinners(amount);

        if (winners.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Nenhum vencedor registrado ainda!");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Últimos Vencedores ===");
        for (int i = 0; i < winners.size(); i++) {
            Map<?, ?> winner = winners.get(i);
            String date = (String) winner.get("date");
            String clanTag = (String) winner.get("clan_tag");
            String playerGiven = (String) winner.get("player_given");

            sender.sendMessage(ChatColor.YELLOW + String.valueOf(i + 1) + ". " + date + " - " +
                    ChatColor.translateAlternateColorCodes('&', clanTag) +
                    ChatColor.YELLOW + " (Prêmio: " + playerGiven + ")");
        }
        return true;
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lunaguerra.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        try {
            configManager.loadConfigs();
            arenaManager.reloadArenas();
            sender.sendMessage(ChatColor.GREEN + "Configurações recarregadas com sucesso!");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Erro ao recarregar configurações: " + e.getMessage());
            plugin.getLogger().warning("Error reloading config: " + e.getMessage());
        }
        return true;
    }

    private boolean handleLimit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lunaguerra.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /guerra limit <arena> <quantidade>");
            return true;
        }

        String arenaId = args[1];
        Arena arena = arenaManager.getArena(arenaId);
        if (arena == null) {
            sender.sendMessage(configManager.getMessage("guerra.not_found_arena"));
            return true;
        }

        try {
            int limit = Integer.parseInt(args[2]);
            if (limit <= 0) {
                sender.sendMessage(ChatColor.RED + "O limite deve ser maior que 0!");
                return true;
            }

            if (configManager.setArenaPlayerLimit(arenaId, limit)) {
                sender.sendMessage(ChatColor.GREEN + "Limite de jogadores alterado para: " + limit);
                arena.setPlayersPerClan(limit);
            } else {
                sender.sendMessage(ChatColor.RED + "Erro ao alterar o limite de jogadores!");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Por favor, digite um número válido!");
        }
        return true;
    }

    private boolean handleKit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar este comando!");
            return true;
        }

        if (!sender.hasPermission("lunaguerra.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para salvar kits!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /guerra kit <arena>");
            return true;
        }

        String arenaId = args[1];
        Arena arena = arenaManager.getArena(arenaId);

        if (arena == null) {
            sender.sendMessage(configManager.getMessage("guerra.not_found_arena"));
            return true;
        }

        Player player = (Player) sender;
        kitManager.savePlayerKitToArena(player, arenaId);
        return true;
    }

    private boolean handleDisplayName(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lunaguerra.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /guerra displayname <arena> <nome> ou /guerra displayname <arena> \"<nome com espaços>\"");
            return true;
        }

        String arenaId = args[1];
        String displayName;
        
        String rawInput = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (rawInput.startsWith("\"") && rawInput.endsWith("\"")) {
            displayName = rawInput.substring(1, rawInput.length() - 1);
        } else {
            displayName = args[2];
        }

        Arena arena = arenaManager.getArena(arenaId);
        if (arena == null) {
            sender.sendMessage(configManager.getMessage("guerra.not_found_arena"));
            return true;
        }

        if (configManager.setArenaDisplayName(arenaId, displayName)) {
            sender.sendMessage(ChatColor.GREEN + "Nome da arena alterado para: " + displayName);
            arena.setDisplayName(displayName);
        } else {
            sender.sendMessage(ChatColor.RED + "Erro ao alterar o nome da arena!");
        }
        return true;
    }



    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "=== Ajuda do Plugin LunaGuerra ===");
        if (sender.hasPermission("lunaguerra.join")) {
            sender.sendMessage(ChatColor.YELLOW + "/guerra entrar <arena>" + ChatColor.WHITE + " - Entrar em uma guerra");
            sender.sendMessage(ChatColor.YELLOW + "/guerra sair" + ChatColor.WHITE + " - Sair da guerra atual");
            sender.sendMessage(ChatColor.YELLOW + "/guerra camarote <arena>" + ChatColor.WHITE + " - Assistir uma guerra");
        }
        // Winners command is available to everyone
        sender.sendMessage(ChatColor.YELLOW + "/guerra vencedores [quantidade]" + ChatColor.WHITE + " - Ver últimos vencedores");
        if (sender.hasPermission("lunaguerra.info")) {
            sender.sendMessage(ChatColor.YELLOW + "/guerra info <arena>" + ChatColor.WHITE + " - Ver informações de uma arena");
        }
        if (sender.hasPermission("lunaguerra.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/guerra create <arena>" + ChatColor.WHITE + " - Criar uma nova arena");
            sender.sendMessage(ChatColor.YELLOW + "/guerra delete <arena>" + ChatColor.WHITE + " - Deletar uma arena");
            sender.sendMessage(ChatColor.YELLOW + "/guerra start <arena>" + ChatColor.WHITE + " - Iniciar uma guerra");
            sender.sendMessage(ChatColor.YELLOW + "/guerra stop <arena>" + ChatColor.WHITE + " - Parar uma guerra");
            sender.sendMessage(ChatColor.YELLOW + "/guerra forcestart <arena>" + ChatColor.WHITE + " - Iniciar uma guerra imediatamente");
            sender.sendMessage(ChatColor.YELLOW + "/guerra displayname <arena> <nome>" + ChatColor.WHITE + " - Alterar nome da arena (use \"aspas\" para nomes com espaços)");
            sender.sendMessage(ChatColor.YELLOW + "/guerra limit <arena> <quantidade>" + ChatColor.WHITE + " - Alterar limite de jogadores");
            sender.sendMessage(ChatColor.YELLOW + "/guerra reload" + ChatColor.WHITE + " - Recarregar configurações");
        }
        if (sender.hasPermission("lunaguerra.set")) {
            sender.sendMessage(ChatColor.YELLOW + "/guerra set <arena> <camarote|espera|inicio|saida>" + ChatColor.WHITE + " - Definir locais da arena");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();

            // Add winners command for everyone
            subCommands.add("vencedores");
            
            if (sender.hasPermission("lunaguerra.join")) {
                subCommands.addAll(Arrays.asList("entrar", "sair", "camarote"));
            }
            if (sender.hasPermission("lunaguerra.info")) {
                subCommands.add("info");
            }

            if (sender.hasPermission("lunaguerra.admin")) {
                subCommands.addAll(Arrays.asList("start", "stop", "forcestart", "create", "delete", "reload", "displayname", "limit", "kit"));
            }
            if (sender.hasPermission("lunaguerra.set")) {
                subCommands.add("set");
            }

            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            List<String> options = new ArrayList<>();

            switch (subCommand) {
                case "entrar":
                case "start":
                case "stop":
                case "forcestart":
                case "info":
                case "delete":
                case "set":
                case "kit":
                case "displayname":
                case "limit":
                    arenaManager.getArenas().forEach(arena -> options.add(arena.getId()));
                    break;
                case "vencedores":
                    options.addAll(Arrays.asList("5", "10", "15", "20"));
                    break;
            }

            StringUtil.copyPartialMatches(args[1], options, completions);
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("set")) {
                List<String> locations = Arrays.asList("camarote", "espera", "inicio", "saida");
                StringUtil.copyPartialMatches(args[2], locations, completions);
            } else if (subCommand.equals("limit")) {
                List<String> limits = Arrays.asList("1", "2", "3", "4", "5", "10", "15", "20");
                StringUtil.copyPartialMatches(args[2], limits, completions);
            } else if (subCommand.equals("displayname")) {
                // For displayname, we don't provide completions for the actual name
                // as it's free-form text that can be quoted
                return completions;
            }
        }

        Collections.sort(completions);
        return completions;
    }


}
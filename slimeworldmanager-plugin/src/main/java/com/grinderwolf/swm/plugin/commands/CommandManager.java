package com.grinderwolf.swm.plugin.commands;

import com.grinderwolf.swm.plugin.commands.sub.*;
import com.grinderwolf.swm.plugin.log.Logging;

import lc.lcspigot.commands.Command;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandManager implements Command {

    @Getter
    private static CommandManager instance;
    private Map<String, Subcommand> commands = new HashMap<>();

    /* A list containing all the worlds that are being performed operations on, so two commands cannot be run at the same time */
    @Getter
    private final Set<String> worldsInUse = new HashSet<>();

    public CommandManager() {
        instance = this;

        commands.put("help", new HelpCmd());
        commands.put("version", new VersionCmd());
        commands.put("goto", new GotoCmd());
        commands.put("load", new LoadWorldCmd());
        commands.put("load-template", new LoadTemplateWorldCmd());
        commands.put("clone-world", new CloneWorldCmd());
        commands.put("unload", new UnloadWorldCmd());
        commands.put("list", new WorldListCmd());
        commands.put("dslist", new DSListCmd());
        commands.put("migrate", new MigrateWorldCmd());
        commands.put("delete", new DeleteWorldCmd());
        commands.put("import", new ImportWorldCmd());
        commands.put("reload", new ReloadConfigCmd());
        commands.put("create", new CreateWorldCmd());
    }

    public Collection<Subcommand> getCommands() {
        return commands.values();
    }


    @Override
    public void handle(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.AQUA + "Slime World Manager" + ChatColor.GRAY + " is a plugin that implements the Slime Region Format, " +
                    "designed by the Hypixel Dev Team to load and save worlds more efficiently. To check out the help page, type "
                    + ChatColor.YELLOW + "/swm help" + ChatColor.GRAY + ".");

            return;
        }

        Subcommand command = commands.get(args[0]);

        if (command == null) {
            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown command. To check out the help page, type " + ChatColor.GRAY + "/swm help" + ChatColor.RED + ".");

            return;
        }

        if (command.inGameOnly() && !(sender instanceof Player)) {
            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "This command can only be run in-game.");

            return;
        }

        if (!command.getPermission().equals("") && !sender.hasPermission(command.getPermission()) && !sender.hasPermission("swm.*")) {
            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "You do not have permission to perform this command.");

            return;
        }

        String[] subCmdArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subCmdArgs, 0, subCmdArgs.length);

        if (!command.onCommand(sender, subCmdArgs)) {
            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Command usage: /swm " + ChatColor.GRAY + command.getUsage() + ChatColor.RED + ".");
        }

    }

    @Override
    public String[] tab(CommandSender sender, String[] args) {
        List<String> toReturn = null;
        if (args.length <= 0) {
            return none();
        }
        final String typed = args[0].toLowerCase();

        if (args.length == 1) {
            for (Map.Entry<String, Subcommand> entry : commands.entrySet()) {
                final String name = entry.getKey();
                final Subcommand subcommand = entry.getValue();

                if (name.startsWith(typed) && !subcommand.getPermission().equals("")
                        && (sender.hasPermission(subcommand.getPermission()) || sender.hasPermission("swm.*"))) {

                    if (name.equalsIgnoreCase("goto") && (sender instanceof ConsoleCommandSender)) {
                        continue;
                    }

                    if (toReturn == null) {
                        toReturn = new LinkedList<>();
                    }

                    toReturn.add(name);
                }
            }
        }

        if (args.length > 1) {
            final String subName = args[0];
            final Subcommand subcommand = commands.get(subName);

            if (subcommand != null) {
                toReturn = subcommand.onTabComplete(sender, args);
            }
        }

        return toReturn == null ? none() : toReturn.toArray(new String[1]);
    }
}

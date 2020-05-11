package me.olipulse.deathtotems.Commands;

import me.olipulse.deathtotems.DeathTotem.DeathTotem;
import me.olipulse.deathtotems.DeathTotems;
import me.olipulse.deathtotems.EventListeners.EventListenerClass;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CommandClass implements CommandExecutor, TabCompleter {

    private final DeathTotems plugin;
    private final Economy economy;
    private final String CHATPREFIX = "&b[&3DeathTotems&b] ";


    public CommandClass(DeathTotems plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = ((Player) sender).getPlayer();
        }
        if (player != null) {
            if (command.getName().equalsIgnoreCase("restore")) {
                restoreInventory(player);
            }
        }
        if (command.getName().equalsIgnoreCase("dt")
                || command.getName().equalsIgnoreCase("deathtotems")
                || command.getName().equalsIgnoreCase("deathtotem")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("deathtotems.reload")) {
                        plugin.reloadConfig();
                        plugin.reloadPlugin();
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', CHATPREFIX + "&aThe plugin's configuration has been reloaded."));
                    } else {
                        sendPlayerNoPerm(sender);
                    }
                } else if (args[0].equalsIgnoreCase("restore")) {
                    if (player != null) {
                        if (player.hasPermission("deathtotems.restore")) {
                            restoreInventory(player);
                        }
                    }
                } else if (args[0].equalsIgnoreCase("list")) {
                    if (sender.hasPermission("deathtotems.list")) {
                        sendPlayerDeathTotemList(sender);
                    } else {
                        sendPlayerNoPerm(sender);
                    }
                } else if (args[0].equalsIgnoreCase("help")) {
                    if (sender.hasPermission("deathtotems.default")) {
                        sendPlayerHelp(sender);
                    } else {
                        sendPlayerNoPerm(sender);
                    }
                } else if (args[0].equalsIgnoreCase("discord")) {
                    if (sender.hasPermission("deathtotems.default")) {
                        sendPlayerDiscord(sender);
                    } else {
                        sendPlayerNoPerm(sender);
                    }
                }
                else {
                    sendPlayerUnknownCommand(sender);
                }
            } else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', CHATPREFIX + "&7The Inventory Restoring Plugin &f- &7By &9OliPulse"));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("dt")
                || command.getName().equalsIgnoreCase("deathtotems")
                || command.getName().equalsIgnoreCase("deathtotem"))
        {
            if (args.length == 1) {
                list.add("restore");
                if (sender.hasPermission("deathtotems.reload")) {
                    list.add("reload");
                }
                if (sender.hasPermission("deathtotems.list")) {
                    list.add("list");
                }
                if (sender.hasPermission("deathtotems.default")) {
                    list.add("help");
                    list.add("discord");
                }
            }
        }
        return list.stream().filter(a -> a.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
    }

    private void restoreInventory(Player player) {
        Configuration config = plugin.getConfig();
        String customPrefix = config.getString("chat-prefix");

        HashMap<UUID, DeathTotem> pendingDeathTotems = EventListenerClass.getPendingDeathTotems();
        HashMap<UUID, Inventory> pendingInventories = EventListenerClass.getPendingInventories();
        HashMap<UUID, Integer> pendingTimers = EventListenerClass.getPendingTimers();

        UUID playerUUID = player.getUniqueId();

        if (pendingInventories.containsKey(playerUUID) && pendingTimers.containsKey(playerUUID)) {
            double price = config.getDouble("recover-price");
            if (economy.has(player, price)) {
                economy.withdrawPlayer(player, price);
                EventListenerClass.playAnimationAndSound(player, pendingDeathTotems.get(playerUUID).getLocation());
                player.getInventory().setContents(pendingInventories.get(playerUUID).getContents());
                EventListenerClass.stopPlayersCurrentTimer(player);
                EventListenerClass.removePlayerFromPendingHashMaps(player);
                String recoverMessage = config.getString("recover-message");
                if (recoverMessage != null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', customPrefix + recoverMessage));
                }
            } else {
                String notEnoughMoneyMessage = config.getString("not-enough-money-message");
                if (notEnoughMoneyMessage != null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', customPrefix + notEnoughMoneyMessage));
                }
            }
        } else {
            String invalidRestoreMessage = config.getString("invalid-restore");
            if (invalidRestoreMessage != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', customPrefix + invalidRestoreMessage));
            }
        }
    }

    private void sendPlayerDeathTotemList(CommandSender sender) {
        HashMap<UUID, DeathTotem> deathTotemHashMap = EventListenerClass.getPendingDeathTotems();
        if (!deathTotemHashMap.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            int count = 1;
            for (UUID playerUUID : deathTotemHashMap.keySet()) {
                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null) {
                    DeathTotem totem = deathTotemHashMap.get(playerUUID);
                    Location loc = totem.getLocation();
                    builder.append("&6").append(count++).append(". &b").append(player.getName()).append(": &aX: &f").append(Math.round(loc.getX())).append(" &aY: &f")
                            .append(Math.round(loc.getY())).append(" &aZ: &f").append(Math.round(loc.getZ())).append("\n");
                }
            }
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b&m---------&r &3DeathTotems &b&m---------"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', builder.toString()));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b&m------------------------------"));
        } else {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', CHATPREFIX + "&cThere are no pending death totems at this moment."));
        }

    }

    private void sendPlayerUnknownCommand(CommandSender sender) {
        if (sender.hasPermission("deathtotems.default")) {
            sendPlayerHelp(sender);
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', CHATPREFIX + "&cUnknown command"));
    }

    private void sendPlayerHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b&m-----------&r &3DeathTotems &b&m-----------"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&3* &f/restore &8| &f/dt restore"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "    &6- &7Restore your pending items."));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&3* &f/dt list"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "    &6- &7Display all pending death totems."));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&3* &f/dt reload"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "    &6- &7Reload the plugin's configuration."));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&3* &f/dt discord"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "    &6- &7Join our support Discord Server."));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&3* &f/dt help"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "    &6- &7Open this menu."));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b&m----------------------------------"));
    }

    private void sendPlayerDiscord(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', CHATPREFIX + "&6https://discord.gg/47YEbMm &3&l<< &b(Click me)"));
    }

    private void sendPlayerNoPerm(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',  CHATPREFIX + "&cYou don't have permission to perform that command"));
    }

}

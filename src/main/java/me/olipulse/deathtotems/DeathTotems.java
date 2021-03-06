package me.olipulse.deathtotems;

import com.google.common.collect.Lists;
import me.olipulse.deathtotems.Commands.CommandClass;
import me.olipulse.deathtotems.EventListeners.EventListenerClass;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class DeathTotems extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    private static Economy economy = null;
    private static CommandClass commandClass;
    private static EventListenerClass eventListenerClass;
    private static final List<World> disabledWorlds = new ArrayList<>();

    @Override
    public void onEnable() {
        // Plugin startup logic

        this.saveDefaultConfig();

        if (!setupEconomy() ) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        commandClass = new CommandClass(this, economy);
        Objects.requireNonNull(getCommand("dt")).setExecutor(commandClass);
        Objects.requireNonNull(getCommand("restore")).setExecutor(commandClass);

        eventListenerClass = new EventListenerClass(this);
        getServer().getPluginManager().registerEvents(eventListenerClass, this);

        setupDisabledWorlds();

        getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "Enabled &b[&3DeathTotems&b] &8v" + getDescription().getVersion() + "&r - by &9OliPulse"));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }

    public void reloadPlugin() {
        commandClass = new CommandClass(this, economy);

        List<UUID> pendingPlayerUUIDs = Lists.newArrayList(EventListenerClass.getPendingDeathTotems().keySet());
        String timeUpMessage = getConfig().getString("time-up-message");

        for (UUID playerUUID : pendingPlayerUUIDs) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                if (timeUpMessage != null) {
                    String customPrefix = getConfig().getString("chat-prefix");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', customPrefix + timeUpMessage));
                }
                boolean shouldDropItems = false;
                Configuration config = getConfig();
                if (config.contains("drop-items-on-time-up")) {
                    shouldDropItems = config.getBoolean("drop-items-on-time-up");
                }
                EventListenerClass.removePlayerFromPendingHashMaps(player, shouldDropItems);
            }
        }

        eventListenerClass = new EventListenerClass(this);

        setupDisabledWorlds();
    }

    private void  setupDisabledWorlds() {
        disabledWorlds.clear();
        Configuration config = getConfig();
        if (config.contains("disabled-worlds", true)) {
            List<String> disabledWorldNames = config.getStringList("disabled-worlds").stream().filter(s -> !s.equals("")).collect(Collectors.toList());
            disabledWorldNames.forEach(worldName -> {
                        World world = Bukkit.getWorld(worldName);
                if (world != null) {
                            disabledWorlds.add(world);
                        } else {
                            log.warning(String.format("[%s] - Could not find disabled world with name '%s'!", getDescription().getName(), worldName));
                        }
                    }
            );
        }

        for (World world : Bukkit.getWorlds()) {
            if (!disabledWorlds.contains(world)) {
                world.setGameRule(GameRule.KEEP_INVENTORY, true);
            }
        }
    }

    public static List<World> getDisabledWorlds() {
        return disabledWorlds;
    }
}

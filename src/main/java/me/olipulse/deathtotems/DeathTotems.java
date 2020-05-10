package me.olipulse.deathtotems;

import com.google.common.collect.Lists;
import me.olipulse.deathtotems.Commands.CommandClass;
import me.olipulse.deathtotems.EventListeners.EventListenerClass;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public final class DeathTotems extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    private static Economy economy = null;
    private static CommandClass commandClass;
    private static EventListenerClass eventListenerClass;

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

        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
        }

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
                EventListenerClass.removePlayerFromPendingHashMaps(player);
            }
        }

        eventListenerClass = new EventListenerClass(this);

        for (World world : Bukkit.getWorlds()) {
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
        }
    }
}

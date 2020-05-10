package me.olipulse.deathtotems.EventListeners;

import me.olipulse.deathtotems.DeathTotem.DeathTotem;
import me.olipulse.deathtotems.DeathTotems;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;

public class EventListenerClass implements Listener {

    private static DeathTotems plugin;
    private static BukkitScheduler scheduler;
    private static HashMap<UUID, Inventory> pendingInventories;
    private static HashMap<UUID, Integer> pendingTimers;
    private static HashMap<UUID, DeathTotem> pendingDeathTotems;
    private static Material deathTotemMaterial;
    private static List<Particle> particles;
    private static Random random;
    private String customPrefix;

    public EventListenerClass(DeathTotems plugin) {
        EventListenerClass.plugin = plugin;
        this.customPrefix = plugin.getConfig().getString("chat-prefix");
        pendingInventories = new HashMap<>();
        pendingTimers = new HashMap<>();
        pendingDeathTotems = new HashMap<>();
        scheduler = plugin.getServer().getScheduler();
        String materialString = plugin.getConfig().getString("death-totem-material");
        if (materialString != null) {
            deathTotemMaterial = Material.getMaterial(materialString);
        }
        particles = new ArrayList<>();
        random = new Random();
        setupParticles();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        if (!player.hasPermission("deathtotems.bypass")) {
            Inventory inventory = player.getInventory();
            UUID playerUUID = player.getUniqueId();
            if (!pendingDeathTotems.containsKey(playerUUID)) {
                if (inventoryHasItems(player.getInventory())) {
                    Configuration config = plugin.getConfig();
                    String deathMessage = config.getString("death-message");
                    String customPrefix = config.getString("chat-prefix");
                    if (deathMessage != null) {
                        deathMessage = deathMessage.replaceAll("%POSITION_X%", ((int)player.getLocation().getX()) + "");
                        deathMessage = deathMessage.replaceAll("%POSITION_Y%", ((int)player.getLocation().getY()) + "");
                        deathMessage = deathMessage.replaceAll("%POSITION_Z%", ((int)player.getLocation().getZ()) + "");
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', customPrefix + deathMessage));

                        String hologram = config.getString("death-totem-hologram");
                        if (hologram != null) {
                            hologram = ChatColor.translateAlternateColorCodes('&', hologram.replaceAll("%PLAYER%", player.getName()));
                        }
                        DeathTotem deathTotem = new DeathTotem(e.getEntity().getLocation(), inventory, player, deathTotemMaterial, hologram);
                        pendingDeathTotems.put(playerUUID, deathTotem);

                        Inventory pendingInventory = Bukkit.createInventory(inventory.getHolder(), inventory.getType());
                        pendingInventory.setContents(inventory.getContents());
                        pendingInventories.put(playerUUID, pendingInventory);
                        startTimer(player);
                        inventory.clear();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDeathTotemInteract(PlayerInteractEvent e) {
        if (e.getHand() != null && e.getHand().equals(EquipmentSlot.OFF_HAND)) return;
        if (e.getClickedBlock() != null) {
            if (e.getClickedBlock().getType() == deathTotemMaterial) {
                DeathTotem deathTotem = getPendingDeathTotems(e.getClickedBlock().getLocation());
                if (deathTotem != null) {
                    Configuration config = plugin.getConfig();
                    Player player = e.getPlayer();
                    if (deathTotem.getPlayer() == player) {
                        if (e.useInteractedBlock() == Event.Result.DENY) {
                            e.setUseInteractedBlock(Event.Result.ALLOW);
                        }

                        String deathTotemMessage = config.getString("death-totem-message");
                        String customPrefix = config.getString("chat-prefix");
                        if (deathTotemMessage != null) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', customPrefix + deathTotemMessage));

                            deathTotem.delete();
                            deathTotem.dropItems();
                            UUID playerUUID = player.getUniqueId();

                            pendingDeathTotems.remove(playerUUID);
                            pendingInventories.remove(playerUUID);
                            stopPlayersCurrentTimer(player);
                            pendingTimers.remove(playerUUID);
                            e.setCancelled(true);
                            playAnimationAndSound(player, deathTotem.getLocation());
                        }
                    } else {
                        String invalidDeathTotemMessage = config.getString("invalid-death-totem-message");
                        if (invalidDeathTotemMessage != null) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', customPrefix + invalidDeathTotemMessage));
                        }
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        UUID playerUUID = player.getUniqueId();
        if (pendingDeathTotems.containsKey(playerUUID)) {
            removePlayerFromPendingHashMaps(player);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onDeathTotemExplode(EntityExplodeEvent e) {
        List<Block> deathTotems = new ArrayList<>();
        for (DeathTotem deathTotem : pendingDeathTotems.values()) {
            for (Block block : e.blockList()) {
                if (deathTotem.isDeathTotem(block.getLocation())) {
                    deathTotems.add(block);
                }
            }
        }
        e.blockList().removeAll(deathTotems);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onDeathTotemExtendedByPiston(BlockPistonExtendEvent e)  {
        for (DeathTotem deathTotem : pendingDeathTotems.values()) {
            for (Block block : e.getBlocks()) {
                if (deathTotem.isDeathTotem(block.getLocation())) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onDeathTotemRetractedByPiston(BlockPistonRetractEvent e)  {
        for (DeathTotem deathTotem : pendingDeathTotems.values()) {
            for (Block block : e.getBlocks()) {
                if (deathTotem.isDeathTotem(block.getLocation())) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onDeathTotemBurn(BlockBurnEvent e) {
        for (DeathTotem deathTotem : pendingDeathTotems.values()) {
            if (deathTotem.isDeathTotem(e.getBlock().getLocation())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    public boolean inventoryHasItems(Inventory inventory) {
        for(ItemStack item : inventory)
        {
            if(item != null) return true;
        }
        return false;
    }

    public void startTimer(Player player) {
        Configuration config = plugin.getConfig();
        Runnable runnable = () -> {
            if (removePlayerFromPendingHashMaps(player)) {
                String timeUpMessage = config.getString("time-up-message");
                if (timeUpMessage != null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', customPrefix + timeUpMessage));
                }
            }
        };
        long time = config.getLong("recover-timer") * 20;
        int id = scheduler.runTaskLater(plugin, runnable, time).getTaskId();
        UUID playerUUID = player.getUniqueId();
        pendingTimers.put(playerUUID, id);
    }

    public static boolean removePlayerFromPendingHashMaps(Player player) {
        boolean deleted = false;
        UUID playerUUID = player.getUniqueId();
        pendingInventories.remove(playerUUID);
        stopPlayersCurrentTimer(player);
        pendingTimers.remove(playerUUID);
        if (pendingDeathTotems.containsKey(playerUUID)) {
            deleted = pendingDeathTotems.get(playerUUID).delete();
        }

        pendingDeathTotems.remove(playerUUID);
        return deleted;
    }

    public static void stopPlayersCurrentTimer(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (pendingTimers.get(playerUUID) != null) {
            if (scheduler.isQueued(pendingTimers.get(playerUUID))) {
                scheduler.cancelTask(pendingTimers.get(playerUUID));
            }
        }
    }

    public static HashMap<UUID, Inventory> getPendingInventories() {
        return pendingInventories;
    }

    public static HashMap<UUID, Integer> getPendingTimers() {
        return pendingTimers;
    }

    public static HashMap<UUID, DeathTotem> getPendingDeathTotems() {
        return pendingDeathTotems;
    }

    public static DeathTotem getPendingDeathTotems(Location location) {
        for (DeathTotem deathTotem : pendingDeathTotems.values()) {
            if (deathTotem.isDeathTotem(location)) {
                return deathTotem;
            }
        }
        return null;
    }

    public static void setupParticles() {
        particles.add(Particle.TOTEM);
        particles.add(Particle.SMOKE_LARGE);
        particles.add(Particle.END_ROD);
        particles.add(Particle.FLAME);
        particles.add(Particle.NAUTILUS);
        particles.add(Particle.PORTAL);
        particles.add(Particle.FIREWORKS_SPARK);
        particles.add(Particle.SPELL_WITCH);
        particles.add(Particle.SPELL);
        particles.add(Particle.VILLAGER_HAPPY);
        particles.add(Particle.EXPLOSION_NORMAL);
        particles.add(Particle.CLOUD);
        particles.add(Particle.LAVA);
        particles.add(Particle.SQUID_INK);


    }

    public static void playAnimationAndSound(Player player, Location totemLocation) {
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.1f, random.nextFloat() + 0.5f);
        World world = totemLocation.getWorld();
        Location location = new Location(world, totemLocation.getX(), totemLocation.getY() + 0.5, totemLocation.getZ());
        if (world != null) {
            Particle particle = particles.get(random.nextInt(particles.size()));
            for (int i = 0; i < 10; i++) {
                world.spawnParticle(particle, location, 3, 1, 0.5, 1, 0.1, null, false);
            }
        }
    }

}

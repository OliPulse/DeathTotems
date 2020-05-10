package me.olipulse.deathtotems.DeathTotem;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DeathTotem {

    private Location location;

    private Inventory inventory;

    private List<ItemStack> items;

    private Player player;

    private Material material;

    private ArmorStand marker;

    private BlockData previousBlock;

    public DeathTotem(Location location, Inventory inventory, Player player, Material material, String hologram) {
        if (location.getX() < 0) {
            location.setX(location.getX() - 1);
        } if (location.getZ() < 0) {
            location.setZ(location.getZ() - 1);
        }
        this.location = new Location(location.getWorld(), (int)(location.getX()), (int)location.getY(), (int)location.getZ());
        previousBlock = this.location.getBlock().getBlockData().clone();
        this.inventory = inventory;
        items = Arrays.asList(inventory.getContents());
        items = items.stream().filter(Objects::nonNull).collect(Collectors.toList());
        this.player = player;
        this.material = material;
        spawn(hologram);
    }

    public void spawn(String hologram) {
        location.getBlock().setType(material);
        displayMarker(hologram);
    }

    public boolean isDeathTotem(Location location) {
        return location.equals(this.location);
    }

    public boolean delete() {
        restoreReplacedBlocks();
        deleteMarker();
        return true;
    }

    public void dropItems() {
        World world = location.getWorld();
        if (world != null) {
            for (ItemStack item : items) {
                world.dropItemNaturally(location, item);
            }
        }
    }

    public void displayMarker(String hologram) {
        marker = (ArmorStand) Objects.requireNonNull(location.getWorld()).spawnEntity(
                new Location(location.getWorld(), location.getX() + 0.5, location.getY() + 0.9, location.getZ() + 0.5), EntityType.ARMOR_STAND);
        marker.setVisible(false);
        marker.setCustomName(hologram);
        marker.setCustomNameVisible(true);
        marker.setSmall(true);
        marker.setInvulnerable(true);
        marker.setGravity(false);
        marker.setBasePlate(false);
        marker.setMarker(true);
    }

    public void deleteMarker() {
        marker.remove();
        Collection<Entity> entities = Objects.requireNonNull(location.getWorld())
                .getNearbyEntities(new Location(location.getWorld(), location.getX() + 0.5, location.getY() + 0.9, location.getZ() + 0.5)
                        , 0.1, 0.1, 0.1);
        for (Entity entity : entities) {
            if (entity instanceof ArmorStand) {
                if (((ArmorStand) entity).isMarker()) {
                    entity.remove();
                }
            }
        }
    }

    public Player getPlayer() {
        return player;
    }

    public void restoreReplacedBlocks() {
        location.getBlock().setType(Material.AIR);
        location.getBlock().setBlockData(previousBlock);
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public Location getLocation() {
        return location;
    }
}

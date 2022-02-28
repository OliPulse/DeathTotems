package me.olipulse.deathtotems.DeathTotem;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
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
        //Fix block height limit issue
        //Fix for new height in 1.18
        if (this.location.getY() > 319) {
            this.location.setY(319);
        }
        //Fix below 0 totem  issue
        //Fix for new height in 1.18
        if (this.location.getY() < -64) {
            this.location.setY(-64);
        }
        previousBlock = this.location.getBlock().getBlockData().clone();
        //Disable stealing from chests
        if (previousBlock.getMaterial() == Material.CHEST && this.location.getY() < 255) {
            this.location.setY(this.location.getY() + 1);
            previousBlock = this.location.getBlock().getBlockData().clone();
        }
        this.inventory = inventory;
        items = Arrays.asList(inventory.getContents());
        //Remove curse of vanishing and null items
        items = items.stream().filter(i -> i != null && !i.getEnchantments().containsKey(Enchantment.VANISHING_CURSE)).collect(Collectors.toList());
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
        //Don't replace lava (items will burn)
        if (previousBlock.getMaterial() != Material.LAVA) {
            location.getBlock().setBlockData(previousBlock);
        }
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public Location getLocation() {
        return location;
    }
}

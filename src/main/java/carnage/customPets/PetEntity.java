package carnage.customPets;

import carnage.customPets.Buffs.BuffType;
import carnage.customPets.PetItem.AbilityItem;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class PetEntity {
    private final Plugin plugin;
    private Player owner;
    private final Pet pet;
    private ArmorStand armorStand;
    private BukkitRunnable followTask;
    private BukkitRunnable levitationTask;
    private BukkitRunnable buffTask;
    private final Random random = new Random();

    private float floatOffset = 0;
    private int floatDirection = 1;

    private boolean visible = true;
    private boolean floatingEnabled = true;
    private boolean globallyVisible = true;

    private static final double INTERPOLATION_FACTOR = 0.15;
    private static final double FOLLOW_DISTANCE = 1.5;
    private static final double VERTICAL_OFFSET = 1.2;

    public PetEntity(Plugin plugin, Player owner, Pet pet) {
        this.plugin = plugin;
        this.owner = owner;
        this.pet = pet;
        spawn();
    }

    private void spawn() {
        Location loc = owner.getLocation();
        World world = owner.getWorld();
        this.armorStand = world.spawn(loc, ArmorStand.class);

        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setSmall(true);
        armorStand.setCollidable(false);
        armorStand.setInvulnerable(true);

        Component nameComponent = LegacyComponentSerializer.legacySection().deserialize(pet.getDisplayName());
        armorStand.customName(nameComponent);
        armorStand.setCustomNameVisible(visible);

        armorStand.setMetadata("pet-settings", new FixedMetadataValue(plugin, owner.getUniqueId().toString()));

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", pet.getTexture()));
        meta.setPlayerProfile(profile);
        head.setItemMeta(meta);
        armorStand.getEquipment().setHelmet(head);

        startFollowing();
        startLevitationEffect();

        boolean useAbilityItem = plugin.getConfig().getBoolean("pets.use-ability-item", false);
        int intervalSeconds = plugin.getConfig().getInt("pets.effect-interval", 5);

        if (useAbilityItem) {
            giveAbilityItem();
        } else {
            startAutoBuffTask(intervalSeconds);
        }
    }

    public Player getOwner() {
        return owner;
    }

    public Pet getPet() {
        return pet;
    }

    public ArmorStand getArmorStand() {
        return armorStand;
    }

    public void pauseFollowing() {
        if (followTask != null) {
            followTask.cancel();
            followTask = null;
        }
        if (buffTask != null) {
            buffTask.cancel();
            buffTask = null;
        }
    }

    public void resumeFollowing(Player player) {
        this.owner = player;
        startFollowing();

        boolean useAbilityItem = plugin.getConfig().getBoolean("pets.use-ability-item", false);
        int intervalSeconds = plugin.getConfig().getInt("pets.effect-interval", 5);
        if (useAbilityItem) {
            giveAbilityItem();
        } else {
            startAutoBuffTask(intervalSeconds);
        }

        if (!globallyVisible) {
            hideFromAllExceptOwner();
        }
    }

    private void startFollowing() {
        if (followTask != null) {
            followTask.cancel();
        }

        followTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (owner == null || !owner.isOnline() || armorStand == null || armorStand.isDead()) {
                    cancel();
                    return;
                }

                Location playerLoc = owner.getLocation();
                Vector playerDir = playerLoc.getDirection().clone().setY(0).normalize();
                if (playerDir.lengthSquared() < 0.0001) {
                    playerDir = new Vector(0, 0, 1);
                }

                Vector behindOffset = playerDir.clone().multiply(-FOLLOW_DISTANCE);
                Location targetLoc = playerLoc.clone()
                        .add(behindOffset)
                        .add(0, VERTICAL_OFFSET + floatOffset, 0);

                Location currentLoc = armorStand.getLocation();
                Vector diff = targetLoc.toVector().subtract(currentLoc.toVector());
                double distSq = diff.lengthSquared();
                if (distSq > 0.0025) {
                    Vector step = diff.multiply(INTERPOLATION_FACTOR);
                    Location newLoc = currentLoc.clone().add(step);
                    newLoc.setYaw(armorStand.getLocation().getYaw());
                    newLoc.setPitch(armorStand.getLocation().getPitch());
                    armorStand.teleport(newLoc);
                } else {
                    targetLoc.setYaw(armorStand.getLocation().getYaw());
                    targetLoc.setPitch(armorStand.getLocation().getPitch());
                    armorStand.teleport(targetLoc);
                }

                lookAtOwner();
            }
        };

        followTask.runTaskTimer(plugin, 1L, 1L);
    }

    private void lookAtOwner() {
        if (armorStand == null || owner == null || !owner.isOnline()) return;

        Location petLoc = armorStand.getLocation();
        Location playerEye = owner.getLocation().add(0, owner.getEyeHeight() - 0.2, 0);

        Vector toPlayer = playerEye.toVector().subtract(petLoc.toVector());
        double dx = toPlayer.getX();
        double dy = toPlayer.getY();
        double dz = toPlayer.getZ();

        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        double horiz = Math.sqrt(dx * dx + dz * dz);
        double pitch = Math.toDegrees(Math.atan2(-dy, horiz));

        armorStand.setRotation((float) yaw, (float) pitch);
    }

    private void startLevitationEffect() {
        if (levitationTask != null) {
            levitationTask.cancel();
            levitationTask = null;
        }

        levitationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!floatingEnabled || armorStand == null || armorStand.isDead()) {
                    cancel();
                    levitationTask = null;
                    return;
                }

                floatOffset += 0.02f * floatDirection;
                if (floatOffset > 0.15f) {
                    floatDirection = -1;
                } else if (floatOffset < -0.15f) {
                    floatDirection = 1;
                }

                if (random.nextInt(100) < 10) {
                    armorStand.getWorld().spawnParticle(
                            Particle.CLOUD,
                            armorStand.getLocation().add(0, 0.2, 0),
                            1,
                            0.1, 0, 0.1,
                            0.01
                    );
                }
            }
        };

        levitationTask.runTaskTimer(plugin, 1L, 1L);
    }

    private void startAutoBuffTask(int intervalSeconds) {
        if (buffTask != null) {
            buffTask.cancel();
            buffTask = null;
        }

        buffTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (owner == null || !owner.isOnline() || armorStand == null || armorStand.isDead()) {
                    cancel();
                    return;
                }
                triggerBuff();
            }
        };

        buffTask.runTaskTimer(plugin, intervalSeconds * 20L, intervalSeconds * 20L);
    }

    public void triggerBuff() {
        for (Map.Entry<BuffType, Double> entry : pet.getBuffValues().entrySet()) {
            BuffType type = entry.getKey();
            double value = entry.getValue();
            if (!type.isPassive()) {
                type.apply(owner, (int) Math.floor(value));
            }
        }
    }

    private void giveAbilityItem() {
        for (ItemStack item : owner.getInventory().getContents()) {
            if (item != null && AbilityItem.isAbilityItem(item)) {
                return;
            }
        }
        owner.getInventory().addItem(AbilityItem.createAbilityItem(pet.getDisplayName()));
    }

    public void remove() {
        if (followTask != null) {
            followTask.cancel();
            followTask = null;
        }
        if (levitationTask != null) {
            levitationTask.cancel();
            levitationTask = null;
        }
        if (buffTask != null) {
            buffTask.cancel();
            buffTask = null;
        }
        if (armorStand != null && !armorStand.isDead()) {
            armorStand.remove();
        }

        removeAbilityItem();
    }

    private void removeAbilityItem() {
        owner.getInventory().removeItem(AbilityItem.createAbilityItem(pet.getDisplayName()));
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        if (armorStand != null) {
            armorStand.setCustomNameVisible(visible);
        }
    }

    public boolean isFloatingEnabled() {
        return floatingEnabled;
    }

    public void setFloatingEnabled(boolean floatingEnabled) {
        this.floatingEnabled = floatingEnabled;
        if (!floatingEnabled && levitationTask != null) {
            levitationTask.cancel();
            levitationTask = null;
        } else if (floatingEnabled && levitationTask == null) {
            startLevitationEffect();
        }
    }

    public boolean isGloballyVisible() {
        return globallyVisible;
    }

    public void setGloballyVisible(boolean showToAll) {
        this.globallyVisible = showToAll;
        if (armorStand == null) return;

        if (showToAll) {
            for (Player p : armorStand.getWorld().getPlayers()) {
                p.showEntity(plugin, armorStand);
            }
        } else {
            hideFromAllExceptOwner();
        }
    }

    private void hideFromAllExceptOwner() {
        if (armorStand == null) return;
        for (Player p : armorStand.getWorld().getPlayers()) {
            if (!p.getUniqueId().equals(owner.getUniqueId())) {
                p.hideEntity(plugin, armorStand);
            }
        }
    }

    public String getLegacyCustomName() {
        Component comp = armorStand.customName();
        return comp == null
                ? ""
                : LegacyComponentSerializer.legacySection().serialize(comp);
    }

    public void updateDisplay() {
        if (armorStand == null) return;

        // Update the custom name
        Component nameComponent = LegacyComponentSerializer.legacySection().deserialize(pet.getDisplayName());
        armorStand.customName(nameComponent);
        armorStand.setCustomNameVisible(visible);

        // Update the helmet (texture)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", pet.getTexture()));
        meta.setPlayerProfile(profile);
        head.setItemMeta(meta);
        armorStand.getEquipment().setHelmet(head);
    }


}

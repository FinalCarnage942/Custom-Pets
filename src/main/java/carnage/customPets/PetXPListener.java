package carnage.customPets;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PetXPListener implements Listener {

    private final Plugin plugin;
    private final Map<UUID, Long> xpCooldown;

    public PetXPListener(Plugin plugin) {
        this.plugin = plugin;
        this.xpCooldown = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (xpCooldown.containsKey(playerUUID) && System.currentTimeMillis() - xpCooldown.get(playerUUID) < 100) {
            return;
        }

        xpCooldown.put(playerUUID, System.currentTimeMillis());

        PetManager petManager = CustomPets.getInstance().getPetManager();
        if (!petManager.hasPetActive(player)) {
            return;
        }

        Material material = event.getBlock().getType();
        int xp = getXPForBlock(material);
        if (xp > 0) {
            PetListener petListener = new PetListener(CustomPets.getInstance());
            petListener.distributeXP(player, xp);
            player.sendMessage("§aYour pet gained " + xp + " XP from breaking " + material.name().toLowerCase() + "!");
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) {
            return;
        }

        Player player = event.getEntity().getKiller();
        UUID playerUUID = player.getUniqueId();

        if (xpCooldown.containsKey(playerUUID) && System.currentTimeMillis() - xpCooldown.get(playerUUID) < 100) {
            return;
        }

        xpCooldown.put(playerUUID, System.currentTimeMillis());

        PetManager petManager = CustomPets.getInstance().getPetManager();
        if (!petManager.hasPetActive(player)) {
            return;
        }

        EntityType entityType = event.getEntityType();
        int xp = getXPForMob(entityType);
        if (xp > 0) {
            PetListener petListener = new PetListener(CustomPets.getInstance());
            petListener.distributeXP(player, xp);
            player.sendMessage("§aYour pet gained " + xp + " XP from killing " + entityType.name().toLowerCase() + "!");
        }
    }

    private int getXPForBlock(Material material) {
        FileConfiguration xpValuesConfig = CustomPets.getInstance().getXpValuesConfig();
        ConfigurationSection cropsSection = xpValuesConfig.getConfigurationSection("xp-values.crops");
        ConfigurationSection woodSection = xpValuesConfig.getConfigurationSection("xp-values.wood");
        ConfigurationSection oresSection = xpValuesConfig.getConfigurationSection("xp-values.ores");

        switch (material) {
            case WHEAT: return cropsSection.getInt("WHEAT", 10);
            case CARROTS: return cropsSection.getInt("CARROT", 15);
            case POTATOES: return cropsSection.getInt("POTATO", 15);
            case BEETROOTS: return cropsSection.getInt("BEETROOT", 20);
            case NETHER_WART: return cropsSection.getInt("NETHER_WART", 25);
            case SUGAR_CANE: return cropsSection.getInt("SUGAR_CANE", 5);
            case PUMPKIN: return cropsSection.getInt("PUMPKIN", 30);
            case MELON: return cropsSection.getInt("MELON", 30);
            case OAK_LOG: return woodSection.getInt("OAK_LOG", 5);
            case SPRUCE_LOG: return woodSection.getInt("SPRUCE_LOG", 5);
            case BIRCH_LOG: return woodSection.getInt("BIRCH_LOG", 5);
            case JUNGLE_LOG: return woodSection.getInt("JUNGLE_LOG", 5);
            case ACACIA_LOG: return woodSection.getInt("ACACIA_LOG", 5);
            case DARK_OAK_LOG: return woodSection.getInt("DARK_OAK_LOG", 5);
            case MANGROVE_LOG: return woodSection.getInt("MANGROVE_LOG", 5);
            case CHERRY_LOG: return woodSection.getInt("CHERRY_LOG", 5);
            case CRIMSON_STEM: return woodSection.getInt("CRIMSON_STEM", 10);
            case WARPED_STEM: return woodSection.getInt("WARPED_STEM", 10);
            case COAL_ORE: return oresSection.getInt("COAL_ORE", 10);
            case IRON_ORE: return oresSection.getInt("IRON_ORE", 15);
            case COPPER_ORE: return oresSection.getInt("COPPER_ORE", 15);
            case GOLD_ORE: return oresSection.getInt("GOLD_ORE", 20);
            case REDSTONE_ORE: return oresSection.getInt("REDSTONE_ORE", 20);
            case EMERALD_ORE: return oresSection.getInt("EMERALD_ORE", 25);
            case LAPIS_ORE: return oresSection.getInt("LAPIS_ORE", 20);
            case DIAMOND_ORE: return oresSection.getInt("DIAMOND_ORE", 30);
            case NETHER_QUARTZ_ORE: return oresSection.getInt("NETHER_QUARTZ_ORE", 20);
            case NETHER_GOLD_ORE: return oresSection.getInt("NETHER_GOLD_ORE", 25);
            case ANCIENT_DEBRIS: return oresSection.getInt("ANCIENT_DEBRIS", 50);
            default: return 0;
        }
    }

    private int getXPForMob(EntityType entityType) {
        FileConfiguration xpValuesConfig = CustomPets.getInstance().getXpValuesConfig();
        ConfigurationSection mobsSection = xpValuesConfig.getConfigurationSection("xp-values.mobs");

        switch (entityType) {
            case ZOMBIE: return mobsSection.getInt("ZOMBIE", 10);
            case SKELETON: return mobsSection.getInt("SKELETON", 10);
            case SPIDER: return mobsSection.getInt("SPIDER", 10);
            case CAVE_SPIDER: return mobsSection.getInt("CAVE_SPIDER", 15);
            case CREEPER: return mobsSection.getInt("CREEPER", 15);
            case ENDERMAN: return mobsSection.getInt("ENDERMAN", 20);
            case BLAZE: return mobsSection.getInt("BLAZE", 20);
            case GHAST: return mobsSection.getInt("GHAST", 25);
            case SLIME: return mobsSection.getInt("SLIME", 5);
            case MAGMA_CUBE: return mobsSection.getInt("MAGMA_CUBE", 10);
            case PIGLIN: return mobsSection.getInt("PIGLIN", 15);
            case HOGLIN: return mobsSection.getInt("HOGLIN", 20);
            case WITHER_SKELETON: return mobsSection.getInt("WITHER_SKELETON", 25);
            case PHANTOM: return mobsSection.getInt("PHANTOM", 15);
            case GUARDIAN: return mobsSection.getInt("GUARDIAN", 20);
            case ELDER_GUARDIAN: return mobsSection.getInt("ELDER_GUARDIAN", 30);
            case VINDICATOR: return mobsSection.getInt("VINDICATOR", 20);
            case EVOKER: return mobsSection.getInt("EVOKER", 30);
            case VEX: return mobsSection.getInt("VEX", 25);
            case PILLAGER: return mobsSection.getInt("PILLAGER", 15);
            case RAVAGER: return mobsSection.getInt("RAVAGER", 30);
            case WITCH: return mobsSection.getInt("WITCH", 25);
            case ENDERMITE: return mobsSection.getInt("ENDERMITE", 10);
            case SILVERFISH: return mobsSection.getInt("SILVERFISH", 5);
            case PIG: return mobsSection.getInt("PIG", 5);
            case COW: return mobsSection.getInt("COW", 5);
            case SHEEP: return mobsSection.getInt("SHEEP", 5);
            case CHICKEN: return mobsSection.getInt("CHICKEN", 5);
            case RABBIT: return mobsSection.getInt("RABBIT", 5);
            case WOLF: return mobsSection.getInt("WOLF", 10);
            case OCELOT: return mobsSection.getInt("OCELOT", 10);
            case HORSE: return mobsSection.getInt("HORSE", 15);
            case DONKEY: return mobsSection.getInt("DONKEY", 15);
            case MULE: return mobsSection.getInt("MULE", 15);
            case LLAMA: return mobsSection.getInt("LLAMA", 15);
            case PARROT: return mobsSection.getInt("PARROT", 10);
            case TURTLE: return mobsSection.getInt("TURTLE", 10);
            case DOLPHIN: return mobsSection.getInt("DOLPHIN", 15);
            case COD: return mobsSection.getInt("COD", 5);
            case SALMON: return mobsSection.getInt("SALMON", 5);
            case TROPICAL_FISH: return mobsSection.getInt("TROPICAL_FISH", 5);
            case PUFFERFISH: return mobsSection.getInt("PUFFERFISH", 10);
            case FOX: return mobsSection.getInt("FOX", 10);
            case BEE: return mobsSection.getInt("BEE", 5);
            case PANDA: return mobsSection.getInt("PANDA", 15);
            case STRIDER: return mobsSection.getInt("STRIDER", 15);
            case ZOGLIN: return mobsSection.getInt("ZOGLIN", 25);
            case PIGLIN_BRUTE: return mobsSection.getInt("PIGLIN_BRUTE", 30);
            default: return 0;
        }
    }
}

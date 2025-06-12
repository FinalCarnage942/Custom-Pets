// PetGUI.java
package carnage.customPets.GUIs;

import carnage.customPets.*;
import carnage.customPets.Buffs.BuffType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PetGUI implements org.bukkit.inventory.InventoryHolder {
    private final CustomPets plugin;
    private final Player player;
    private Inventory inventory;

    public PetGUI(CustomPets plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(
                this,
                54,
                Component.text("§aSelect Your Pet").decoration(TextDecoration.ITALIC, false)
        );
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        Bukkit.getLogger().info("Opening Pet GUI for player " + player.getName());
        inventory.clear();

        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        border.setItemMeta(borderMeta);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        ItemStack header = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta headerMeta = header.getItemMeta();
        headerMeta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        header.setItemMeta(headerMeta);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, header);
            inventory.setItem(i + 45, header);
        }

        inventory.setItem(49, PetListener.DESPAWN_ITEM);

        PetEntity activePet = plugin.getPetManager().getActivePet(player);
        ItemStack levelItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta levelMeta = levelItem.getItemMeta();
        if (levelMeta != null) {
            if (activePet != null) {
                String petId = activePet.getPet().getId();
                PetManager.PetSettings settings = plugin.getPetManager().getSettings(player.getUniqueId(), petId);
                String lvl = String.valueOf(settings.getLevel());
                int xp = settings.getXp();
                int currentLevel = settings.getLevel();
                int requiredXP = (int) (activePet.getPet().getBaseXP() * Math.pow(activePet.getPet().getXpMultiplier(), currentLevel - 1));

                levelMeta.displayName(Component.text("§ePet Level: §6" + lvl).decoration(TextDecoration.ITALIC, false));
                levelMeta.lore(List.of(
                        Component.text("§7Current Level: §e" + lvl).decoration(TextDecoration.ITALIC, false),
                        Component.text("§7XP: §e" + xp + "§7/§e" + requiredXP).decoration(TextDecoration.ITALIC, false)
                ));
            } else {
                levelMeta.displayName(Component.text("§ePet Level: §6N/A").decoration(TextDecoration.ITALIC, false));
                levelMeta.lore(List.of(
                        Component.text("§7No active pet").decoration(TextDecoration.ITALIC, false),
                        Component.text("§7Level and XP unavailable").decoration(TextDecoration.ITALIC, false)
                ));
            }
            levelItem.setItemMeta(levelMeta);
        }
        inventory.setItem(47, levelItem);

        ItemStack petInfoItem = new ItemStack(Material.BOOK);
        ItemMeta petInfoMeta = petInfoItem.getItemMeta();
        petInfoMeta.displayName(Component.text("§bPet Info").decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Click to view pet buffs and their effects.").decoration(TextDecoration.ITALIC, false));

        if (activePet != null) {
            String petId = activePet.getPet().getId();
            Pet pet = plugin.getPetManager().getPets().get(petId);
            Map<BuffType, Double> buffs = pet.getBuffValues();

            for (Map.Entry<BuffType, Double> entry : buffs.entrySet()) {
                BuffType buffType = entry.getKey();
                double value = entry.getValue();
                String description = getBuffDescription(buffType, value);
                lore.add(Component.text("§7" + buffType.getDisplayName() + ": §e" + description).decoration(TextDecoration.ITALIC, false));
            }
        } else {
            lore.add(Component.text("§7No active pet to display info.").decoration(TextDecoration.ITALIC, false));
        }

        petInfoMeta.lore(lore);
        petInfoItem.setItemMeta(petInfoMeta);
        inventory.setItem(4, petInfoItem);

        ItemStack shareExp = new ItemStack(Material.EMERALD);
        ItemMeta shareMeta = shareExp.getItemMeta();
        if (shareMeta != null) {
            shareMeta.displayName(Component.text("§bShare EXP Between Pets").decoration(TextDecoration.ITALIC, false));
            shareMeta.lore(List.of(
                    Component.text("§7Click to open the Share EXP menu"),
                    Component.text("§7Select up to 3 other pets")
            ));
            shareExp.setItemMeta(shareMeta);
        }
        inventory.setItem(51, shareExp);

        int[] petSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
        PetManager petManager = plugin.getPetManager();
        List<String> redeemedPets = (List<String>) plugin.getRedeemedPets(player.getUniqueId());

        // Parse the redeemedPets list into a map of pet IDs to rarities
        Map<String, Pet.Rarity> petRarities = new HashMap<>();
        for (String petId : redeemedPets) {
            Pet.Rarity rarity = plugin.getRedeemedPetRarity(player.getUniqueId(), petId);
            petRarities.put(petId, rarity);
        }

        int slotIndex = 0;
        for (Map.Entry<String, Pet> entry : petManager.getPets().entrySet()) {
            if (slotIndex >= petSlots.length) break;
            String petId = entry.getKey();

            // Check if this pet is in our parsed redeemed pets data
            if (!petRarities.containsKey(petId)) continue;

            Pet petDef = entry.getValue();
            PetManager.PetSettings settings = petManager.getSettings(player.getUniqueId(), petId);

            // Get the player-specific rarity if available
            Pet.Rarity playerRarity = petRarities.get(petId);

            ItemStack petItem = petDef.createItem(settings.getLevel(), playerRarity);

            ItemMeta meta = petItem.getItemMeta();

            String customName = settings.getCustomName();
            if (customName != null && !customName.isEmpty()) {
                meta.displayName(Component.text(customName).decoration(TextDecoration.ITALIC, false));
            }

            meta.getPersistentDataContainer().set(
                    petManager.getPetKey(),
                    PersistentDataType.STRING,
                    petId
            );
            petItem.setItemMeta(meta);

            inventory.setItem(petSlots[slotIndex], petItem);
            slotIndex++;
        }

        player.openInventory(inventory);
    }

    private String getBuffDescription(BuffType buffType, double value) {
        return switch (buffType) {
            case SPEED -> "Boosts player's movement speed for " + (int) value + " seconds.";
            case JUMP_BOOST -> "Increases jump height for " + (int) value + " seconds.";
            case STRENGTH -> "Boosts melee attack damage for " + (int) value + "seconds.";
            case REGENERATION -> "Gradual health recovery for " + (int) value + " seconds.";
            case FIRE_RESISTANCE -> "Immunity to fire and lava damage for " + (int) value + "seconds.";
            case INVISIBILITY -> "Makes the player invisible for " + (int) value + "seconds.";
            case NIGHT_VISION -> "Improves visibility in darkness for " + (int) value + " seconds.";
            case WATER_BREATHING -> "Allows breathing underwater for " + (int) value + "seconds.";
            case HASTE -> "Increases mining speed for " + (int) value + "seconds.";
            case ABSORPTION -> "Grants extra temporary hearts for " + (int) value + "seconds.";
            case SATURATION -> "Keeps hunger bar filled passively for " + (int) value + "seconds.";
            case RESISTANCE -> "Slightly reduces all incoming damage for " + (int) value + "seconds.";
            case HEALTH_BOOST -> "Increases max health temporarily for " + (int) value + "seconds.";
            case SLOW_FALLING -> "Prevents fall damage for " + (int) value + "seconds.";
            case CONDUIT_POWER -> "Improved underwater abilities for " + (int) value + "seconds.";
            case HERO_OF_THE_VILLAGE -> "Improved trades with villagers for " + (int) value + "seconds.";
            case DOUBLE_CROP_DROP -> "Chance to double harvested crops: " + value + "%.";
            case FISHING_LUCK -> "Increases chance for rare fishing loot: " + value + "%.";
            case ONE_SHOT -> "Chance to instantly kill mobs on hit: " + value + "%.";
            case DOUBLE_MOB_DROPS -> "Chance to double mob item drops: " + value + "%.";
            case XP_BOOST -> "Gain more XP from mobs and activities: " + value + "%.";
            case LOOTING_BOOST -> "Increases chance for rare mob drops: " + value + "%.";
            case AREA_MINE -> "Breaks surrounding blocks in an area: " + value + "%.";
            case WOOD_CHOP -> "Fells an entire tree with one log: " + value + "%.";
            case WOOD_FORTUNE -> "Increases log drops when chopping: " + value + "%.";
            case SCAVENGER -> "Chance to find bonus loot when killing mobs: " + value + "%.";
            case LUCKY_KILL -> "Chance to drop emerald or rare item on mob kill: " + value + "%.";
            case MOB_REPELLENT -> "Passive chance to prevent hostile mob targeting: " + value + "%.";
            case INSTANT_SMELT -> "Ores drop smelted ingots instantly: " + value + "%.";
            case NO_FALL -> "Prevents fall damage: " + value + "%.";
            case ORE_XRAY -> "Highlights nearby ores when mining: " + value + "%.";
            case AUTO_REPLANT -> "Crops are replanted automatically when harvested: " + value + "%.";
            case MOB_SLOW -> "Slows mobs within a small radius: " + value + "%.";
            case AREA_PICKUP -> "Automatically picks up drops in an area: " + value + "%.";
            case FREEZE_STRIKE -> "Chance to freeze enemy on hit: " + value + "%.";
            case SHIELD_WALL -> "Chance to block all incoming damage briefly: " + value + "%.";
            case MAGNETIC_MINING -> "Auto-pickup ores while mining: " + value + "%.";
            case BLOODLUST -> "Temporary strength boost after mob kill: " + value + "%.";
            case DODGE -> "Chance to completely avoid damage: " + value + "%.";
            case BURN_AURA -> "Burns nearby enemies passively: " + value + "%.";
            case POISON_TOUCH -> "Applies poison to enemies on hit: " + value + "%.";
            case GOLDEN_TOUCH -> "Chance for ores to drop gold instead: " + value + "%.";
            case PET_HEAL -> "Slowly heals you while pet is summoned: " + value + "%.";
            case TREASURE_HUNTER -> "Chance to find loot in dirt/gravel/sand: " + value + "%.";
            case FARM_HAND -> "Speeds up crop growth nearby: " + value + "%.";
            case SOUL_COLLECTOR -> "Killing mobs grants short strength bonus: " + value + "%.";
            case VEIN_MINER -> "Mines entire connected vein of ores: " + value + "%.";
            case REFLECT_DAMAGE -> "Chance to reflect incoming damage back: " + value + "%.";
            default -> "No description available.";
        };
    }
}

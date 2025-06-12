package carnage.customPets;

import carnage.customPets.Buffs.BuffType;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class Pet {
    private final String id;
    private final String displayName;
    private final String texture;
    private final String description;
    private final Map<BuffType, Double> buffValues;
    private final PetType type;
    private Rarity rarity;
    private final Map<Rarity, RarityConfig> rarityConfigs;
    private final List<String> recipe;
    private final Map<Character, Material> ingredients;

    public Pet(String id, ConfigurationSection config, ConfigurationSection raritiesSection) {
        this.id = id;
        this.displayName = config.getString("display-name", "§fPet");
        this.texture = config.getString("texture", "");
        this.description = config.getString("description", "A helpful companion.");

        String typeStr = config.getString("type", "FARMING").toUpperCase(Locale.ROOT);
        PetType parsedType;
        try {
            parsedType = PetType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            parsedType = PetType.FARMING;
        }
        this.type = parsedType;

        String rarityStr = config.getString("rarity", "COMMON").toUpperCase(Locale.ROOT);
        Rarity parsedRarity;
        try {
            parsedRarity = Rarity.valueOf(rarityStr);
        } catch (IllegalArgumentException e) {
            parsedRarity = Rarity.COMMON;
        }
        this.rarity = parsedRarity;

        this.rarityConfigs = new EnumMap<>(Rarity.class);
        ConfigurationSection rarityConfigsSection = config.getConfigurationSection("rarity-configs");

        // Default values for rarity configurations
        int defaultBaseXP = 100;
        double defaultXpMultiplier = 1.5;
        int defaultMaxLevel = 2;

        if (rarityConfigsSection != null) {
            for (Rarity r : Rarity.values()) {
                String rarityKey = r.name();
                ConfigurationSection rarityConfig = rarityConfigsSection.getConfigurationSection(rarityKey);
                if (rarityConfig != null) {
                    int baseXP = rarityConfig.getInt("base-xp", defaultBaseXP);
                    double xpMultiplier = rarityConfig.getDouble("xp-multiplier", defaultXpMultiplier);
                    int maxLevel = rarityConfig.getInt("max-level", defaultMaxLevel);
                    rarityConfigs.put(r, new RarityConfig(baseXP, xpMultiplier, maxLevel));
                } else {
                    // Use default values if the rarity configuration is not found
                    rarityConfigs.put(r, new RarityConfig(defaultBaseXP, defaultXpMultiplier, defaultMaxLevel));
                }
            }
        } else {
            // Use default values for all rarities if the rarity-configs section is not found
            for (Rarity r : Rarity.values()) {
                rarityConfigs.put(r, new RarityConfig(defaultBaseXP, defaultXpMultiplier, defaultMaxLevel));
            }
        }

        this.recipe = config.getStringList("recipe");
        this.ingredients = new HashMap<>();
        ConfigurationSection ingredientsSection = config.getConfigurationSection("ingredients");
        if (ingredientsSection != null) {
            for (String key : ingredientsSection.getKeys(false)) {
                if (key.length() != 1) {
                    Bukkit.getLogger().warning("Invalid ingredient key in pet '" + id + "': " + key);
                    continue;
                }
                String materialName = ingredientsSection.getString(key, "AIR").toUpperCase();
                Material material = Material.matchMaterial(materialName);
                if (material != null) {
                    ingredients.put(key.charAt(0), material);
                } else {
                    Bukkit.getLogger().warning("Invalid material in pet '" + id + "': " + materialName);
                }
            }
        }

        if (texture.isEmpty()) {
            throw new IllegalArgumentException("Missing texture for pet " + id);
        }

        this.buffValues = new LinkedHashMap<>();
        ConfigurationSection buffsSection = config.getConfigurationSection("buffs");
        if (buffsSection != null) {
            for (String key : buffsSection.getKeys(false)) {
                String upper = key.toUpperCase(Locale.ROOT);
                ConfigurationSection sub = buffsSection.getConfigurationSection(key);
                if (sub == null) {
                    continue;
                }

                if (upper.equals("SPREAD")) {
                    double chance = sub.getDouble("chance", 0.0);
                    double blocks = sub.getDouble("blocks", 0.0);
                    if (chance > 0) {
                        buffValues.put(BuffType.SPREAD_CHANCE, chance);
                    }
                    if (blocks > 0) {
                        buffValues.put(BuffType.SPREAD_COUNT, blocks);
                    }
                    continue;
                }

                try {
                    BuffType bt = BuffType.valueOf(upper);

                    if (bt.isPassive()) {
                        double chance = sub.getDouble("chance", 0.0);
                        if (chance > 0) {
                            buffValues.put(bt, chance);
                        }
                    } else {
                        double timer = sub.getDouble("timer", 0.0);
                        if (timer > 0) {
                            buffValues.put(bt, timer);
                        }
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public void assignRandomBuff() {
        Map<BuffType, Double> possibleBuffs = new HashMap<>();
        switch (type) {
            case FARMING:
                possibleBuffs.put(BuffType.DOUBLE_CROP_DROP, 20.0);
                possibleBuffs.put(BuffType.AUTO_REPLANT, 100.0);
                possibleBuffs.put(BuffType.FARM_HAND, 15.0);
                possibleBuffs.put(BuffType.TREASURE_HUNTER, 10.0);
                break;
            case FORAGING:
                possibleBuffs.put(BuffType.WOOD_FORTUNE, 20.0);
                possibleBuffs.put(BuffType.TREASURE_HUNTER, 10.0);
                possibleBuffs.put(BuffType.AREA_PICKUP, 15.0);
                break;
            case PVP:
                possibleBuffs.put(BuffType.ONE_SHOT, 5.0);
                possibleBuffs.put(BuffType.DOUBLE_MOB_DROPS, 25.0);
                possibleBuffs.put(BuffType.XP_BOOST, 15.0);
                possibleBuffs.put(BuffType.LOOTING_BOOST, 10.0);
                possibleBuffs.put(BuffType.SCAVENGER, 10.0);
                possibleBuffs.put(BuffType.LUCKY_KILL, 5.0);
                possibleBuffs.put(BuffType.MOB_REPELLENT, 15.0);
                possibleBuffs.put(BuffType.MOB_SLOW, 10.0);
                possibleBuffs.put(BuffType.FREEZE_STRIKE, 10.0);
                possibleBuffs.put(BuffType.SHIELD_WALL, 10.0);
                possibleBuffs.put(BuffType.BLOODLUST, 10.0);
                possibleBuffs.put(BuffType.DODGE, 10.0);
                possibleBuffs.put(BuffType.BURN_AURA, 10.0);
                possibleBuffs.put(BuffType.POISON_TOUCH, 10.0);
                possibleBuffs.put(BuffType.SOUL_COLLECTOR, 10.0);
                possibleBuffs.put(BuffType.REFLECT_DAMAGE, 10.0);
                possibleBuffs.put(BuffType.PET_HEAL, 10.0);
                break;
            case MINING:
                possibleBuffs.put(BuffType.AREA_MINE, 20.0);
                possibleBuffs.put(BuffType.INSTANT_SMELT, 15.0);
                possibleBuffs.put(BuffType.ORE_XRAY, 10.0);
                possibleBuffs.put(BuffType.MAGNETIC_MINING, 15.0);
                possibleBuffs.put(BuffType.GOLDEN_TOUCH, 10.0);
                possibleBuffs.put(BuffType.VEIN_MINER, 10.0);
                possibleBuffs.put(BuffType.TREASURE_HUNTER, 10.0);
                break;
        }

        if (!possibleBuffs.isEmpty()) {
            List<BuffType> buffTypes = new ArrayList<>(possibleBuffs.keySet());
            BuffType randomBuffType = buffTypes.get(new Random().nextInt(buffTypes.size()));
            double chance = possibleBuffs.get(randomBuffType);
            buffValues.put(randomBuffType, chance);
        }
    }

    public ItemStack createItem(int level) {
        return createItem(level, null);
    }

    public ItemStack createItem(int level, Rarity displayRarity) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", texture));
        meta.setPlayerProfile(profile);

        meta.displayName(Component.text(displayName).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();

        lore.add(Component.text("§7Type: §b" + type.name().substring(0, 1) + type.name().substring(1).toLowerCase())
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.text(""));

        for (Map.Entry<BuffType, Double> entry : buffValues.entrySet()) {
            BuffType bt = entry.getKey();
            double val = entry.getValue();
            String line;

            if (bt.isPassive()) {
                if (bt == BuffType.SPREAD_COUNT) {
                    line = "§7" + bt.getDisplayName() + ": §c" + (int) val + " blocks";
                } else {
                    line = "§7Chance for " + bt.getDisplayName() + ": §c" + val + "%";
                }
            } else {
                line = "§7" + bt.getDisplayName() + ": §c" + (int) val + " seconds";
            }

            lore.add(Component.text(line).decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.text(""));
        for (String wrapLine : wrapText(description, 30)) {
            lore.add(Component.text(wrapLine).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text(""));
        lore.add(Component.text(""));

        Rarity rarityToDisplay = (displayRarity != null) ? displayRarity : this.rarity;
        String rarityName = getRarityName(rarityToDisplay);
        lore.add(Component.text("§7Rarity: " + getRarityColor(rarityToDisplay) + rarityName)
                .decoration(TextDecoration.ITALIC, false));

        RarityConfig config = rarityConfigs.get(rarityToDisplay);
        lore.add(Component.text("§7Level: §e" + level + "/§e" + config.getMaxLevel())
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.text(""));
        lore.add(Component.text("§eRight-click to summon this pet")
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("§eLeft-click to open the settings menu")
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private List<String> wrapText(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;

        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder("§7");

        for (String w : words) {
            if (current.length() + w.length() + 1 > maxLength) {
                lines.add(current.toString());
                current = new StringBuilder("§7").append(w).append(" ");
            } else {
                current.append(w).append(" ");
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    public String getRarityColor(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> "§f";
            case UNCOMMON -> "§a";
            case RARE -> "§9";
            case EPIC -> "§5";
            case LEGENDARY -> "§6";
        };
    }

    public String getRarityName(Rarity rarity) {
        FileConfiguration raritiesConfig = ((CustomPets) Bukkit.getPluginManager().getPlugin("CustomPets")).getRaritiesConfig();
        ConfigurationSection raritiesSection = raritiesConfig.getConfigurationSection("rarities");
        if (raritiesSection != null) {
            String key = rarity.name().toLowerCase() + "-name";
            return raritiesSection.getString(key, rarity.name());
        }
        return rarity.name();
    }

    public int getBaseXP() {
        RarityConfig config = rarityConfigs.get(rarity);
        return config != null ? config.getBaseXP() : 100;
    }

    public double getXpMultiplier() {
        RarityConfig config = rarityConfigs.get(rarity);
        return config != null ? config.getXpMultiplier() : 1.5;
    }

    public int getMaxLevel() {
        RarityConfig config = rarityConfigs.get(rarity);
        return config != null ? config.getMaxLevel() : 2;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getTexture() { return texture; }
    public String getDescription() { return description; }
    public PetType getType() { return type; }
    public Rarity getRarity() { return rarity; }
    public void setRarity(Rarity rarity) { this.rarity = rarity; }
    public List<String> getRecipe() { return recipe; }
    public Map<Character, Material> getIngredients() { return ingredients; }
    public Map<BuffType, Double> getBuffValues() {
        return new LinkedHashMap<>(buffValues);
    }

    public enum PetType {
        MINING, FARMING, FORAGING, PVP
    }

    public enum Rarity {
        COMMON, UNCOMMON, RARE, EPIC, LEGENDARY
    }

    public static class RarityConfig {
        private final int baseXP;
        private final double xpMultiplier;
        private final int maxLevel;

        public RarityConfig(int baseXP, double xpMultiplier, int maxLevel) {
            this.baseXP = baseXP;
            this.xpMultiplier = xpMultiplier;
            this.maxLevel = maxLevel;
        }

        public int getBaseXP() {
            return baseXP;
        }

        public double getXpMultiplier() {
            return xpMultiplier;
        }

        public int getMaxLevel() {
            return maxLevel;
        }
    }
}

package carnage.customPets;

import carnage.customPets.Buffs.BuffListener;
import carnage.customPets.Commands.PetAscendCommand;
import carnage.customPets.Commands.PetCommand;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class CustomPets extends JavaPlugin {

    private static CustomPets instance;
    private PetManager petManager;
    private FileConfiguration buffsConfig;
    private FileConfiguration petsConfig;
    private FileConfiguration xpValuesConfig;
    private FileConfiguration raritiesConfig;

    @Override
    public void onEnable() {
        instance = this;

        // Save default configurations
        saveDefaultConfig();
        saveResource("buffs.yml", false);
        saveResource("pets.yml", false);
        saveResource("xp_values.yml", false);
        saveResource("rarities.yml", false);

        // Load configurations
        loadConfigs();

        petManager = new PetManager(this);
        PetXPListener petXPListener = new PetXPListener(this);

        getServer().getPluginManager().registerEvents(new PetListener(this), this);
        getServer().getPluginManager().registerEvents(new BuffListener(this), this);
        getServer().getPluginManager().registerEvents(petXPListener, this);
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onQuit(PlayerQuitEvent e) {
                petManager.handlePlayerQuit(e.getPlayer());
            }

            @EventHandler
            public void onJoin(PlayerJoinEvent e) {
                petManager.handlePlayerJoin(e.getPlayer());
            }
        }, this);

        getCommand("pets").setExecutor(new PetCommand(this));
        getCommand("petascend").setExecutor(new PetAscendCommand(this));

        // Register custom recipes
        CustomRecipes.registerRecipes(this, petManager.getPets());

        getLogger().info("CustomPets has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CustomPets has been disabled!");
    }

    public static CustomPets getInstance() {
        return instance;
    }

    public PetManager getPetManager() {
        return petManager;
    }

    private void loadConfigs() {
        // Load buffs configuration
        File buffsFile = new File(getDataFolder(), "buffs.yml");
        if (!buffsFile.exists()) {
            saveResource("buffs.yml", false);
        }
        buffsConfig = YamlConfiguration.loadConfiguration(buffsFile);

        // Load pets configuration
        File petsFile = new File(getDataFolder(), "pets.yml");
        if (!petsFile.exists()) {
            saveResource("pets.yml", false);
        }
        petsConfig = YamlConfiguration.loadConfiguration(petsFile);

        // Load XP values configuration
        File xpValuesFile = new File(getDataFolder(), "xp_values.yml");
        if (!xpValuesFile.exists()) {
            saveResource("xp_values.yml", false);
        }
        xpValuesConfig = YamlConfiguration.loadConfiguration(xpValuesFile);

        // Load rarities configuration
        File raritiesFile = new File(getDataFolder(), "rarities.yml");
        if (!raritiesFile.exists()) {
            saveResource("rarities.yml", false);
        }
        raritiesConfig = YamlConfiguration.loadConfiguration(raritiesFile);
    }

    public void reload() {
        // Reload all configurations
        reloadConfig();
        saveDefaultConfig();
        saveResource("buffs.yml", false);
        saveResource("pets.yml", false);
        saveResource("xp_values.yml", false);
        saveResource("rarities.yml", false);
        loadConfigs();

        petManager.reload();

        CustomRecipes.registerRecipes(this, petManager.getPets());

        BuffListener buffListener = new BuffListener(this);
        getServer().getPluginManager().registerEvents(buffListener, this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            PetEntity petEntity = petManager.getActivePet(player);
            if (petEntity != null) {
                petManager.spawnPet(player, petEntity.getPet().getId());
            }
        }
    }

    public void redeemPetForPlayer(UUID playerUUID, String petId) {
        Pet.Rarity rarity = determinePetRarity();
        String path = "redeemed." + playerUUID.toString() + "." + petId;
        getConfig().set(path, rarity.name());
        saveConfig();

        // Ensure the pet's rarity is set correctly in the PetManager
        PetManager petManager = getPetManager();
        Pet pet = petManager.getPets().get(petId);
        if (pet != null) {
            pet.setRarity(rarity);
        }
    }

    public List<String> getRedeemedPets(UUID playerUUID) {
        String base = "redeemed." + playerUUID.toString();
        Map<String, Object> section = getConfig().getConfigurationSection(base) == null
                ? Collections.emptyMap()
                : getConfig().getConfigurationSection(base).getValues(false);
        return new ArrayList<>(section.keySet());
    }

    public Pet.Rarity getRedeemedPetRarity(UUID playerUUID, String petId) {
        FileConfiguration config = getConfig();

        String playerPath = "player-pets." + playerUUID + "." + petId + ".rarity";
        String playerRarityStr = config.getString(playerPath);
        if (playerRarityStr != null) {
            try {
                return Pet.Rarity.valueOf(playerRarityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid player rarity value for pet " + petId + ": " + playerRarityStr);
            }
        }

        String redeemedPath = "redeemed." + playerUUID.toString() + "." + petId;
        String redeemedRarityStr = config.getString(redeemedPath);
        if (redeemedRarityStr != null) {
            try {
                return Pet.Rarity.valueOf(redeemedRarityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid redeemed rarity value for pet " + petId + ": " + redeemedRarityStr);
            }
        }

        return Pet.Rarity.COMMON;
    }

    public Pet.Rarity determinePetRarity() {
        FileConfiguration raritiesConfig = getRaritiesConfig();
        ConfigurationSection rarityChances = raritiesConfig.getConfigurationSection("rarity-crafting-chances");

        if (rarityChances == null) {
            getLogger().warning("No 'rarity-crafting-chances' section found in rarities config! Using default chances.");
            return Pet.Rarity.COMMON;
        }

        Map<Pet.Rarity, Integer> chances = new EnumMap<>(Pet.Rarity.class);
        int totalChance = 0;

        // Load the chances for each rarity
        for (String key : rarityChances.getKeys(false)) {
            try {
                Pet.Rarity rarity = Pet.Rarity.valueOf(key.toUpperCase());
                int chance = rarityChances.getInt(key);
                chances.put(rarity, chance);
                totalChance += chance;
            } catch (IllegalArgumentException ignored) {
                getLogger().warning("Invalid rarity key in 'rarity-crafting-chances': " + key);
            }
        }

        // If there's a rarity with a 100% chance, return it immediately
        for (Map.Entry<Pet.Rarity, Integer> entry : chances.entrySet()) {
            if (entry.getValue() == 100) {
                return entry.getKey();
            }
        }

        // If total chances don't add up to 100, normalize them
        if (totalChance != 100) {
            getLogger().warning("Rarity chances don't sum to 100 (sum is " + totalChance + "). Normalizing probabilities.");

            if (totalChance > 0) {
                Map<Pet.Rarity, Integer> normalizedChances = new EnumMap<>(Pet.Rarity.class);
                for (Map.Entry<Pet.Rarity, Integer> entry : chances.entrySet()) {
                    int normalizedChance = (int) Math.round((entry.getValue() * 100.0) / totalChance);
                    normalizedChances.put(entry.getKey(), normalizedChance);
                }
                chances = normalizedChances;
            } else {
                chances.clear();
                chances.put(Pet.Rarity.COMMON, 70);
                chances.put(Pet.Rarity.UNCOMMON, 20);
                chances.put(Pet.Rarity.RARE, 10);
            }
        }

        // Otherwise, use the weighted random selection
        int totalWeight = chances.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) {
            getLogger().warning("Total weight for rarity chances is <= 0. Defaulting to COMMON.");
            return Pet.Rarity.COMMON;
        }

        Random random = new Random();
        int randomValue = random.nextInt(totalWeight);
        int cumulativeWeight = 0;

        for (Map.Entry<Pet.Rarity, Integer> entry : chances.entrySet()) {
            cumulativeWeight += entry.getValue();
            if (randomValue < cumulativeWeight) {
                return entry.getKey();
            }
        }

        return Pet.Rarity.COMMON;
    }

    public FileConfiguration getBuffsConfig() {
        return buffsConfig;
    }

    public FileConfiguration getPetsConfig() {
        return petsConfig;
    }

    public FileConfiguration getXpValuesConfig() {
        return xpValuesConfig;
    }

    public FileConfiguration getRaritiesConfig() {
        return raritiesConfig;
    }
}

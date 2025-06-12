package carnage.customPets;

import carnage.customPets.Buffs.BuffListener;
import carnage.customPets.Commands.PetAscendCommand;
import carnage.customPets.Commands.PetCommand;
import carnage.customPets.PetItem.AbilityItemListener;
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
        getServer().getPluginManager().registerEvents(new AbilityItemListener(this), this);
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
        loadConfigs();

        // Reload the PetManager to apply the new configurations
        petManager.reload();

        // Re-register custom recipes with the updated pets
        CustomRecipes.registerRecipes(this, petManager.getPets());

        // Reinitialize BuffListener with updated configurations
        BuffListener buffListener = new BuffListener(this);
        getServer().getPluginManager().registerEvents(buffListener, this);

        // Notify all online players that the configurations have been reloaded
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

        // 1. Check player-pets section first
        String playerPath = "player-pets." + playerUUID + "." + petId + ".rarity";
        String playerRarityStr = config.getString(playerPath);
        if (playerRarityStr != null) {
            try {
                return Pet.Rarity.valueOf(playerRarityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid player rarity value for pet " + petId + ": " + playerRarityStr);
            }
        }

        // 2. Check redeemed section
        String redeemedPath = "redeemed." + playerUUID.toString() + "." + petId;
        String redeemedRarityStr = config.getString(redeemedPath);
        if (redeemedRarityStr != null) {
            try {
                return Pet.Rarity.valueOf(redeemedRarityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid redeemed rarity value for pet " + petId + ": " + redeemedRarityStr);
            }
        }

        // 3. Return UNCOMMON as default
        return Pet.Rarity.UNCOMMON;
    }

    public Pet.Rarity determinePetRarity() {
        ConfigurationSection rarityChances = raritiesConfig.getConfigurationSection("rarity-crafting-chances");
        Map<Pet.Rarity, Integer> chances = new EnumMap<>(Pet.Rarity.class);

        // Load the chances for each rarity
        for (String key : rarityChances.getKeys(false)) {
            try {
                Pet.Rarity rarity = Pet.Rarity.valueOf(key.toUpperCase());
                int chance = rarityChances.getInt(key);
                chances.put(rarity, chance);
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

        // Otherwise, use the weighted random selection
        int totalWeight = chances.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) {
            return Pet.Rarity.COMMON; // Default to UNCOMMON if no valid chances are found
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

        // Fallback to UNCOMMON if something unexpected happens
        return Pet.Rarity.UNCOMMON;
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

package carnage.customPets;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class PetManager implements Listener {
    private final Plugin plugin;
    private final Map<UUID, PetEntity> activePets;
    private final Map<UUID, PetEntity> persistentPets;
    private final Map<String, Pet> pets;
    private final NamespacedKey petKey;
    private final Map<UUID, Map<String, PetSettings>> petSettingsMap;

    public PetManager(Plugin plugin) {
        this.plugin = plugin;
        this.activePets = new HashMap<>();
        this.persistentPets = new HashMap<>();
        this.pets = new HashMap<>();
        this.petSettingsMap = new HashMap<>();
        this.petKey = new NamespacedKey(plugin, "pet-type");
        loadPets();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void loadPets() {
        FileConfiguration petsConfig = ((CustomPets) plugin).getPetsConfig();
        ConfigurationSection petsSection = petsConfig.getConfigurationSection("pets");
        if (petsSection == null) {
            plugin.getLogger().warning("No 'pets' section found in config!");
            return;
        }

        FileConfiguration raritiesConfig = ((CustomPets) plugin).getRaritiesConfig();
        ConfigurationSection raritiesSection = raritiesConfig.getConfigurationSection("rarities");
        if (raritiesSection == null) {
            plugin.getLogger().warning("No 'rarities' section found in config! Using default names.");
            raritiesSection = raritiesConfig.createSection("rarities");
        }

        for (String petId : petsSection.getKeys(false)) {
            try {
                ConfigurationSection petConfig = petsSection.getConfigurationSection(petId);
                if (petConfig != null) {
                    pets.put(petId, new Pet(petId, petConfig, raritiesSection));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load pet " + petId + ": " + e.getMessage());
            }
        }
    }

    public void handlePlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        PetEntity petEntity = activePets.get(uuid);

        if (petEntity != null) {
            savePetData(uuid, petEntity);

            boolean despawnOnQuit = plugin.getConfig().getBoolean("pets.despawn-on-quit", false);
            if (despawnOnQuit) {
                petEntity.remove();
            } else {
                petEntity.pauseFollowing();
                persistentPets.put(uuid, petEntity);
            }
            activePets.remove(uuid);
        }
    }

    public void handlePlayerJoin(Player player) {
        UUID uuid = player.getUniqueId();

        PetEntity petEntity = persistentPets.remove(uuid);
        if (petEntity != null) {
            petEntity.resumeFollowing(player);
            activePets.put(uuid, petEntity);
            return;
        }

        FileConfiguration config = plugin.getConfig();
        String playerBase = "player-pets." + uuid.toString();

        ConfigurationSection playerSection = config.getConfigurationSection(playerBase);
        if (playerSection == null) return;

        for (String petId : playerSection.getKeys(false)) {
            if (!pets.containsKey(petId)) continue;

            String basePath = playerBase + "." + petId + ".";

            boolean visible = config.getBoolean(basePath + "visible", true);
            boolean floating = config.getBoolean(basePath + "floating", true);
            String customName = config.getString(basePath + "custom-name", null);
            boolean visibleToOthers = config.getBoolean(basePath + "visible-to-others", true);
            int level = config.getInt(basePath + "level", 1);
            int xp = config.getInt(basePath + "xp", 0);
            String rarityStr = config.getString(basePath + "rarity", null);

            PetEntity newEntity = new PetEntity(plugin, player, pets.get(petId));
            if (!visible) newEntity.setVisible(false);
            if (!floating) newEntity.setFloatingEnabled(false);
            if (customName != null && !customName.isEmpty()) {
                Component comp = LegacyComponentSerializer.legacySection().deserialize(customName);
                newEntity.getArmorStand().customName(comp);
            }
            if (!visibleToOthers) newEntity.setGloballyVisible(false);

            PetSettings settings = new PetSettings();
            settings.setVisible(visible);
            settings.setFloatingEnabled(floating);
            settings.setCustomName(customName);
            settings.setVisibleToOthers(visibleToOthers);
            settings.setLevel(level);
            settings.setXp(xp);
            if (rarityStr != null) {
                try {
                    settings.setCustomRarity(Pet.Rarity.valueOf(rarityStr.toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
            }

            petSettingsMap.computeIfAbsent(uuid, k -> new HashMap<>()).put(petId, settings);

            activePets.put(uuid, newEntity);
            break;
        }
    }

    public void spawnPet(Player player, String petId) {
        removePet(player);

        Pet pet = pets.get(petId);
        if (pet == null) return;

        UUID uuid = player.getUniqueId();
        PetEntity petEntity = new PetEntity(plugin, player, pet);

        PetSettings settings = getSettings(uuid, petId);
        if (settings != null) {
            if (!settings.isVisible()) petEntity.setVisible(false);
            if (!settings.isFloatingEnabled()) petEntity.setFloatingEnabled(false);
            String customName = settings.getCustomName();
            if (customName != null && !customName.isEmpty()) {
                Component comp = LegacyComponentSerializer.legacySection().deserialize(customName);
                petEntity.getArmorStand().customName(comp);
            }
            if (!settings.isVisibleToOthers()) petEntity.setGloballyVisible(false);
            if (settings.getCustomRarity() != null) pet.setRarity(settings.getCustomRarity());
        }

        activePets.put(uuid, petEntity);
        savePetData(uuid, petEntity);
    }

    public void removePet(Player player) {
        UUID uuid = player.getUniqueId();
        PetEntity petEntity = activePets.remove(uuid);
        if (petEntity != null) {
            clearSavedPet(uuid, petEntity.getPet().getId());
            petEntity.remove();
        }
        petEntity = persistentPets.remove(uuid);
        if (petEntity != null) {
            clearSavedPet(uuid, petEntity.getPet().getId());
            petEntity.remove();
        }
    }

    public boolean hasPetActive(Player player) {
        return activePets.containsKey(player.getUniqueId());
    }

    public PetEntity getActivePet(Player player) {
        return activePets.get(player.getUniqueId());
    }

    public Map<String, Pet> getPets() {
        return new HashMap<>(pets);
    }

    public void reload() {
        cleanupAllPets();
        plugin.reloadConfig();
        loadPets();
    }

    public NamespacedKey getPetKey() {
        return petKey;
    }

    public void cleanupAllPets() {
        activePets.values().forEach(PetEntity::remove);
        persistentPets.values().forEach(PetEntity::remove);
        activePets.clear();
        persistentPets.clear();
    }

    public ItemStack createPetItem(String petId) {
        Pet pet = pets.get(petId);
        if (pet == null) return null;

        ItemStack item = pet.createItem(1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(petKey, PersistentDataType.STRING, petId);
            item.setItemMeta(meta);
        }
        return item;
    }

    public PetSettings getSettings(UUID playerUUID, String petId) {
        return petSettingsMap
                .computeIfAbsent(playerUUID, k -> new HashMap<>())
                .computeIfAbsent(petId, k -> {
                    FileConfiguration config = plugin.getConfig();
                    String basePath = "player-pets." + playerUUID + "." + petId + ".";

                    if (config.isSet(basePath + "pet-id")) {
                        PetSettings loaded = new PetSettings();
                        loaded.setVisible(config.getBoolean(basePath + "visible", true));
                        loaded.setFloatingEnabled(config.getBoolean(basePath + "floating", true));
                        loaded.setCustomName(config.getString(basePath + "custom-name", null));
                        loaded.setVisibleToOthers(config.getBoolean(basePath + "visible-to-others", true));
                        loaded.setLevel(config.getInt(basePath + "level", 1));
                        loaded.setXp(config.getInt(basePath + "xp", 0));
                        String rarityStr = config.getString(basePath + "rarity");
                        if (rarityStr != null) {
                            try {
                                loaded.setCustomRarity(Pet.Rarity.valueOf(rarityStr.toUpperCase()));
                            } catch (IllegalArgumentException ignored) {}
                        }
                        return loaded;
                    } else {
                        return new PetSettings();
                    }
                });
    }

    public void savePetData(UUID playerUUID, PetEntity petEntity) {
        PetSettings settings = getSettings(playerUUID, petEntity.getPet().getId());
        String petId = petEntity.getPet().getId();
        String basePath = "player-pets." + playerUUID + "." + petId + ".";

        plugin.getConfig().set(basePath + "pet-id", petId);
        plugin.getConfig().set(basePath + "visible", settings.isVisible());
        plugin.getConfig().set(basePath + "floating", settings.isFloatingEnabled());
        plugin.getConfig().set(basePath + "custom-name", settings.getCustomName());
        plugin.getConfig().set(basePath + "visible-to-others", settings.isVisibleToOthers());
        plugin.getConfig().set(basePath + "level", settings.getLevel());
        plugin.getConfig().set(basePath + "xp", settings.getXp());
        if (settings.getCustomRarity() != null) {
            plugin.getConfig().set(basePath + "rarity", settings.getCustomRarity().name());
        }
        plugin.saveConfig();
    }

    public void clearSavedPet(UUID playerUUID, String petId) {
        plugin.getConfig().set("player-pets." + playerUUID + "." + petId, null);
        plugin.saveConfig();
    }

    @EventHandler
    public void onServerReload(ServerLoadEvent event) {
        cleanupAllPets();
    }

    public void addXPToPet(UUID playerUUID, String petId, int xp) {
        PetSettings settings = getSettings(playerUUID, petId);
        settings.setXp(settings.getXp() + xp);

        Pet pet = pets.get(petId);
        if (pet != null) {
            int currentLevel = settings.getLevel();
            int requiredXP = calculateRequiredXP(pet, currentLevel);

            if (currentLevel < pet.getMaxLevel() && settings.getXp() >= requiredXP) {
                settings.setLevel(currentLevel + 1);
                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null) {
                    player.sendMessage("§aYour pet " + pet.getDisplayName() + " has leveled up to level " + (currentLevel + 1) + "!");
                }
            } else if (currentLevel >= pet.getMaxLevel()) {
                int requiredXPForMaxLevel = calculateRequiredXP(pet, pet.getMaxLevel());
                if (settings.getXp() >= requiredXPForMaxLevel) {
                    Player player = Bukkit.getPlayer(playerUUID);
                    if (player != null) {
                        TextComponent yesComponent = Component.text("[Yes]")
                                .color(NamedTextColor.GREEN)
                                .clickEvent(ClickEvent.runCommand("/petascend yes " + petId));
                        TextComponent noComponent = Component.text("[No]")
                                .color(NamedTextColor.RED)
                                .clickEvent(ClickEvent.runCommand("/petascend no " + petId));

                        Component message = Component.text("Do you wish to ascend your pet to the next rarity? ")
                                .append(yesComponent).append(Component.text(" ")).append(noComponent);

                        player.sendMessage(message);
                        player.setMetadata("awaitingPetAscension", new FixedMetadataValue(plugin, petId));
                    }
                }
            }
        }

        savePetData(playerUUID, activePets.get(playerUUID));
    }

    public void ascendPetRarity(UUID playerUUID, String petId) {
        PetSettings settings = getSettings(playerUUID, petId);
        Pet pet = pets.get(petId);

        if (pet != null) {
            Pet.Rarity currentRarity = settings.getCustomRarity() != null ? settings.getCustomRarity() : pet.getRarity();
            Pet.Rarity nextRarity = getNextRarity(currentRarity);

            if (nextRarity != null) {
                settings.setCustomRarity(nextRarity);
                pet.setRarity(nextRarity);

                // Reset level to 1 when ascending to a new rarity
                settings.setLevel(1);
                settings.setXp(0);

                // Assign a random buff based on the pet type
                pet.assignRandomBuff();

                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null) {
                    player.sendMessage("§aYour pet has been ascended to " + nextRarity.name() + "!");
                }

                savePetData(playerUUID, activePets.get(playerUUID));
            } else {
                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null) {
                    player.sendMessage("§cYour pet has reached the highest rarity and cannot be ascended further.");
                }
            }
        }
    }

    private Pet.Rarity getNextRarity(Pet.Rarity currentRarity) {
        return switch (currentRarity) {
            case COMMON -> Pet.Rarity.UNCOMMON;
            case UNCOMMON -> Pet.Rarity.RARE;
            case RARE -> Pet.Rarity.EPIC;
            case EPIC -> Pet.Rarity.LEGENDARY;
            case LEGENDARY -> null; // No further ascension possible
            default -> null;
        };
    }

    private int calculateRequiredXP(Pet pet, int currentLevel) {
        return (int) (pet.getBaseXP() * Math.pow(pet.getXpMultiplier(), currentLevel - 1));
    }

    public static class PetSettings {
        private boolean visible = true;
        private boolean floatingEnabled = true;
        private String customName = null;
        private boolean visibleToOthers = true;
        private int level = 1;
        private int xp = 0;
        private Pet.Rarity customRarity = null;

        public boolean isVisible() { return visible; }
        public void setVisible(boolean visible) { this.visible = visible; }

        public boolean isFloatingEnabled() { return floatingEnabled; }
        public void setFloatingEnabled(boolean floatingEnabled) { this.floatingEnabled = floatingEnabled; }

        public String getCustomName() { return customName; }
        public void setCustomName(String customName) { this.customName = customName; }

        public boolean isVisibleToOthers() { return visibleToOthers; }
        public void setVisibleToOthers(boolean visibleToOthers) { this.visibleToOthers = visibleToOthers; }

        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }

        public int getXp() { return xp; }
        public void setXp(int xp) { this.xp = xp; }

        public Pet.Rarity getCustomRarity() { return customRarity; }
        public void setCustomRarity(Pet.Rarity customRarity) { this.customRarity = customRarity; }
    }
}

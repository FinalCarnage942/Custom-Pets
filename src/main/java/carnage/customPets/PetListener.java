package carnage.customPets;

import carnage.customPets.GUIs.PetSettingsGUI;
import carnage.customPets.GUIs.PetGUI;
import carnage.customPets.GUIs.PetSelectionGUI;
import carnage.customPets.GUIs.ShareExpGUI;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class PetListener implements Listener {
    private final CustomPets plugin;
    public static final ItemStack DESPAWN_ITEM;
    private final Map<UUID, PetEntity> renamingPets;

    static {
        DESPAWN_ITEM = new ItemStack(Material.BARRIER);
        ItemMeta meta = DESPAWN_ITEM.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Despawn Pet", NamedTextColor.RED));
            meta.lore(Arrays.asList(
                    Component.text("Click to remove your active pet", NamedTextColor.GRAY),
                    Component.text("No pet will be following you", NamedTextColor.GRAY)
            ));
            DESPAWN_ITEM.setItemMeta(meta);
        }
    }

    public PetListener(CustomPets plugin) {
        this.plugin = plugin;
        this.renamingPets = new HashMap<>();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());

        if (title.equals("§aSelect Your Pet")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            if (clicked.isSimilar(DESPAWN_ITEM)) {
                if (plugin.getPetManager().hasPetActive(player)) {
                    plugin.getPetManager().removePet(player);
                    player.sendMessage("§cYour pet has been despawned");
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.5f);
                } else {
                    player.sendMessage("§cYou don't have a pet active");
                }
                player.closeInventory();
                return;
            }

            int rawSlot = event.getRawSlot();
            if (rawSlot == 47) {
                event.setCancelled(true);
                player.sendMessage("§eYour pet's current level is displayed above.");
                return;
            }
            if (rawSlot == 51) {
                event.setCancelled(true);
                PetEntity activePet = plugin.getPetManager().getActivePet(player);
                if (activePet != null) {
                    String activeId = activePet.getPet().getId();
                    new ShareExpGUI(plugin, player, activeId).open();
                } else {
                    player.sendMessage("§cNo active pet to share EXP from.");
                }
                return;
            }

            String petId = clicked.getItemMeta()
                    .getPersistentDataContainer()
                    .get(plugin.getPetManager().getPetKey(), PersistentDataType.STRING);
            if (petId == null) return;

            ClickType clickType = event.getClick();
            if (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT) {
                if (plugin.getPetManager().hasPetActive(player)) {
                    PetEntity active = plugin.getPetManager().getActivePet(player);
                    if (active.getPet().getId().equals(petId)) {
                        PetSettingsGUI.open(player);
                    } else {
                        player.sendMessage("§cThat pet is not currently summoned. Summon it first to configure settings.");
                    }
                } else {
                    player.sendMessage("§cYou don't have a pet summoned to configure. Right-click to summon first.");
                }
                return;
            }

            if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
                player.closeInventory();
                plugin.getPetManager().spawnPet(player, petId);

                removePetFromShareExp(player, petId);

                PetManager.PetSettings settings = plugin.getPetManager()
                        .getSettings(player.getUniqueId(), petId);
                String displayName = (settings.getCustomName() != null && !settings.getCustomName().isEmpty())
                        ? settings.getCustomName()
                        : plugin.getPetManager().getPets().get(petId).getDisplayName();

                player.sendMessage("§aSelected " + displayName + "§a!");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
            }
        } else if (title.equals("§bPet Settings")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            PetEntity activePet = plugin.getPetManager().getActivePet(player);
            if (activePet == null) {
                player.sendMessage("§cNo active pet to configure.");
                player.closeInventory();
                return;
            }

            String petId = activePet.getPet().getId();
            boolean allowRename = plugin.getConfig().getBoolean("pets.allow-rename", true);
            int slot = event.getRawSlot();

            if (slot == 11) {
                boolean newVis = !activePet.isVisible();
                activePet.setVisible(newVis);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                player.sendMessage(newVis
                        ? "§aPet name visibility enabled."
                        : "§cPet name visibility disabled.");

                PetManager.PetSettings settings = plugin.getPetManager()
                        .getSettings(player.getUniqueId(), petId);
                settings.setVisible(newVis);
                plugin.getPetManager().savePetData(player.getUniqueId(), activePet);
            } else if (slot == 13) {
                if (allowRename) {
                    player.closeInventory();
                    player.sendMessage("§aPlease type the new name for your pet in the chat.");
                    renamingPets.put(player.getUniqueId(), activePet);
                } else {
                    player.sendMessage("§cPet renaming is disabled.");
                }
            } else if (slot == 15) {
                boolean newFloat = !activePet.isFloatingEnabled();
                activePet.setFloatingEnabled(newFloat);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                player.sendMessage(newFloat
                        ? "§aPet floating enabled."
                        : "§cPet floating disabled.");

                PetManager.PetSettings settings = plugin.getPetManager()
                        .getSettings(player.getUniqueId(), petId);
                settings.setFloatingEnabled(newFloat);
                plugin.getPetManager().savePetData(player.getUniqueId(), activePet);
            } else if (slot == 22) {
                player.closeInventory();
                new PetGUI(plugin, player).open();
            } else if (slot == 9) {
                boolean nowVisible = !activePet.isGloballyVisible();
                activePet.setGloballyVisible(nowVisible);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 1.0f, 1.2f);
                player.sendMessage(nowVisible
                        ? "§aYour pet is now visible to everyone."
                        : "§cYour pet is now hidden from all other players.");

                PetManager.PetSettings settings = plugin.getPetManager()
                        .getSettings(player.getUniqueId(), petId);
                settings.setVisibleToOthers(nowVisible);
                plugin.getPetManager().savePetData(player.getUniqueId(), activePet);
            }
        } else if (title.equals("§bShare EXP")) {
            event.setCancelled(true);
            ShareExpGUI shareExpGUI = (ShareExpGUI) event.getInventory().getHolder();
            shareExpGUI.handleClick(event);
        } else if (title.equals("§aSelect Pet for EXP Share")) {
            event.setCancelled(true);
            PetSelectionGUI petSelectionGUI = (PetSelectionGUI) event.getInventory().getHolder();
            petSelectionGUI.handleClick(event);
        }
    }

    private void removePetFromShareExp(Player player, String petId) {
        List<String> selectedPetIds = getSelectedPetIds(player);
        if (selectedPetIds.contains(petId)) {
            selectedPetIds.remove(petId);
            saveSelectedPetIds(player, selectedPetIds);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getSelectedPetIds(Player player) {
        if (player.hasMetadata("selectedPetIds")) {
            return (List<String>) player.getMetadata("selectedPetIds").get(0).value();
        }
        return new ArrayList<>();
    }

    private void saveSelectedPetIds(Player player, List<String> selectedPetIds) {
        player.setMetadata("selectedPetIds", new FixedMetadataValue(plugin, selectedPetIds));
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        PetEntity petEntity = renamingPets.remove(player.getUniqueId());
        if (petEntity != null) {
            event.setCancelled(true);

            String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
            String oldLegacy = petEntity.getLegacyCustomName();
            String colorCode = "";
            if (oldLegacy.length() >= 2 && oldLegacy.charAt(0) == '§') {
                colorCode = oldLegacy.substring(0, 2);
            }
            String newNameLegacy = colorCode + plain;

            Component comp = LegacyComponentSerializer.legacySection().deserialize(newNameLegacy);
            petEntity.getArmorStand().customName(comp);

            player.sendMessage("§aPet renamed to: " + newNameLegacy);

            String petId = petEntity.getPet().getId();
            PetManager.PetSettings settings = plugin.getPetManager()
                    .getSettings(player.getUniqueId(), petId);
            settings.setCustomName(newNameLegacy);
            plugin.getPetManager().savePetData(player.getUniqueId(), petEntity);
        }

        if (player.hasMetadata("awaitingPetAscension")) {
            event.setCancelled(true);
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            String petId = player.getMetadata("awaitingPetAscension").get(0).asString();
            if (message.equalsIgnoreCase("yes")) {
                plugin.getPetManager().ascendPetRarity(player.getUniqueId(), petId);
                player.sendMessage("§aYour pet has been ascended to the next rarity!");
            } else if (message.equalsIgnoreCase("no")) {
                player.sendMessage("§cPet ascension cancelled.");
            }
            player.removeMetadata("awaitingPetAscension", plugin);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        String petId = item.getItemMeta()
                .getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "pet-redeem"), PersistentDataType.STRING);

        if (petId != null) {
            PetManager pm = plugin.getPetManager();
            Pet pet = pm.getPets().get(petId);

            // Determine the rarity based on crafting chances
            Pet.Rarity rarity = plugin.determinePetRarity();

            // Set the rarity of the pet
            pet.setRarity(rarity);

            // Save the rarity for the player
            String path = "redeemed." + player.getUniqueId() + "." + petId;
            plugin.getConfig().set(path, rarity.name());
            plugin.saveConfig();

            plugin.redeemPetForPlayer(player.getUniqueId(), petId);

            // Create and reward the pet
            player.sendMessage("§aYou have redeemed the pet: §6" + pet.getDisplayName() + " §7(" + getRarityName(rarity) + ")");

            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                player.getInventory().removeItem(item);
            }
            event.setCancelled(true);
        }
    }

    private String getRarityName(Pet.Rarity rarity) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection rarityNames = config.getConfigurationSection("rarities");
        if (rarityNames != null) {
            String key = rarity.name().toLowerCase() + "-name";
            return rarityNames.getString(key, rarity.name());
        }
        return rarity.name();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPetManager().handlePlayerQuit(event.getPlayer());
    }

    public void distributeXP(Player player, int xp) {
        PetEntity activePetEntity = plugin.getPetManager().getActivePet(player);
        if (activePetEntity == null) return;

        String activePetId = activePetEntity.getPet().getId();
        List<String> selectedPetIds = getSelectedPetIds(player);
        int numberOfPets = selectedPetIds.size();

        PetManager petManager = plugin.getPetManager();
        petManager.addXPToPet(player.getUniqueId(), activePetId, xp);

        if (numberOfPets > 0) {
            int xpPerPet = xp / 3;
            for (String petId : selectedPetIds) {
                petManager.addXPToPet(player.getUniqueId(), petId, xpPerPet);
            }
        }
    }
}

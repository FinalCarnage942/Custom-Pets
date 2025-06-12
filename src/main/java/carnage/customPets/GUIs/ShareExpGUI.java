package carnage.customPets.GUIs;

import carnage.customPets.CustomPets;
import carnage.customPets.PetManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ShareExpGUI implements org.bukkit.inventory.InventoryHolder {
    private final CustomPets plugin;
    private final Player player;
    private final String activePetId;
    private Inventory inventory;

    public ShareExpGUI(CustomPets plugin, Player player, String activePetId) {
        this.plugin = plugin;
        this.player = player;
        this.activePetId = activePetId;
        this.inventory = Bukkit.createInventory(
                this,
                36,
                Component.text("§bShare EXP").decoration(TextDecoration.ITALIC, false)
        );
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @SuppressWarnings("unchecked")
    private List<String> getSelectedPetIds() {
        if (player.hasMetadata("selectedPetIds")) {
            return (List<String>) player.getMetadata("selectedPetIds").get(0).value();
        }
        return new ArrayList<>();
    }

    private void saveSelectedPetIds(List<String> selectedPetIds) {
        player.setMetadata("selectedPetIds", new FixedMetadataValue(plugin, selectedPetIds));
    }

    public void open() {
        List<String> selectedPetIds = getSelectedPetIds();

        // Fill the entire inventory with a gray border first
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        border.setItemMeta(borderMeta);

        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, border);
        }

        // Set red stained glass panes in the slots where pets can be placed
        ItemStack redGlassPane = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta redGlassMeta = redGlassPane.getItemMeta();
        redGlassMeta.displayName(Component.text("§cSelect a Pet").decoration(TextDecoration.ITALIC, false));
        redGlassPane.setItemMeta(redGlassMeta);

        inventory.setItem(11, redGlassPane);
        inventory.setItem(13, redGlassPane);
        inventory.setItem(15, redGlassPane);

        // Display selected pets
        for (int i = 0; i < selectedPetIds.size(); i++) {
            String petId = selectedPetIds.get(i);
            ItemStack petItem = plugin.getPetManager().createPetItem(petId);
            ItemMeta meta = petItem.getItemMeta();

            PetManager.PetSettings settings = plugin.getPetManager().getSettings(player.getUniqueId(), petId);
            String customName = settings.getCustomName();
            if (customName != null && !customName.isEmpty()) {
                meta.displayName(Component.text(customName).decoration(TextDecoration.ITALIC, false));
            }

            // Add XP information to the lore
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Level: §e" + settings.getLevel()).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("§7XP: §e" + settings.getXp()).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);

            petItem.setItemMeta(meta);
            inventory.setItem(11 + i * 2, petItem);
        }

        // Add "Remove Pet" buttons under the red glass panes
        ItemStack removeButton = new ItemStack(Material.BARRIER);
        ItemMeta removeMeta = removeButton.getItemMeta();
        removeMeta.displayName(Component.text("§cRemove Pet").decoration(TextDecoration.ITALIC, false));
        removeButton.setItemMeta(removeMeta);

        inventory.setItem(20, removeButton);
        inventory.setItem(22, removeButton);
        inventory.setItem(24, removeButton);

        player.openInventory(inventory);
    }

    public void addSelectedPet(String petId) {
        List<String> selectedPetIds = getSelectedPetIds();
        if (!selectedPetIds.contains(petId) && selectedPetIds.size() < 3) {
            selectedPetIds.add(petId);
            saveSelectedPetIds(selectedPetIds);
        }
    }

    public void removeSelectedPet(String petId) {
        List<String> selectedPetIds = getSelectedPetIds();
        if (selectedPetIds.contains(petId)) {
            selectedPetIds.remove(petId);
            saveSelectedPetIds(selectedPetIds);
        }
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        ClickType clickType = event.getClick();

        // Handle red glass pane clicks to open pet selection GUI
        if (slot == 11 || slot == 13 || slot == 15) {
            if (clickType == ClickType.LEFT) {
                player.closeInventory();
                new PetSelectionGUI(plugin, player, activePetId, this).open();
            }
        }
        // Handle "Remove Pet" button clicks
        else if (slot == 20 || slot == 22 || slot == 24) {
            if (clickType == ClickType.LEFT) {
                int petSlot = slot - 9;
                ItemStack petItem = inventory.getItem(petSlot);
                if (petItem != null && petItem.getType() != Material.AIR) {
                    String petId = petItem.getItemMeta().getPersistentDataContainer()
                            .get(plugin.getPetManager().getPetKey(), PersistentDataType.STRING);
                    if (petId != null) {
                        removeSelectedPet(petId);
                        player.sendMessage("§cRemoved pet from EXP share.");
                        open(); // Refresh the GUI
                    }
                }
            }
        }
    }
}

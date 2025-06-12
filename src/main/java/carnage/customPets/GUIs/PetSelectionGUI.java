package carnage.customPets.GUIs;

import carnage.customPets.CustomPets;
import carnage.customPets.Pet;
import carnage.customPets.PetManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;

public class PetSelectionGUI implements org.bukkit.inventory.InventoryHolder {
    private final CustomPets plugin;
    private final Player player;
    private final String activePetId;
    private final ShareExpGUI shareExpGUI;
    private Inventory inventory;

    public PetSelectionGUI(CustomPets plugin, Player player, String activePetId, ShareExpGUI shareExpGUI) {
        this.plugin = plugin;
        this.player = player;
        this.activePetId = activePetId;
        this.shareExpGUI = shareExpGUI;
        this.inventory = Bukkit.createInventory(
                this,
                54,
                Component.text("§aSelect Pet for EXP Share").decoration(TextDecoration.ITALIC, false)
        );
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
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

        int[] petSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };
        PetManager petManager = plugin.getPetManager();
        List<String> redeemedPets = (List<String>) plugin.getRedeemedPets(player.getUniqueId());
        int slotIndex = 0;

        for (Map.Entry<String, Pet> entry : petManager.getPets().entrySet()) {
            if (slotIndex >= petSlots.length) break;
            String petId = entry.getKey();
            if (!redeemedPets.contains(petId) || petId.equals(activePetId)) continue;

            Pet petDef = entry.getValue();


            PetManager.PetSettings settings = petManager.getSettings(player.getUniqueId(), petId);
            ItemStack petItem = petDef.createItem(settings.getLevel());
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

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String petId = clicked.getItemMeta()
                .getPersistentDataContainer()
                .get(plugin.getPetManager().getPetKey(), PersistentDataType.STRING);
        if (petId == null) return;

        shareExpGUI.addSelectedPet(petId);
        shareExpGUI.open();
    }
}

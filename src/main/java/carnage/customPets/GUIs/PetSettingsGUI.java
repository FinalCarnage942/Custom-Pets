package carnage.customPets.GUIs;

import carnage.customPets.CustomPets;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PetSettingsGUI {

    public static void open(Player player) {
        CustomPets plugin = CustomPets.getInstance();
        boolean allowRename = plugin.getConfig().getBoolean("pets.allow-rename", true);

        Inventory gui = Bukkit.createInventory(null, 27, "§bPet Settings");

        // Toggle Name Visibility
        ItemStack toggleVisibility = new ItemStack(Material.ENDER_EYE);
        ItemMeta tvMeta = toggleVisibility.getItemMeta();
        tvMeta.displayName(Component.text("§eToggle Name Visibility").decoration(TextDecoration.ITALIC, false));
        toggleVisibility.setItemMeta(tvMeta);

        // Rename Pet or Placeholder if disabled
        ItemStack renamePet;
        ItemMeta rpMeta;
        if (allowRename) {
            renamePet = new ItemStack(Material.NAME_TAG);
            rpMeta = renamePet.getItemMeta();
            rpMeta.displayName(Component.text("§eRename Pet").decoration(TextDecoration.ITALIC, false));
            renamePet.setItemMeta(rpMeta);
        } else {
            renamePet = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            rpMeta = renamePet.getItemMeta();
            rpMeta.displayName(Component.text("§7Rename Disabled").decoration(TextDecoration.ITALIC, false));
            renamePet.setItemMeta(rpMeta);
        }

        // Toggle Floating
        ItemStack toggleFloat = new ItemStack(Material.FEATHER);
        ItemMeta tfMeta = toggleFloat.getItemMeta();
        tfMeta.displayName(Component.text("§eToggle Floating").decoration(TextDecoration.ITALIC, false));
        toggleFloat.setItemMeta(tfMeta);

        // Placeholder for remaining slots
        ItemStack placeholder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta phMeta = placeholder.getItemMeta();
        phMeta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        placeholder.setItemMeta(phMeta);

        // Fill entire inventory with placeholder first
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, placeholder);
        }

        // Set the three main options
        gui.setItem(11, toggleVisibility);
        gui.setItem(13, renamePet);
        gui.setItem(15, toggleFloat);

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text("§eBack").decoration(TextDecoration.ITALIC, false));
        back.setItemMeta(backMeta);
        gui.setItem(22, back);

        // Toggle Global Visibility
        ItemStack toggleGlobal = new ItemStack(Material.ENDER_EYE);
        ItemMeta tgMeta = toggleGlobal.getItemMeta();
        tgMeta.displayName(Component.text("§eToggle Global Visibility").decoration(TextDecoration.ITALIC, false));
        toggleGlobal.setItemMeta(tgMeta);
        gui.setItem(9, toggleGlobal);

        player.openInventory(gui);
    }
}

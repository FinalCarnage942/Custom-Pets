package carnage.customPets.PetItem;

import carnage.customPets.CustomPets;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class AbilityItem {
    private static final NamespacedKey ABILITY_ITEM_KEY = new NamespacedKey(CustomPets.getPlugin(CustomPets.class), "pet-ability-item");

    public static ItemStack createAbilityItem(String petName) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        // Add glow effect
        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // Set display name and lore
        meta.displayName(Component.text("§6" + petName + "'s Ability")
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(Arrays.asList(
                Component.text("§7Right-click to activate your pet's abilities")
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("§8Cooldown: §e30 seconds")
                        .decoration(TextDecoration.ITALIC, false)
        ));

        // Add persistent data tag
        meta.getPersistentDataContainer().set(ABILITY_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isAbilityItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(ABILITY_ITEM_KEY, PersistentDataType.BYTE);
    }
}
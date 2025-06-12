package carnage.customPets;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

public class SkullUtils {

    public static ItemStack getCustomSkull(String texture) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (texture == null || texture.isEmpty()) return head;

        SkullMeta meta = (SkullMeta) head.getItemMeta();

        // FIX: Use a non-null dummy name
        GameProfile profile = new GameProfile(UUID.randomUUID(), "PetSkin");
        profile.getProperties().put("textures", new Property("textures", texture));

        try {
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        head.setItemMeta(meta);
        return head;
    }
}

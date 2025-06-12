package carnage.customPets;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class CustomRecipes {

    private static final Set<NamespacedKey> registeredPetRecipes = new HashSet<>();

    public static void registerRecipes(Plugin plugin, Map<String, Pet> pets) {
        removePreviousRecipes(plugin);
        for (Pet pet : pets.values()) {
            createPetRecipe(plugin, pet);
        }
    }

    private static void removePreviousRecipes(Plugin plugin) {
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof ShapedRecipe shaped) {
                NamespacedKey key = shaped.getKey();
                if (registeredPetRecipes.contains(key)) {
                    iterator.remove(); // Remove old recipe
                    plugin.getLogger().info("Removed old pet recipe: " + key);
                }
            }
        }
        registeredPetRecipes.clear();
    }

    private static void createPetRecipe(Plugin plugin, Pet pet) {
        List<String> recipeShape = pet.getRecipe();
        if (recipeShape == null || recipeShape.isEmpty()) return;

        ItemStack petItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) petItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(pet.getDisplayName());
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", pet.getTexture()));
            meta.setPlayerProfile(profile);
            meta.setLore(List.of("§7Right-click to unlock this pet."));
            petItem.setItemMeta(meta);
        }

        ItemMeta itemMeta = petItem.getItemMeta();
        if (itemMeta != null) {
            itemMeta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "pet-redeem"),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    pet.getId()
            );
            petItem.setItemMeta(itemMeta);
        }

        NamespacedKey key = new NamespacedKey(plugin, pet.getId() + "_pet");
        ShapedRecipe petRecipe = new ShapedRecipe(key, petItem);
        petRecipe.shape(recipeShape.toArray(new String[0]));

        for (Map.Entry<Character, Material> entry : pet.getIngredients().entrySet()) {
            petRecipe.setIngredient(entry.getKey(), entry.getValue());
        }

        Bukkit.addRecipe(petRecipe);
        registeredPetRecipes.add(key);
        plugin.getLogger().info("Registered recipe for pet: " + pet.getId());
    }
}

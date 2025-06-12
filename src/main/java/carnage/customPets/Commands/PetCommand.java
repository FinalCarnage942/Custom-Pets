package carnage.customPets.Commands;

import carnage.customPets.CustomPets;
import carnage.customPets.GUIs.PetGUI;
import carnage.customPets.Pet;
import carnage.customPets.PetManager;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PetCommand implements CommandExecutor, TabCompleter {
    private final CustomPets plugin;
    private final Random random = new Random();

    public PetCommand(CustomPets plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§eUsage:");
            player.sendMessage("  §a/pets give <petId> §7→ Give redeemable pet");
            player.sendMessage("  §a/pets menu §7→ Open your pet menu");
            player.sendMessage("  §a/pets reload §7→ Reload config");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                if (!player.hasPermission("pets.give")) {
                    player.sendMessage("§cYou don’t have permission to give pets.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /pets give <petId>");
                    return true;
                }
                String petId = args[1];
                PetManager pm = plugin.getPetManager();
                if (!pm.getPets().containsKey(petId)) {
                    player.sendMessage("§cNo such pet ID: " + petId);
                    return true;
                }

                Pet pet = pm.getPets().get(petId);
                Pet.Rarity rarity = determineRarity(pet.getRarity());
                ItemStack redeemablePet = createRedeemablePetItem(pet, rarity);

                player.getInventory().addItem(redeemablePet);
                player.sendMessage("§aYou received a redeemable pet §6" + pet.getDisplayName() + "§a. Right-click it to unlock!");
                break;

            case "menu":
                new PetGUI(plugin, player).open();
                break;

            case "reload":
                if (!player.hasPermission("pets.reload")) {
                    player.sendMessage("§cYou don’t have permission to reload the plugin configuration.");
                    return true;
                }
                // Reload all configurations
                plugin.reload();
                player.sendMessage("§aConfig reloaded!");
                break;

            default:
                player.sendMessage("§cUnknown subcommand. Use §e/pets give <petId> §cor §e/pets menu");
        }
        return true;
    }

    private Pet.Rarity determineRarity(Pet.Rarity defaultRarity) {
        Map<String, Integer> craftingChances = plugin.getRaritiesConfig().getConfigurationSection("rarity-crafting-chances").getValues(false)
                .entrySet().stream().collect(
                        HashMap::new,
                        (map, entry) -> map.put(entry.getKey().toUpperCase(), Integer.parseInt(entry.getValue().toString())),
                        HashMap::putAll
                );

        int randomValue = random.nextInt(100);
        int cumulativeProbability = 0;

        for (Map.Entry<String, Integer> entry : craftingChances.entrySet()) {
            cumulativeProbability += entry.getValue();
            if (randomValue < cumulativeProbability) {
                try {
                    return Pet.Rarity.valueOf(entry.getKey().toUpperCase());
                } catch (IllegalArgumentException e) {
                    return defaultRarity;
                }
            }
        }

        return defaultRarity;
    }

    private ItemStack createRedeemablePetItem(Pet pet, Pet.Rarity rarity) {
        ItemStack redeemablePet = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) redeemablePet.getItemMeta();

        if (meta != null) {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", pet.getTexture()));
            meta.setPlayerProfile(profile);

            meta.setDisplayName("§eRedeem Pet: §a" + pet.getDisplayName());
            meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "pet-redeem"),
                    PersistentDataType.STRING,
                    pet.getId()
            );
            meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "pet-rarity"),
                    PersistentDataType.STRING,
                    rarity.name()
            );
            meta.setLore(Collections.singletonList("§7Right-click to unlock this pet."));
            redeemablePet.setItemMeta(meta);
        }

        return redeemablePet;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (player.hasPermission("pets.give")) {
                subs.add("give");
            }
            subs.add("menu");
            if (player.hasPermission("pets.reload")) {
                subs.add("reload");
            }

            List<String> ret = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) ret.add(s);
            }
            return ret;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give") && player.hasPermission("pets.give")) {
            List<String> petIds = new ArrayList<>(plugin.getPetManager().getPets().keySet());
            List<String> ret = new ArrayList<>();
            for (String id : petIds) {
                if (id.toLowerCase().startsWith(args[1].toLowerCase())) ret.add(id);
            }
            return ret;
        }
        return Collections.emptyList();
    }
}

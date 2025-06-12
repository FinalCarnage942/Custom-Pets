package carnage.customPets.PetItem;

import carnage.customPets.CustomPets;
import carnage.customPets.Buffs.BuffType;
import carnage.customPets.PetEntity;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class AbilityItemListener implements Listener {
    private final CustomPets plugin;

    public AbilityItemListener(CustomPets plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only trigger on right-click
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {

            Player player = event.getPlayer();
            ItemStack item = event.getItem();
            if (item == null || !AbilityItem.isAbilityItem(item)) return;

            PetEntity petEntity = plugin.getPetManager().getActivePet(player);
            if (petEntity == null) {
                player.sendMessage("§cYou have no active pet to use this ability.");
                return;
            }

            // Cooldown logic: use the same interval from config
            int interval = plugin.getConfig().getInt("pets.effect-interval", 5);
            UUID uuid = player.getUniqueId();
            String keyString = uuid.toString() + "-pet-ability-cooldown";
            long now = System.currentTimeMillis();

            long lastUsed = 0;
            if (player.getPersistentDataContainer().has(new NamespacedKey(plugin, keyString), PersistentDataType.LONG)) {
                lastUsed = player.getPersistentDataContainer()
                        .get(new NamespacedKey(plugin, keyString), PersistentDataType.LONG);
            }

            if (now - lastUsed < interval * 1000L) {
                long remaining = (interval * 1000L - (now - lastUsed)) / 1000;
                player.sendMessage("§cAbility on cooldown! " + remaining + "s remaining.");
                return;
            }

            // Apply buffs
            petEntity.triggerBuff();

            // Update cooldown
            player.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, keyString),
                    PersistentDataType.LONG,
                    now
            );

            event.setCancelled(true);
        }
    }
}

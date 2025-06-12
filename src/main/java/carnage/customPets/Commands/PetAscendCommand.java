package carnage.customPets.Commands;

import carnage.customPets.CustomPets;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PetAscendCommand implements CommandExecutor {

    private final CustomPets plugin;

    public PetAscendCommand(CustomPets plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length != 2) {
            player.sendMessage("Usage: /petascend <yes|no> <petId>");
            return true;
        }

        String response = args[0];
        String petId = args[1];

        if (response.equalsIgnoreCase("yes")) {
            plugin.getPetManager().ascendPetRarity(player.getUniqueId(), petId);
            player.sendMessage("§aYour pet has been ascended to the next rarity!");
        } else if (response.equalsIgnoreCase("no")) {
            player.sendMessage("§cPet ascension cancelled.");
        }

        player.removeMetadata("awaitingPetAscension", plugin);
        return true;
    }
}

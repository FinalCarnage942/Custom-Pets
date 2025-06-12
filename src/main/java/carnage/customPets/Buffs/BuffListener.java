package carnage.customPets.Buffs;

import carnage.customPets.CustomPets;
import carnage.customPets.PetEntity;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class BuffListener implements Listener {
    private final CustomPets plugin;
    private final Random random = new Random();
    private final Economy econ;
    private int scavengerMin, scavengerMax, scavengerTimer;
    private int luckyKillMin, luckyKillMax;
    private int oreXRayCooldownMin, oreXRayCooldownMax;
    private final Map<Material, Double> treasureMap = new LinkedHashMap<>();
    private final Map<UUID, Long> breakCooldown = new HashMap<>();
    private final Map<Material, Particle.DustOptions> oreParticles = new HashMap<>();

    public BuffListener(CustomPets plugin) {
        this.plugin = plugin;
        var reg = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        this.econ = reg != null ? reg.getProvider() : null;

        // Load configuration values from buffs.yml
        FileConfiguration buffsConfig = plugin.getBuffsConfig();

        ConfigurationSection scavengerSection = buffsConfig.getConfigurationSection("scavenger");
        if (scavengerSection != null) {
            this.scavengerMin = scavengerSection.getInt("min-amount", 5);
            this.scavengerMax = scavengerSection.getInt("max-amount", 5);
            this.scavengerTimer = scavengerSection.getInt("timer", 2); // Load the scavenger timer
        } else {
            this.scavengerMin = 1;
            this.scavengerMax = 5;
            this.scavengerTimer = 30;
        }

        ConfigurationSection luckyKillSection = buffsConfig.getConfigurationSection("lucky-kill");
        if (luckyKillSection != null) {
            this.luckyKillMin = luckyKillSection.getInt("min-amount", 1);
            this.luckyKillMax = luckyKillSection.getInt("max-amount", 5);
        } else {
            this.luckyKillMin = 1;
            this.luckyKillMax = 5;
        }

        ConfigurationSection oreXRaySection = buffsConfig.getConfigurationSection("ore-xray");
        if (oreXRaySection != null) {
            this.oreXRayCooldownMin = oreXRaySection.getInt("min-timer", 1);
            this.oreXRayCooldownMax = oreXRaySection.getInt("max-timer", 5);
        } else {
            this.oreXRayCooldownMin = 1;
            this.oreXRayCooldownMax = 5;
        }

        // Initialize ore particles with colors
        oreParticles.put(Material.COAL_ORE, new Particle.DustOptions(Color.BLACK, 1));
        oreParticles.put(Material.IRON_ORE, new Particle.DustOptions(Color.fromRGB(192, 192, 192), 1));
        oreParticles.put(Material.COPPER_ORE, new Particle.DustOptions(Color.fromRGB(222, 119, 72), 1));
        oreParticles.put(Material.GOLD_ORE, new Particle.DustOptions(Color.YELLOW, 1));
        oreParticles.put(Material.REDSTONE_ORE, new Particle.DustOptions(Color.RED, 1));
        oreParticles.put(Material.EMERALD_ORE, new Particle.DustOptions(Color.GREEN, 1));
        oreParticles.put(Material.LAPIS_ORE, new Particle.DustOptions(Color.BLUE, 1));
        oreParticles.put(Material.DIAMOND_ORE, new Particle.DustOptions(Color.AQUA, 1));

        // Load treasure hunter drops
        ConfigurationSection treasureSection = buffsConfig.getConfigurationSection("treasure-hunter-drops");
        if (treasureSection != null) {
            for (String key : treasureSection.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                double chance = treasureSection.getDouble(key + ".chance", 0.0) * 100.0;
                if (material != null && chance > 0) {
                    treasureMap.put(material, chance);
                    plugin.getLogger().info("Loaded treasure drop: " + material.name() + " with chance " + chance);
                } else {
                    plugin.getLogger().warning("Invalid material or chance for treasure-hunter-drops: " + key);
                }
            }
        } else {
            plugin.getLogger().warning("treasure-hunter-drops section not found in buffs config.");
        }

        // Recurring buffs
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    applyPotionBuffs(p);
                    applyFarmHand(p);
                    applyAreaPickup(p);
                    applyMagneticMining(p);
                }
            }
        }.runTaskTimer(plugin, 0, 5 * 20L);

        // Ore X-Ray task
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    applyOreXRay(p);
                }
            }
        }.runTaskTimer(plugin, 0, (oreXRayCooldownMin + random.nextInt(oreXRayCooldownMax - oreXRayCooldownMin + 1)) * 20L);

        // Scavenger task with the timer from the configuration
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    applyScavenger(p);
                }
            }
        }.runTaskTimer(plugin, 0, scavengerTimer * 20L);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }



    private void applyScavenger(Player p) {
        PetEntity pet = plugin.getPetManager().getActivePet(p);
        if (pet == null || econ == null) return;
        double c = pet.getPet().getBuffValues().getOrDefault(BuffType.SCAVENGER, 0.0);
        if (c > 0 && random.nextDouble() * 100 < c) {
            int amt = scavengerMin + random.nextInt(scavengerMax - scavengerMin + 1);
            econ.depositPlayer(p, amt);
            p.sendMessage("§aScavenger: +" + amt + " coins!");
        }
    }

    // Other methods remain unchanged
    private void applyPotionBuffs(Player p) {
        PetEntity pet = plugin.getPetManager().getActivePet(p);
        if (pet == null) return;
        var b = pet.getPet().getBuffValues();
        applyBuff(b, BuffType.SPEED, p, PotionEffectType.SPEED, 1);
        applyBuff(b, BuffType.JUMP_BOOST, p, PotionEffectType.JUMP_BOOST, 1);
        applyBuff(b, BuffType.STRENGTH, p, PotionEffectType.STRENGTH, 0);
        applyBuff(b, BuffType.REGENERATION, p, PotionEffectType.REGENERATION, 0);
        applyBuff(b, BuffType.FIRE_RESISTANCE, p, PotionEffectType.FIRE_RESISTANCE, 0);
        applyBuff(b, BuffType.INVISIBILITY, p, PotionEffectType.INVISIBILITY, 0);
        applyBuff(b, BuffType.NIGHT_VISION, p, PotionEffectType.NIGHT_VISION, 0);
        applyBuff(b, BuffType.WATER_BREATHING, p, PotionEffectType.WATER_BREATHING, 0);
        applyBuff(b, BuffType.HASTE, p, PotionEffectType.HASTE, 0);
        applyBuff(b, BuffType.ABSORPTION, p, PotionEffectType.ABSORPTION, 0);
        applyBuff(b, BuffType.SATURATION, p, PotionEffectType.SATURATION, 0);
        applyBuff(b, BuffType.RESISTANCE, p, PotionEffectType.RESISTANCE, 0);
        applyBuff(b, BuffType.HEALTH_BOOST, p, PotionEffectType.HEALTH_BOOST, 0);
        applyBuff(b, BuffType.SLOW_FALLING, p, PotionEffectType.SLOW_FALLING, 0);
        applyBuff(b, BuffType.CONDUIT_POWER, p, PotionEffectType.CONDUIT_POWER, 0);
        applyBuff(b, BuffType.HERO_OF_THE_VILLAGE, p, PotionEffectType.HERO_OF_THE_VILLAGE, 0);
    }

    private void applyBuff(Map<BuffType, Double> b, BuffType type, Player p, PotionEffectType effect, int amp) {
        double c = b.getOrDefault(type, 0.0);
        if (c > 0 && random.nextDouble() * 100 < c) {
            p.removePotionEffect(effect);
            p.addPotionEffect(new PotionEffect(effect, 6 * 20, amp, true, true));
        }
    }

    private void applyFarmHand(Player p) {
        PetEntity pet = plugin.getPetManager().getActivePet(p);
        if (pet == null) return;
        double c = pet.getPet().getBuffValues().getOrDefault(BuffType.FARM_HAND, 0.0);
        if (c > 0 && random.nextDouble() * 100 < c) acceleratePlants(p, 5, 1, 5);
    }

    private void applyAreaPickup(Player p) {
        PetEntity pet = plugin.getPetManager().getActivePet(p);
        if (pet == null) return;
        if (pet.getPet().getBuffValues().containsKey(BuffType.AREA_PICKUP)) {
            collectNearby(p, 5);
        }
    }

    private void applyMagneticMining(Player p) {
        PetEntity pet = plugin.getPetManager().getActivePet(p);
        if (pet == null) return;
        if (pet.getPet().getBuffValues().containsKey(BuffType.MAGNETIC_MINING)) {
            // nothing here; pickups happen in blockBreak
        }
    }

    private void applyOreXRay(Player p) {
        PetEntity pet = plugin.getPetManager().getActivePet(p);
        if (pet == null) return;
        double c = pet.getPet().getBuffValues().getOrDefault(BuffType.ORE_XRAY, 0.0);
        if (c > 0) {
            showNearbyOres(p);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && e.getEntity() instanceof Player p) {
            PetEntity pet = plugin.getPetManager().getActivePet(p);
            if (pet != null && pet.getPet().getBuffValues().getOrDefault(BuffType.NO_FALL, 0.0) > 0) {
                e.setCancelled(true);
                p.sendMessage("§aNo Fall!");
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        if (breakCooldown.containsKey(id) && System.currentTimeMillis() - breakCooldown.get(id) < 200) return;
        breakCooldown.put(id, System.currentTimeMillis());

        PetEntity pet = plugin.getPetManager().getActivePet(p);
        if (pet == null) return;
        var b = pet.getPet().getBuffValues();
        Block block = e.getBlock();
        Material m = block.getType();

        // Magnetic Mining: immediate pickup
        if (b.getOrDefault(BuffType.MAGNETIC_MINING, 0.0) > 0 && isOre(m)) {
            for (ItemStack drop : block.getDrops()) {
                p.getInventory().addItem(drop);
            }
            e.setDropItems(false);
            p.updateInventory();
        }

        // Auto-Replant
        if (isCrop(m) && b.getOrDefault(BuffType.AUTO_REPLANT, 0.0) > 0 && p.getInventory().contains(seedFor(m)) && random.nextDouble() * 100 < b.get(BuffType.AUTO_REPLANT)) {
            e.setDropItems(true);
            replant(block, m);
            p.sendMessage("§aAuto Replant!");
        }

        // Double Crop Drop
        if (isCrop(m) && b.getOrDefault(BuffType.DOUBLE_CROP_DROP, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.DOUBLE_CROP_DROP)) {
            e.setDropItems(false);
            for (ItemStack drop : block.getDrops()) p.getInventory().addItem(drop);
            p.updateInventory();
            p.sendMessage("§aDouble Crop Drop!");
        }

        // Golden Touch on crops
        if (isCrop(m) && b.getOrDefault(BuffType.GOLDEN_TOUCH, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.GOLDEN_TOUCH)) {
            p.getInventory().addItem(new ItemStack(Material.GOLD_NUGGET));
            p.updateInventory();
            p.sendMessage("§aGolden Touch!");
        }

        // Spread
        if (b.getOrDefault(BuffType.SPREAD_CHANCE, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.SPREAD_CHANCE)) {
            spread(block, p);
            p.sendMessage("§aSpread!");
        }

        // Treasure Hunter
        if ((m == Material.DIRT || m == Material.GRAVEL || m == Material.SAND) && b.getOrDefault(BuffType.TREASURE_HUNTER, 0.0) > 0) {
            for (Map.Entry<Material, Double> entry : treasureMap.entrySet()) {
                if (random.nextDouble() * 100 < entry.getValue()) {
                    p.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(entry.getKey()));
                    p.sendMessage("§aTreasure Hunter: " + entry.getKey());
                }
            }
        }

        // Wood Chop
        if (isLog(m) && b.getOrDefault(BuffType.WOOD_CHOP, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.WOOD_CHOP)) {
            felltree(block);
            p.sendMessage("§aWood Chop!");
        }

        // Wood Fortune
        if (isLog(m) && b.getOrDefault(BuffType.WOOD_FORTUNE, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.WOOD_FORTUNE)) {
            for (ItemStack drop : block.getDrops()) p.getInventory().addItem(drop);
            p.updateInventory();
            p.sendMessage("§aWood Fortune!");
        }

        // Area Pickup in 5×5 instantly
        if (b.getOrDefault(BuffType.AREA_PICKUP, 0.0) > 0) {
            collectNearby(p, 5);
        }

        // Area Mine + Instant Smelt
        if (b.getOrDefault(BuffType.AREA_MINE, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.AREA_MINE)) {
            areaMine(block, b.getOrDefault(BuffType.INSTANT_SMELT, 0.0) > 0, p);
            p.sendMessage("§aArea Mine!");
        }

        // Single ore
        if (isOre(m)) {
            // Instant Smelt
            if (b.getOrDefault(BuffType.INSTANT_SMELT, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.INSTANT_SMELT)) {
                e.setDropItems(false);
                smeltBlock(block, p);
                p.sendMessage("§aAuto Smelt!");
            }
            // Golden Touch on ore
            if (b.getOrDefault(BuffType.GOLDEN_TOUCH, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.GOLDEN_TOUCH)) {
                p.getInventory().addItem(new ItemStack(Material.GOLD_INGOT));
                p.updateInventory();
                p.sendMessage("§aGolden Touch!");
            }
            // Vein Miner
            if (b.getOrDefault(BuffType.VEIN_MINER, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.VEIN_MINER)) {
                veinMine(block, p);
                p.sendMessage("§aVein Miner!");
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        PetEntity pet = plugin.getPetManager().getActivePet(p);
        if (pet == null) return;
        var b = pet.getPet().getBuffValues();

        // Shield Wall
        if (b.getOrDefault(BuffType.SHIELD_WALL, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.SHIELD_WALL)) {
            e.setCancelled(true);
            p.sendMessage("§aShield Wall!");
            return;
        }

        // One-Shot
        if (b.getOrDefault(BuffType.ONE_SHOT, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.ONE_SHOT) && e.getEntity() instanceof LivingEntity tgt) {
            tgt.setHealth(0);
            p.sendMessage("§cOne-Shot!");
        }

        // Freeze Strike
        if (b.getOrDefault(BuffType.FREEZE_STRIKE, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.FREEZE_STRIKE) && e.getEntity() instanceof Mob mob) {
            mob.setAI(false);
            new BukkitRunnable() {
                @Override
                public void run() {
                    mob.setAI(true);
                }
            }.runTaskLater(plugin, 5 * 20L);
            p.sendMessage("§aFreeze Strike!");
        }

        // Poison Touch
        if (b.getOrDefault(BuffType.POISON_TOUCH, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.POISON_TOUCH) && e.getEntity() instanceof LivingEntity t) {
            t.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
            p.sendMessage("§aPoison Touch!");
        }

        // Dodge
        if (b.getOrDefault(BuffType.DODGE, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.DODGE)) {
            e.setCancelled(true);
            p.sendMessage("§aDodge!");
        }

        // Reflect Damage
        if (b.getOrDefault(BuffType.REFLECT_DAMAGE, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.REFLECT_DAMAGE)) {
            p.damage(e.getDamage(), e.getDamager());
            p.sendMessage("§aReflected!");
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        Player p = e.getEntity().getKiller();
        if (p == null) return;
        PetEntity pet = plugin.getPetManager().getActivePet(p);
        if (pet == null) return;
        var b = pet.getPet().getBuffValues();

        // Double Mob Drops
        if (b.getOrDefault(BuffType.DOUBLE_MOB_DROPS, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.DOUBLE_MOB_DROPS)) {
            for (ItemStack drop : e.getDrops()) p.getWorld().dropItemNaturally(e.getEntity().getLocation(), drop);
            p.sendMessage("§aDouble Mob Drops!");
        }

        // Looting Boost
        if (b.getOrDefault(BuffType.LOOTING_BOOST, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.LOOTING_BOOST)) {
            e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), new ItemStack(Material.PHANTOM_MEMBRANE));
            p.sendMessage("§aLooting Boost!");
        }

        // Lucky Kill (coins) — now only once
        if (econ != null && b.getOrDefault(BuffType.LUCKY_KILL, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.LUCKY_KILL)) {
            int amt = luckyKillMin + random.nextInt(luckyKillMax - luckyKillMin + 1);
            econ.depositPlayer(p, amt);
            p.sendMessage("§aLucky Kill: +" + amt + " coins!");
        }

        // XP Boost
        if (b.getOrDefault(BuffType.XP_BOOST, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.XP_BOOST)) {
            for (ExperienceOrb orb : p.getWorld().getEntitiesByClass(ExperienceOrb.class)) {
                if (orb.getLocation().distanceSquared(e.getEntity().getLocation()) < 1)
                    orb.setExperience(orb.getExperience() * 2);
            }
            p.sendMessage("§aXP Boost!");
        }

        // Golden Touch on death
        if (b.getOrDefault(BuffType.GOLDEN_TOUCH, 0.0) > 0 && random.nextDouble() * 100 < b.get(BuffType.GOLDEN_TOUCH)) {
            p.getWorld().dropItemNaturally(e.getEntity().getLocation(), new ItemStack(Material.GOLD_NUGGET));
            p.sendMessage("§aGolden Touch!");
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player p = e.getPlayer();
        PetEntity pet = plugin.getPetManager().getActivePet(p);
        if (pet == null) return;
        double c = pet.getPet().getBuffValues().getOrDefault(BuffType.FISHING_LUCK, 0.0);
        if (c > 0 && random.nextDouble() * 100 < c && e.getCaught() instanceof Item it) {
            ItemStack st = it.getItemStack();
            st.setAmount(st.getAmount() * 2);
            it.setItemStack(st);
            p.sendMessage("§aFishing Luck!");
        }
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if (!(e.getTarget() instanceof Player p)) return;
        PetEntity pet = plugin.getPetManager().getActivePet(p);
        if (pet == null) return;
        double c = pet.getPet().getBuffValues().getOrDefault(BuffType.MOB_REPELLENT, 0.0);
        if (c > 0 && random.nextDouble() * 100 < c) e.setCancelled(true);
    }

    // Utility methods remain unchanged
    private Material seedFor(Material m) {
        return switch (m) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            default -> null;
        };
    }

    private boolean isCrop(Material m) {
        return seedFor(m) != null;
    }

    private boolean isOre(Material m) {
        return m.name().endsWith("_ORE");
    }

    private boolean isLog(Material m) {
        return m.name().endsWith("_LOG");
    }

    private boolean isSpreadable(Material m) {
        return isOre(m) || m == Material.STONE || m == Material.COBBLESTONE || m == Material.DEEPSLATE ||
                m == Material.DIRT || m == Material.TERRACOTTA || m == Material.END_STONE || m == Material.NETHERRACK;
    }

    private void replant(Block b, Material m) {
        Material crop = switch (m) {
            case WHEAT -> Material.WHEAT;
            case CARROTS -> Material.CARROTS;
            case POTATOES -> Material.POTATOES;
            case BEETROOTS -> Material.BEETROOTS;
            default -> null;
        };
        if (crop != null) {
            BlockData d = Bukkit.createBlockData(crop);
            if (d instanceof Ageable age) {
                age.setAge(0);
                b.setBlockData(age, false);
            } else b.setType(crop, false);
        }
    }

    private void spread(Block b, Player p) {
        Material m = b.getType();
        if (isSpreadable(m)) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        Block nearbyBlock = b.getRelative(dx, dy, dz);
                        if (nearbyBlock.getType() == m) {
                            nearbyBlock.breakNaturally();
                        }
                    }
                }
            }
        }
    }

    private void felltree(Block start) {
        Queue<Block> q = new ArrayDeque<>();
        Set<Block> v = new HashSet<>();
        q.add(start);
        v.add(start);
        while (!q.isEmpty()) {
            Block cur = q.poll();
            cur.breakNaturally();
            for (Block rel : List.of(
                    cur.getRelative(1, 0, 0), cur.getRelative(-1, 0, 0),
                    cur.getRelative(0, 1, 0), cur.getRelative(0, -1, 0),
                    cur.getRelative(0, 0, 1), cur.getRelative(0, 0, -1)
            )) {
                if (!v.contains(rel) && rel.getType() == start.getType()) {
                    v.add(rel);
                    q.add(rel);
                }
            }
        }
    }

    private void areaMine(Block b, boolean autoSmelt, Player p) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                Block n = b.getRelative(dx, 0, dz);
                Material m = n.getType();
                if (m.isBlock() && m != Material.BEDROCK) {
                    n.breakNaturally();
                    if (autoSmelt && isOre(m)) smeltBlock(n, p);
                }
            }
    }

    private void smeltBlock(Block b, Player p) {
        Material type = b.getType();
        Material smeltedMaterial = null;

        switch (type) {
            case IRON_ORE:
                smeltedMaterial = Material.IRON_INGOT;
                break;
            case DEEPSLATE_IRON_ORE:
                smeltedMaterial = Material.IRON_INGOT;
                break;
            case DEEPSLATE_GOLD_ORE:
                smeltedMaterial = Material.GOLD_INGOT;
                break;
            case GOLD_ORE:
                smeltedMaterial = Material.GOLD_INGOT;
                break;
            case COPPER_ORE:
                smeltedMaterial = Material.COPPER_INGOT;
                break;
            case DEEPSLATE_COPPER_ORE:
                smeltedMaterial = Material.COPPER_INGOT;
                break;
            case ANCIENT_DEBRIS:
                smeltedMaterial = Material.NETHERITE_SCRAP;
                break;
            case COBBLESTONE:
                smeltedMaterial = Material.STONE;
                break;
            case STONE:
                smeltedMaterial = Material.SMOOTH_STONE;
                break;
            case COBBLED_DEEPSLATE:
                smeltedMaterial = Material.DEEPSLATE;
                break;
            default:
                break;
        }

        if (smeltedMaterial != null) {
            p.getInventory().addItem(new ItemStack(smeltedMaterial));
            p.sendMessage("§aAuto Smelt: " + smeltedMaterial.name());
        }
    }

    private void veinMine(Block start, Player p) {
        Queue<Block> q = new ArrayDeque<>();
        Set<Block> v = new HashSet<>();
        q.add(start);
        v.add(start);
        while (!q.isEmpty()) {
            Block c = q.poll();
            if (!isOre(c.getType())) continue;
            c.breakNaturally();
            for (Block rel : List.of(
                    c.getRelative(1, 0, 0), c.getRelative(-1, 0, 0),
                    c.getRelative(0, 1, 0), c.getRelative(0, -1, 0),
                    c.getRelative(0, 0, 1), c.getRelative(0, 0, -1)
            )) {
                if (!v.contains(rel) && isOre(rel.getType())) {
                    v.add(rel);
                    q.add(rel);
                }
            }
        }
    }

    private void acceleratePlants(Player p, int rx, int ry, int rz) {
        Location loc = p.getLocation();
        World w = loc.getWorld();
        for (int dx = -rx; dx <= rx; dx++)
            for (int dy = -ry; dy <= ry; dy++)
                for (int dz = -rz; dz <= rz; dz++) {
                    Block b = w.getBlockAt(loc.getBlockX() + dx, loc.getBlockY() + dy, loc.getBlockZ() + dz);
                    if (isCrop(b.getType())) {
                        Ageable age = (Ageable) b.getBlockData();
                        if (age.getAge() < age.getMaximumAge()) {
                            age.setAge(age.getAge() + 1);
                            b.setBlockData(age, false);
                        }
                    }
                }
    }

    private void collectNearby(Player p, int r) {
        collectNearby(p, r, mat -> true);
    }

    private void collectNearby(Player p, int r, java.util.function.Predicate<Material> f) {
        for (Entity e : p.getWorld().getNearbyEntities(p.getLocation(), r, r, r)) {
            if (e instanceof Item it && f.test(it.getItemStack().getType())) {
                p.getInventory().addItem(it.getItemStack());
                it.remove();
            }
        }
        p.updateInventory();
    }

    private void showNearbyOres(Player p) {
        Location loc = p.getLocation();
        World world = p.getWorld();
        int range = 16; // Range to search for ores

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    Block block = world.getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
                    Material type = block.getType();
                    if (isOre(type)) {
                        Particle.DustOptions dustOptions = oreParticles.getOrDefault(type, new Particle.DustOptions(Color.BLACK, 1));
                        Location blockLoc = block.getLocation().add(0.5, 0.5, 0.5);
                        Vector direction = blockLoc.toVector().subtract(loc.toVector()).normalize();
                        double distance = blockLoc.distance(loc);

                        for (double i = 0; i < distance; i += 0.5) {
                            Location particleLoc = loc.clone().add(direction.clone().multiply(i));
                            world.spawnParticle(Particle.DUST, particleLoc, 1, dustOptions);
                        }
                    }
                }
            }
        }
    }
}

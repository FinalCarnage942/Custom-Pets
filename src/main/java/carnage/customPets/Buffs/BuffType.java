package carnage.customPets.Buffs;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public enum BuffType {
    SPEED("Speed", false) {
        @Override
        public void apply(Player player, int seconds) {
            player.removePotionEffect(PotionEffectType.SPEED);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED,
                    seconds * 20,
                    1,
                    true,
                    true
            ));
        }
    },
    JUMP_BOOST("Jump Boost", false) {
        @Override
        public void apply(Player player, int seconds) {
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.JUMP_BOOST,
                    seconds * 20,
                    1,
                    true,
                    true
            ));
        }
    },
    STRENGTH("Strength", false) {
        @Override
        public void apply(Player player, int seconds) {
            player.removePotionEffect(PotionEffectType.STRENGTH);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.STRENGTH,
                    seconds * 20,
                    0,
                    true,
                    true
            ));
        }
    },
    REGENERATION("Regeneration", false) {
        @Override
        public void apply(Player player, int seconds) {
            player.removePotionEffect(PotionEffectType.REGENERATION);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION,
                    seconds * 20,
                    0,
                    true,
                    true
            ));
        }
    },
    FIRE_RESISTANCE("Fire Resistance", false) {
        @Override
        public void apply(Player player, int seconds) {
            player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.FIRE_RESISTANCE,
                    seconds * 20,
                    0,
                    true,
                    true
            ));
        }
    },
    INVISIBILITY("Invisibility", false) {
        @Override
        public void apply(Player player, int seconds) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.INVISIBILITY,
                    seconds * 20,
                    0,
                    true,
                    true
            ));
        }
    },

    NIGHT_VISION("Night Vision", false) {
        @Override
        public void apply(Player player, int seconds) {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.NIGHT_VISION,
                    seconds * 20,
                    0,
                    true,
                    true
            ));
        }
    },
    WATER_BREATHING("Water Breathing", false) {
        @Override
        public void apply(Player player, int seconds) {
            player.removePotionEffect(PotionEffectType.WATER_BREATHING);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.WATER_BREATHING,
                    seconds * 20,
                    0,
                    true,
                    true
            ));
        }
    },
    HASTE("Haste", false) {
        @Override
        public void apply(Player player, int seconds) {
            player.removePotionEffect(PotionEffectType.HASTE);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.HASTE,
                    seconds * 20,
                    0,
                    true,
                    true
            ));
        }
    },
    ABSORPTION("Absorption", false) {
        @Override
        public void apply(Player player, int seconds) {
            player.removePotionEffect(PotionEffectType.ABSORPTION);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.ABSORPTION,
                    seconds * 20,
                    0,
                    true,
                    true
            ));
        }
    },
    SATURATION("Saturation", false) {
        @Override
        public void apply(Player player, int seconds) {
            player.removePotionEffect(PotionEffectType.SATURATION);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SATURATION,
                    seconds * 20,
                    0,
                    true,
                    true
            ));
        }
    },
    RESISTANCE("Resistance", false) {
        @Override
        public void apply(Player player, int seconds) {
            player.removePotionEffect(PotionEffectType.RESISTANCE);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.RESISTANCE,
                    seconds * 20,
                    0,
                    true,
                    true
            ));
        }
    },
    HEALTH_BOOST("Health Boost", false) {
        @Override
        public void apply(Player player, int seconds) {
            player.removePotionEffect(PotionEffectType.HEALTH_BOOST);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.HEALTH_BOOST,
                    seconds * 20,
                    0,
                    true,
                    true
            ));
        }
    },
    SLOW_FALLING("Slow Falling", false) {
        @Override
        public void apply(Player player, int seconds) {
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOW_FALLING,
                    seconds * 20,
                    0,
                    true,
                    true
            ));
        }
    },
    CONDUIT_POWER("Conduit Power", false) {
        @Override
        public void apply(Player player, int seconds) {
            player.removePotionEffect(PotionEffectType.CONDUIT_POWER);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.CONDUIT_POWER,
                    seconds * 20,
                    0,
                    true,
                    true
            ));
        }
    },
    HERO_OF_THE_VILLAGE("Hero of the Village", false) {
        @Override
        public void apply(Player player, int seconds) {
            player.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.HERO_OF_THE_VILLAGE,
                    seconds * 20,
                    0,
                    true,
                    true
            ));
        }
    },
    DOUBLE_CROP_DROP("Double Crop Drops", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    FISHING_LUCK("Fishing Luck", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    DOUBLE_ORE_DROP("Double Ore Drops", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    CRIT_CHANCE("Crit Chance", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    CRIT_DAMAGE("Crit Damage", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    MAGIC_FIND("Magic Find", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    INTELLIGENCE("Intelligence", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    WALK_SPEED("Walk Speed", false) {
        @Override
        public void apply(Player player, int seconds) {
            player.removePotionEffect(PotionEffectType.SPEED);
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED,
                    seconds * 20,
                    1,
                    true,
                    true
            ));
        }
    },
    WOOD_CHOP("Wood Chop", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    WOOD_FORTUNE("Wood Fortune", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    AREA_MINE("Area Mine", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    SPREAD_CHANCE("Spread Chance", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    SPREAD_COUNT("Spread Count", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    ONE_SHOT("One-Shot Chance", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    DOUBLE_MOB_DROPS("Double Mob Drops", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    SCAVENGER("Scavenger", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    LUCKY_KILL("Lucky Kill", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    MOB_REPELLENT("Mob Repellent", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    INSTANT_SMELT("Instant Smelt", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    NO_FALL("No Fall", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    ORE_XRAY("Ore XRay", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    AUTO_REPLANT("Auto Replant", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    MOB_SLOW("Mob Slow", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    AREA_PICKUP("Area Pickup", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    XP_BOOST("XP Boost", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    LOOTING_BOOST("Looting Boost", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    FREEZE_STRIKE("Freeze Strike", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    SHIELD_WALL("Shield Wall", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    MAGNETIC_MINING("Magnetic Mining", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    BLOODLUST("Bloodlust", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    DODGE("Dodge", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    BURN_AURA("Burn Aura", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    POISON_TOUCH("Poison Touch", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    GOLDEN_TOUCH("Golden Touch", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    PET_HEAL("Pet Heal", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    TREASURE_HUNTER("Treasure Hunter", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    FARM_HAND("Farm Hand", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    SOUL_COLLECTOR("Soul Collector", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    VEIN_MINER("Vein Miner", true) {
        @Override public void apply(Player player, int amplifier) { }
    },
    REFLECT_DAMAGE("Reflect Damage", true) {
        @Override public void apply(Player player, int amplifier) { }
    };

    private final String displayName;
    private final boolean isPassive;

    BuffType(String displayName, boolean isPassive) {
        this.displayName = displayName;
        this.isPassive = isPassive;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isPassive() {
        return isPassive;
    }

    public abstract void apply(Player player, int seconds);
}

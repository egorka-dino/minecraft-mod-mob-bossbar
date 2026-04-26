package com.egorka_dino.mobbossbar.paper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class MobBossbarPaperPlugin extends JavaPlugin {
    private static final String BOSS_TAG = "boss";
    private static final long UPDATE_INTERVAL_TICKS = 5L;

    private final Map<UUID, TrackedBoss> trackedBosses = new HashMap<>();
    private BukkitTask updateTask;

    @Override
    public void onEnable() {
        updateTask = Bukkit.getScheduler().runTaskTimer(this, this::updateBossBars, 1L, UPDATE_INTERVAL_TICKS);
    }

    @Override
    public void onDisable() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        trackedBosses.values().forEach(trackedBoss -> trackedBoss.bossBar.removeAll());
        trackedBosses.clear();
    }

    private void updateBossBars() {
        Set<UUID> seenBosses = new HashSet<>();

        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (!shouldTrack(entity)) {
                    continue;
                }

                UUID uuid = entity.getUniqueId();
                seenBosses.add(uuid);

                TrackedBoss trackedBoss = trackedBosses.computeIfAbsent(uuid, ignored -> createTrackedBoss());
                trackedBoss.update(entity);
            }
        }

        trackedBosses.entrySet().removeIf(entry -> {
            if (seenBosses.contains(entry.getKey())) {
                return false;
            }

            entry.getValue().bossBar.removeAll();
            return true;
        });
    }

    private static boolean shouldTrack(LivingEntity entity) {
        return !(entity instanceof Player)
                && entity.isValid()
                && !entity.isDead()
                && entity.getScoreboardTags().contains(BOSS_TAG);
    }

    private static TrackedBoss createTrackedBoss() {
        BossBar bossBar = Bukkit.createBossBar("Boss", BarColor.RED, BarStyle.SOLID);
        bossBar.setVisible(true);
        return new TrackedBoss(bossBar);
    }

    private static String getBossBarTitle(LivingEntity entity) {
        double health = Math.max(0.0D, entity.getHealth());
        double maxHealth = getMaxHealth(entity);
        return String.format(Locale.ROOT, "%s - %.1f/%.1f HP", entity.getName(), health, maxHealth);
    }

    private static double getMaxHealth(LivingEntity entity) {
        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) {
            return Math.max(1.0D, entity.getHealth());
        }

        return Math.max(1.0D, maxHealth.getValue());
    }

    private record TrackedBoss(BossBar bossBar) {
        private void update(LivingEntity entity) {
            double maxHealth = getMaxHealth(entity);

            bossBar.setTitle(getBossBarTitle(entity));
            bossBar.setProgress(clamp(entity.getHealth() / maxHealth, 0.0D, 1.0D));

            syncPlayers(entity.getWorld());
        }

        private void syncPlayers(World world) {
            Set<Player> expectedPlayers = new HashSet<>(world.getPlayers());

            for (Player player : expectedPlayers) {
                bossBar.addPlayer(player);
            }

            for (Player player : Set.copyOf(bossBar.getPlayers())) {
                if (!expectedPlayers.contains(player)) {
                    bossBar.removePlayer(player);
                }
            }
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

package com.egorka_dino.mobbossbar.fabric;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.TypeFilter;

public final class FabricBossMobTracker {
    private static final String BOSS_TAG = "boss";
    private static final int UPDATE_INTERVAL_TICKS = 5;
    private static final Map<UUID, TrackedBoss> TRACKED_BOSSES = new HashMap<>();

    private static int ticksUntilUpdate;

    private FabricBossMobTracker() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(FabricBossMobTracker::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> clearAll());
    }

    private static void tick(MinecraftServer server) {
        ticksUntilUpdate--;
        if (ticksUntilUpdate > 0) {
            return;
        }

        ticksUntilUpdate = UPDATE_INTERVAL_TICKS;
        Set<UUID> seenBosses = new HashSet<>();

        for (ServerWorld world : server.getWorlds()) {
            for (LivingEntity entity : world.getEntitiesByType(TypeFilter.instanceOf(LivingEntity.class), FabricBossMobTracker::shouldTrack)) {
                UUID uuid = entity.getUuid();
                seenBosses.add(uuid);

                TrackedBoss trackedBoss = TRACKED_BOSSES.computeIfAbsent(uuid, ignored -> createTrackedBoss());
                trackedBoss.update(world, entity, server);
            }
        }

        TRACKED_BOSSES.entrySet().removeIf(entry -> {
            if (seenBosses.contains(entry.getKey())) {
                return false;
            }

            entry.getValue().bossBar.clearPlayers();
            return true;
        });
    }

    private static boolean shouldTrack(LivingEntity entity) {
        return !(entity instanceof PlayerEntity)
                && entity.isAlive()
                && entity.getCommandTags().contains(BOSS_TAG);
    }

    private static TrackedBoss createTrackedBoss() {
        ServerBossBar bossBar = new ServerBossBar(Text.literal("Boss"), BossBar.Color.RED, BossBar.Style.PROGRESS);
        bossBar.setVisible(true);
        return new TrackedBoss(bossBar);
    }

    private static Text getBossBarName(LivingEntity entity) {
        float health = Math.max(0.0F, entity.getHealth());
        float maxHealth = Math.max(1.0F, entity.getMaxHealth());
        String healthText = String.format(Locale.ROOT, " - %.1f/%.1f HP", health, maxHealth);

        return Text.literal("")
                .append(entity.getName())
                .append(Text.literal(healthText));
    }

    private static void clearAll() {
        TRACKED_BOSSES.values().forEach(trackedBoss -> trackedBoss.bossBar.clearPlayers());
        TRACKED_BOSSES.clear();
        ticksUntilUpdate = 0;
    }

    private record TrackedBoss(ServerBossBar bossBar) {
        private void update(ServerWorld world, LivingEntity entity, MinecraftServer server) {
            bossBar.setName(getBossBarName(entity));
            bossBar.setPercent(clamp(entity.getHealth() / Math.max(1.0F, entity.getMaxHealth()), 0.0F, 1.0F));

            syncPlayers(world, server);
        }

        private void syncPlayers(ServerWorld world, MinecraftServer server) {
            Set<ServerPlayerEntity> expectedPlayers = new HashSet<>();

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.getEntityWorld() == world) {
                    expectedPlayers.add(player);
                    bossBar.addPlayer(player);
                }
            }

            for (ServerPlayerEntity player : Set.copyOf(bossBar.getPlayers())) {
                if (!expectedPlayers.contains(player)) {
                    bossBar.removePlayer(player);
                }
            }
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

package com.egorka_dino.mobbossbar.fabric;

import net.fabricmc.api.ModInitializer;

public final class MobBossbarFabricMod implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricBossMobTracker.register();
    }
}

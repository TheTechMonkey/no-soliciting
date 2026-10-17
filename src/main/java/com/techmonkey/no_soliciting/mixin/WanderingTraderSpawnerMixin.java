package com.techmonkey.no_soliciting.mixin;

import com.techmonkey.no_soliciting.zone.ZoneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WanderingTraderSpawner.class)
public abstract class WanderingTraderSpawnerMixin {
    @Inject(method = "findSpawnPositionNear", at = @At("RETURN"), cancellable = true)
    private void noSoliciting$rejectProtectedSpawn(
            LevelReader level,
            BlockPos center,
            int range,
            CallbackInfoReturnable<BlockPos> callback
    ) {
        BlockPos candidate = callback.getReturnValue();
        if (candidate != null
                && level instanceof ServerLevel serverLevel
                && ZoneManager.isProtected(serverLevel, candidate)) {
            callback.setReturnValue(null);
        }
    }
}


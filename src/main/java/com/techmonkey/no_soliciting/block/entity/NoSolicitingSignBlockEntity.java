package com.techmonkey.no_soliciting.block.entity;

import com.techmonkey.no_soliciting.NoSoliciting;
import com.techmonkey.no_soliciting.block.NoSolicitingSignBlock;
import com.techmonkey.no_soliciting.zone.ZoneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class NoSolicitingSignBlockEntity extends BlockEntity {
    private static final int REPEL_INTERVAL = 10;

    public NoSolicitingSignBlockEntity(BlockPos pos, BlockState state) {
        super(NoSoliciting.NO_SOLICITING_SIGN_BLOCK_ENTITY, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NoSolicitingSignBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int radius = NoSolicitingSignBlock.radius(state);
        ZoneManager.register(serverLevel, pos, radius);

        if (Math.floorMod(serverLevel.getGameTime() + pos.asLong(), REPEL_INTERVAL) != 0) {
            return;
        }

        double minX = pos.getX() - radius;
        double maxX = pos.getX() + radius + 1.0;
        double minZ = pos.getZ() - radius;
        double maxZ = pos.getZ() + radius + 1.0;
        AABB protectedArea = new AABB(minX, serverLevel.getMinY(), minZ,
                maxX, serverLevel.getMaxY(), maxZ);

        List<Entity> visitors = serverLevel.getEntities(
                (Entity) null,
                protectedArea,
                entity -> entity instanceof WanderingTrader || entity instanceof TraderLlama
        );

        for (Entity visitor : visitors) {
            directOutside(visitor, minX, maxX, minZ, maxZ);
        }
    }

    private static void directOutside(Entity entity, double minX, double maxX, double minZ, double maxZ) {
        double west = entity.getX() - minX;
        double east = maxX - entity.getX();
        double north = entity.getZ() - minZ;
        double south = maxZ - entity.getZ();

        double targetX = entity.getX();
        double targetZ = entity.getZ();
        double closest = west;
        targetX = minX - 1.5;

        if (east < closest) {
            closest = east;
            targetX = maxX + 1.5;
            targetZ = entity.getZ();
        }
        if (north < closest) {
            closest = north;
            targetX = entity.getX();
            targetZ = minZ - 1.5;
        }
        if (south < closest) {
            targetX = entity.getX();
            targetZ = maxZ + 1.5;
        }

        if (entity instanceof WanderingTrader trader) {
            trader.setWanderTarget(BlockPos.containing(targetX, entity.getY(), targetZ));
        }

        if (entity instanceof Mob mob) {
            mob.getNavigation().moveTo(targetX, entity.getY(), targetZ, 1.25);
        }

        Vec3 outward = new Vec3(targetX - entity.getX(), 0.0, targetZ - entity.getZ());
        if (outward.lengthSqr() > 0.001) {
            outward = outward.normalize().scale(0.08);
            entity.setDeltaMovement(entity.getDeltaMovement().add(outward));
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            ZoneManager.remove(serverLevel, worldPosition);
        }
        super.setRemoved();
    }
}


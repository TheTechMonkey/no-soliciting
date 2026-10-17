package com.techmonkey.no_soliciting.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public final class ZoneManager {
    private static final long STALE_AFTER_TICKS = 40;
    private static final Map<ServerLevel, Map<Long, Zone>> ZONES = new WeakHashMap<>();

    private ZoneManager() {
    }

    public static void register(ServerLevel level, BlockPos center, int radius) {
        ZONES.computeIfAbsent(level, ignored -> new HashMap<>())
                .put(center.asLong(), new Zone(center.immutable(), radius, level.getGameTime()));
    }

    public static void remove(ServerLevel level, BlockPos center) {
        Map<Long, Zone> levelZones = ZONES.get(level);
        if (levelZones != null) {
            levelZones.remove(center.asLong());
        }
    }

    public static boolean isProtected(ServerLevel level, BlockPos pos) {
        Map<Long, Zone> levelZones = ZONES.get(level);
        if (levelZones == null || levelZones.isEmpty()) {
            return false;
        }

        long oldestAllowed = level.getGameTime() - STALE_AFTER_TICKS;
        Iterator<Zone> iterator = levelZones.values().iterator();
        while (iterator.hasNext()) {
            Zone zone = iterator.next();
            if (zone.lastSeenTick < oldestAllowed) {
                iterator.remove();
                continue;
            }

            if (Math.abs(pos.getX() - zone.center.getX()) <= zone.radius
                    && Math.abs(pos.getZ() - zone.center.getZ()) <= zone.radius) {
                return true;
            }
        }

        return false;
    }

    private record Zone(BlockPos center, int radius, long lastSeenTick) {
    }
}


package com.techmonkey.no_soliciting.client;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class BoundaryRenderer {
    private static final long DISPLAY_TICKS = 20L * 60L;
    private static final int LINE_COLOR = 0xD9FF3030;
    private static final int SURFACE_COLOR = 0x55FF3030;
    private static final int MARKER_SPACING = 8;
    private static final double REGULAR_MARKER_HEIGHT = 3.0;
    private static final double CORNER_MARKER_HEIGHT = 8.0;

    private static BlockPos center;
    private static long hideAtGameTime;
    private static DrawableGizmoPrimitives boundary;

    private BoundaryRenderer() {
    }

    public static void show(BlockPos signPos, int selectedRadius) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        center = signPos.immutable();
        hideAtGameTime = minecraft.level.getGameTime() + DISPLAY_TICKS;
        boundary = buildBoundary(minecraft.level, center, selectedRadius);
    }

    private static DrawableGizmoPrimitives buildBoundary(ClientLevel level, BlockPos center, int radius) {
        DrawableGizmoPrimitives primitives = new DrawableGizmoPrimitives();
        List<SurfacePoint> points = perimeter(level, center, radius);

        for (int index = 0; index < points.size(); index++) {
            SurfacePoint point = points.get(index);
            SurfacePoint next = points.get((index + 1) % points.size());

            double x = point.x;
            double y = point.y + 0.035;
            double z = point.z;

            primitives.addQuad(
                    new Vec3(x, y, z),
                    new Vec3(x + 1.0, y, z),
                    new Vec3(x + 1.0, y, z + 1.0),
                    new Vec3(x, y, z + 1.0),
                    SURFACE_COLOR
            );

            primitives.addLine(
                    new Vec3(x + 0.5, y + 0.015, z + 0.5),
                    new Vec3(next.x + 0.5, next.y + 0.05, next.z + 0.5),
                    LINE_COLOR,
                    3.0F
            );

            boolean corner = isCorner(point, center, radius);
            if (corner || isMarkerPosition(point, center, radius)) {
                double markerHeight = corner ? CORNER_MARKER_HEIGHT : REGULAR_MARKER_HEIGHT;
                primitives.addLine(
                        new Vec3(x + 0.5, y, z + 0.5),
                        new Vec3(x + 0.5, y + markerHeight, z + 0.5),
                        LINE_COLOR,
                        corner ? 5.0F : 3.0F
                );
            }
        }

        return primitives;
    }

    private static List<SurfacePoint> perimeter(ClientLevel level, BlockPos center, int radius) {
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;
        List<SurfacePoint> points = new ArrayList<>((radius * 2 + 1) * 4);

        for (int x = minX; x <= maxX; x++) {
            points.add(surfacePoint(level, x, minZ));
        }
        for (int z = minZ + 1; z <= maxZ; z++) {
            points.add(surfacePoint(level, maxX, z));
        }
        for (int x = maxX - 1; x >= minX; x--) {
            points.add(surfacePoint(level, x, maxZ));
        }
        for (int z = maxZ - 1; z > minZ; z--) {
            points.add(surfacePoint(level, minX, z));
        }

        return points;
    }

    private static SurfacePoint surfacePoint(ClientLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new SurfacePoint(x, y, z);
    }

    private static boolean isCorner(SurfacePoint point, BlockPos center, int radius) {
        boolean edgeX = Math.abs(point.x - center.getX()) == radius;
        boolean edgeZ = Math.abs(point.z - center.getZ()) == radius;
        return edgeX && edgeZ;
    }

    private static boolean isMarkerPosition(SurfacePoint point, BlockPos center, int radius) {
        if (Math.abs(point.z - center.getZ()) == radius) {
            return Math.floorMod(point.x - (center.getX() - radius), MARKER_SPACING) == 0;
        }
        return Math.floorMod(point.z - (center.getZ() - radius), MARKER_SPACING) == 0;
    }

    public static void render(LevelRenderContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (center == null
                || boundary == null
                || minecraft.level == null
                || minecraft.level.getGameTime() >= hideAtGameTime) {
            center = null;
            boundary = null;
            return;
        }

        boundary.submit(
                context.submitNodeCollector(),
                context.levelState().cameraRenderState,
                false
        );
    }

    private record SurfacePoint(int x, int y, int z) {
    }
}


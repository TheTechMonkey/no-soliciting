package com.techmonkey.no_soliciting.client;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.Set;

public final class BoundaryRenderer {
    private static final long DISPLAY_TICKS = 20L * 60L;
    private static final int LINE_COLOR = 0xD9FF3030;
    private static final int MARKER_SPACING = 8;
    private static final int SURFACE_SEARCH_RANGE = 48;
    private static final double REGULAR_MARKER_HEIGHT = 2.0;
    private static final double CORNER_MARKER_HEIGHT = 5.0;

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
        for (BlockPos marker : markerPositions(center, radius)) {
            int surfaceY = findSurfaceY(level, marker.getX(), marker.getZ(), center.getY());
            boolean corner = isCorner(marker, center, radius);
            double height = corner ? CORNER_MARKER_HEIGHT : REGULAR_MARKER_HEIGHT;
            Vec3 bottom = new Vec3(marker.getX() + 0.5, surfaceY + 0.08, marker.getZ() + 0.5);
            Vec3 top = bottom.add(0.0, height, 0.0);

            primitives.addLine(bottom, top, LINE_COLOR, corner ? 5.0F : 3.0F);
            primitives.addPoint(top, LINE_COLOR, corner ? 7.0F : 5.0F);
        }

        return primitives;
    }

    private static Set<BlockPos> markerPositions(BlockPos center, int radius) {
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;
        Set<BlockPos> markers = new LinkedHashSet<>();

        for (int offset = 0; offset <= radius * 2; offset += MARKER_SPACING) {
            int x = minX + offset;
            int z = minZ + offset;
            markers.add(new BlockPos(x, center.getY(), minZ));
            markers.add(new BlockPos(x, center.getY(), maxZ));
            markers.add(new BlockPos(minX, center.getY(), z));
            markers.add(new BlockPos(maxX, center.getY(), z));
        }

        markers.add(new BlockPos(minX, center.getY(), minZ));
        markers.add(new BlockPos(minX, center.getY(), maxZ));
        markers.add(new BlockPos(maxX, center.getY(), minZ));
        markers.add(new BlockPos(maxX, center.getY(), maxZ));
        return markers;
    }

    private static int findSurfaceY(ClientLevel level, int x, int z, int preferredY) {
        BlockPos.MutableBlockPos current = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();

        for (int distance = 0; distance <= SURFACE_SEARCH_RANGE; distance++) {
            int lowerY = preferredY - distance;
            if (isUsableSurface(level, current, below, x, lowerY, z)) {
                return lowerY;
            }

            if (distance > 0) {
                int upperY = preferredY + distance;
                if (isUsableSurface(level, current, below, x, upperY, z)) {
                    return upperY;
                }
            }
        }

        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    private static boolean isUsableSurface(
            ClientLevel level,
            BlockPos.MutableBlockPos current,
            BlockPos.MutableBlockPos below,
            int x,
            int y,
            int z
    ) {
        if (!level.isInsideBuildHeight(y) || !level.isInsideBuildHeight(y - 1)) {
            return false;
        }

        current.set(x, y, z);
        below.set(x, y - 1, z);
        BlockState currentState = level.getBlockState(current);
        BlockState belowState = level.getBlockState(below);

        boolean supported = !belowState.getCollisionShape(level, below).isEmpty();
        boolean open = currentState.getCollisionShape(level, current).isEmpty()
                || currentState.is(BlockTags.LOGS)
                || currentState.is(BlockTags.LEAVES);
        return supported && open;
    }

    private static boolean isCorner(BlockPos point, BlockPos center, int radius) {
        boolean edgeX = Math.abs(point.getX() - center.getX()) == radius;
        boolean edgeZ = Math.abs(point.getZ() - center.getZ()) == radius;
        return edgeX && edgeZ;
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
}


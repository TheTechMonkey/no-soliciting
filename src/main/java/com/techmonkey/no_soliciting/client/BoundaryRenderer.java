package com.techmonkey.no_soliciting.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public final class BoundaryRenderer {
    private static final long DISPLAY_TICKS = 20L * 60L;
    private static final int COLOR = 0xD9FF3030;
    private static final double LINE_HALF_WIDTH = 0.025;
    private static final double LINE_HEIGHT = 0.03;
    private static final double SURFACE_OFFSET = 0.045;

    private static BlockPos center;
    private static long hideAtGameTime;
    private static VoxelShape boundary;
    private static Vec3 boundaryOrigin;

    private BoundaryRenderer() {
    }

    public static void show(BlockPos signPos, int selectedRadius) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        center = signPos.immutable();
        hideAtGameTime = minecraft.level.getGameTime() + DISPLAY_TICKS;
        buildBoundary(minecraft.level, center, selectedRadius);
    }

    private static void buildBoundary(ClientLevel level, BlockPos center, int radius) {
        List<TerrainPoint> points = perimeter(level, center, radius);
        int originX = center.getX() - radius;
        int originZ = center.getZ() - radius;
        int originY = points.stream().mapToInt(TerrainPoint::y).min().orElse(center.getY());
        List<VoxelShape> pieces = new ArrayList<>(points.size() * 2);

        for (int index = 0; index < points.size(); index++) {
            TerrainPoint current = points.get(index);
            TerrainPoint next = points.get((index + 1) % points.size());
            double x1 = current.x + 0.5 - originX;
            double z1 = current.z + 0.5 - originZ;
            double x2 = next.x + 0.5 - originX;
            double z2 = next.z + 0.5 - originZ;
            double y1 = current.y - originY + SURFACE_OFFSET;
            double y2 = next.y - originY + SURFACE_OFFSET;

            pieces.add(Shapes.box(
                    Math.min(x1, x2) - LINE_HALF_WIDTH,
                    y1,
                    Math.min(z1, z2) - LINE_HALF_WIDTH,
                    Math.max(x1, x2) + LINE_HALF_WIDTH,
                    y1 + LINE_HEIGHT,
                    Math.max(z1, z2) + LINE_HALF_WIDTH
            ));

            if (current.y != next.y) {
                pieces.add(Shapes.box(
                        x2 - LINE_HALF_WIDTH,
                        Math.min(y1, y2),
                        z2 - LINE_HALF_WIDTH,
                        x2 + LINE_HALF_WIDTH,
                        Math.max(y1, y2) + LINE_HEIGHT,
                        z2 + LINE_HALF_WIDTH
                ));
            }
        }

        boundary = Shapes.or(pieces.getFirst(), pieces.subList(1, pieces.size()).toArray(VoxelShape[]::new));
        boundaryOrigin = new Vec3(originX, originY, originZ);
    }

    private static List<TerrainPoint> perimeter(ClientLevel level, BlockPos center, int radius) {
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;
        List<TerrainPoint> points = new ArrayList<>((radius * 2 + 1) * 4);

        for (int x = minX; x <= maxX; x++) {
            points.add(terrainPoint(level, x, minZ));
        }
        for (int z = minZ + 1; z <= maxZ; z++) {
            points.add(terrainPoint(level, maxX, z));
        }
        for (int x = maxX - 1; x >= minX; x--) {
            points.add(terrainPoint(level, x, maxZ));
        }
        for (int z = maxZ - 1; z > minZ; z--) {
            points.add(terrainPoint(level, minX, z));
        }

        return points;
    }

    private static TerrainPoint terrainPoint(ClientLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int startingY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

        for (int y = startingY; level.isInsideBuildHeight(y); y--) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                continue;
            }
            if (Block.isShapeFullBlock(state.getCollisionShape(level, pos))) {
                return new TerrainPoint(x, y + 1, z);
            }
        }

        return new TerrainPoint(x, level.getMinY(), z);
    }

    public static void render(LevelRenderContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (center == null
                || boundary == null
                || boundaryOrigin == null
                || minecraft.level == null
                || minecraft.level.getGameTime() >= hideAtGameTime) {
            center = null;
            boundary = null;
            boundaryOrigin = null;
            return;
        }

        Vec3 camera = context.levelState().cameraRenderState.pos;
        PoseStack pose = context.poseStack();
        pose.pushPose();
        pose.translate(
                boundaryOrigin.x - camera.x,
                boundaryOrigin.y - camera.y,
                boundaryOrigin.z - camera.z
        );
        context.submitNodeCollector().submitShapeOutline(
                pose,
                boundary,
                RenderTypes.linesTranslucentNoDepthWrite(),
                COLOR,
                4.0F,
                true
        );
        pose.popPose();
    }

    private record TerrainPoint(int x, int y, int z) {
    }
}

package com.techmonkey.no_soliciting.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
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
    private static final double SHAPE_THICKNESS = 0.001;
    private static final double SURFACE_OFFSET = 0.055;

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
        List<ContourSegment> segments = mergeFlatRuns(perimeter(level, center, radius));
        int originX = center.getX() - radius;
        int originZ = center.getZ() - radius;
        int originY = segments.stream().mapToInt(ContourSegment::y).min().orElse(center.getY());
        List<VoxelShape> pieces = new ArrayList<>(segments.size() * 2);

        for (int index = 0; index < segments.size(); index++) {
            ContourSegment current = segments.get(index);
            ContourSegment next = segments.get((index + 1) % segments.size());
            double x1 = current.x1 - originX;
            double z1 = current.z1 - originZ;
            double x2 = current.x2 - originX;
            double z2 = current.z2 - originZ;
            double y1 = current.y - originY + SURFACE_OFFSET;
            double y2 = next.y - originY + SURFACE_OFFSET;

            pieces.add(Shapes.box(
                    Math.min(x1, x2) - SHAPE_THICKNESS,
                    y1,
                    Math.min(z1, z2) - SHAPE_THICKNESS,
                    Math.max(x1, x2) + SHAPE_THICKNESS,
                    y1 + SHAPE_THICKNESS,
                    Math.max(z1, z2) + SHAPE_THICKNESS
            ));

            if (current.y != next.y) {
                pieces.add(Shapes.box(
                        x2 - SHAPE_THICKNESS,
                        Math.min(y1, y2),
                        z2 - SHAPE_THICKNESS,
                        x2 + SHAPE_THICKNESS,
                        Math.max(y1, y2) + SHAPE_THICKNESS,
                        z2 + SHAPE_THICKNESS
                ));
            }
        }

        boundary = Shapes.or(pieces.getFirst(), pieces.subList(1, pieces.size()).toArray(VoxelShape[]::new));
        boundaryOrigin = new Vec3(originX, originY, originZ);
    }

    private static List<ContourSegment> mergeFlatRuns(List<ContourSegment> segments) {
        List<ContourSegment> merged = new ArrayList<>();
        ContourSegment run = segments.getFirst();

        for (int index = 1; index < segments.size(); index++) {
            ContourSegment next = segments.get(index);
            int runXDirection = Integer.signum(run.x2 - run.x1);
            int runZDirection = Integer.signum(run.z2 - run.z1);
            int nextXDirection = Integer.signum(next.x2 - next.x1);
            int nextZDirection = Integer.signum(next.z2 - next.z1);
            boolean connected = run.x2 == next.x1 && run.z2 == next.z1;
            boolean sameDirection = runXDirection == nextXDirection && runZDirection == nextZDirection;

            if (connected && sameDirection && run.y == next.y) {
                run = new ContourSegment(run.x1, run.z1, next.x2, next.z2, run.y);
            } else {
                merged.add(run);
                run = next;
            }
        }

        merged.add(run);
        return merged;
    }

    private static List<ContourSegment> perimeter(ClientLevel level, BlockPos center, int radius) {
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;
        List<ContourSegment> segments = new ArrayList<>((radius * 2 + 1) * 4);

        for (int x = minX; x <= maxX; x++) {
            int y = Math.max(boundaryY(level, x, minZ), boundaryY(level, x, minZ - 1));
            segments.add(new ContourSegment(x, minZ, x + 1, minZ, y));
        }
        for (int z = minZ; z <= maxZ; z++) {
            int y = Math.max(boundaryY(level, maxX, z), boundaryY(level, maxX + 1, z));
            segments.add(new ContourSegment(maxX + 1, z, maxX + 1, z + 1, y));
        }
        for (int x = maxX; x >= minX; x--) {
            int y = Math.max(boundaryY(level, x, maxZ), boundaryY(level, x, maxZ + 1));
            segments.add(new ContourSegment(x + 1, maxZ + 1, x, maxZ + 1, y));
        }
        for (int z = maxZ; z >= minZ; z--) {
            int y = Math.max(boundaryY(level, minX, z), boundaryY(level, minX - 1, z));
            segments.add(new ContourSegment(minX, z + 1, minX, z, y));
        }

        return segments;
    }

    private static int boundaryY(ClientLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int startingY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        int terrainY = level.getMinY();

        for (int y = startingY; level.isInsideBuildHeight(y); y--) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                continue;
            }
            if (Block.isShapeFullBlock(state.getCollisionShape(level, pos))) {
                terrainY = y + 1;
                break;
            }
        }

        int worldSurface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        for (int y = worldSurface - 1; y >= terrainY; y--) {
            pos.set(x, y, z);
            if (level.getFluidState(pos).is(FluidTags.WATER)) {
                return y + 1;
            }
        }

        return terrainY;
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

    private record ContourSegment(int x1, int z1, int x2, int z2, int y) {
    }
}

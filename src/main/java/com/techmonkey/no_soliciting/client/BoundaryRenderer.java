package com.techmonkey.no_soliciting.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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

import java.util.ArrayList;
import java.util.List;

public final class BoundaryRenderer {
    private static final long DISPLAY_TICKS = 20L * 60L;
    private static final int COLOR = 0xD9FF3030;
    private static final float LINE_WIDTH = 4.0F;
    private static final double SURFACE_OFFSET = 0.055;

    private static BlockPos center;
    private static long hideAtGameTime;
    private static List<BoundaryLine> boundaryLines;
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
        List<ContourSegment> segments = perimeter(level, center, radius);
        int originX = center.getX() - radius;
        int originZ = center.getZ() - radius;
        int originY = segments.stream().mapToInt(ContourSegment::y).min().orElse(center.getY());
        List<BoundaryLine> lines = new ArrayList<>(segments.size() * 2);

        for (int index = 0; index < segments.size(); index++) {
            ContourSegment current = segments.get(index);
            ContourSegment next = segments.get((index + 1) % segments.size());
            double y = current.y - originY + SURFACE_OFFSET;

            lines.add(new BoundaryLine(
                    new Vec3(current.x1 - originX, y, current.z1 - originZ),
                    new Vec3(current.x2 - originX, y, current.z2 - originZ)
            ));

            if (current.y != next.y) {
                lines.add(new BoundaryLine(
                        new Vec3(current.x2 - originX, y, current.z2 - originZ),
                        new Vec3(
                                current.x2 - originX,
                                next.y - originY + SURFACE_OFFSET,
                                current.z2 - originZ
                        )
                ));
            }
        }

        boundaryLines = List.copyOf(lines);
        boundaryOrigin = new Vec3(originX, originY, originZ);
    }

    private static List<ContourSegment> perimeter(ClientLevel level, BlockPos center, int radius) {
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;
        List<ContourSegment> segments = new ArrayList<>((radius * 2 + 1) * 4);

        for (int x = minX; x <= maxX; x++) {
            segments.add(new ContourSegment(x, minZ, x + 1, minZ, terrainY(level, x, minZ)));
        }
        for (int z = minZ; z <= maxZ; z++) {
            segments.add(new ContourSegment(maxX + 1, z, maxX + 1, z + 1, terrainY(level, maxX, z)));
        }
        for (int x = maxX; x >= minX; x--) {
            segments.add(new ContourSegment(x + 1, maxZ + 1, x, maxZ + 1, terrainY(level, x, maxZ)));
        }
        for (int z = maxZ; z >= minZ; z--) {
            segments.add(new ContourSegment(minX, z + 1, minX, z, terrainY(level, minX, z)));
        }

        return segments;
    }

    private static int terrainY(ClientLevel level, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int startingY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

        for (int y = startingY; level.isInsideBuildHeight(y); y--) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                continue;
            }
            if (Block.isShapeFullBlock(state.getCollisionShape(level, pos))) {
                return y + 1;
            }
        }

        return level.getMinY();
    }

    public static void render(LevelRenderContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (center == null
                || boundaryLines == null
                || boundaryOrigin == null
                || minecraft.level == null
                || minecraft.level.getGameTime() >= hideAtGameTime) {
            center = null;
            boundaryLines = null;
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
        context.submitNodeCollector().submitCustomGeometry(
                pose,
                RenderTypes.linesTranslucentNoDepthWrite(),
                (entry, consumer) -> renderLines(entry, consumer)
        );
        pose.popPose();
    }

    private static void renderLines(PoseStack.Pose pose, VertexConsumer consumer) {
        for (BoundaryLine line : boundaryLines) {
            Vec3 direction = line.to.subtract(line.from).normalize();
            float normalX = (float) direction.x;
            float normalY = (float) direction.y;
            float normalZ = (float) direction.z;

            consumer.addVertex(pose, (float) line.from.x, (float) line.from.y, (float) line.from.z)
                    .setColor(COLOR)
                    .setNormal(pose, normalX, normalY, normalZ)
                    .setLineWidth(LINE_WIDTH);
            consumer.addVertex(pose, (float) line.to.x, (float) line.to.y, (float) line.to.z)
                    .setColor(COLOR)
                    .setNormal(pose, normalX, normalY, normalZ)
                    .setLineWidth(LINE_WIDTH);
        }
    }

    private record ContourSegment(int x1, int z1, int x2, int z2, int y) {
    }

    private record BoundaryLine(Vec3 from, Vec3 to) {
    }
}

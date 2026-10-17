package com.techmonkey.no_soliciting.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BoundaryRenderer {
    private static final long DISPLAY_TICKS = 20L * 60L;
    private static final int COLOR = 0xD9FF3030;

    private static BlockPos center;
    private static int radius;
    private static long hideAtGameTime;

    private BoundaryRenderer() {
    }

    public static void show(BlockPos signPos, int selectedRadius) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        center = signPos.immutable();
        radius = selectedRadius;
        hideAtGameTime = minecraft.level.getGameTime() + DISPLAY_TICKS;
    }

    public static void render(LevelRenderContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (center == null || minecraft.level == null || minecraft.level.getGameTime() >= hideAtGameTime) {
            center = null;
            return;
        }

        Vec3 camera = context.levelState().cameraRenderState.pos;
        double minX = center.getX() - radius;
        double minZ = center.getZ() - radius;
        double y = center.getY() + 0.03;
        double size = radius * 2.0 + 1.0;
        VoxelShape boundary = Shapes.box(0.0, 0.0, 0.0, size, 0.025, size);

        PoseStack pose = context.poseStack();
        pose.pushPose();
        pose.translate(minX - camera.x, y - camera.y, minZ - camera.z);
        context.submitNodeCollector().submitShapeOutline(
                pose,
                boundary,
                RenderTypes.linesTranslucentNoDepthWrite(),
                COLOR,
                3.0F,
                true
        );
        pose.popPose();
    }
}


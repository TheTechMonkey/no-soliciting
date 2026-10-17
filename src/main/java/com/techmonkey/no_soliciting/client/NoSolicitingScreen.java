package com.techmonkey.no_soliciting.client;

import com.techmonkey.no_soliciting.block.NoSolicitingSignBlock;
import com.techmonkey.no_soliciting.network.UpdateRadiusPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class NoSolicitingScreen extends Screen {
    private static final int BUTTON_WIDTH = 46;
    private static final int BUTTON_GAP = 4;

    private final BlockPos signPos;
    private int selectedIndex;
    private final Button[] radiusButtons = new Button[NoSolicitingSignBlock.RADII.length];

    public NoSolicitingScreen(BlockPos signPos, int selectedIndex) {
        super(Component.translatable("screen.no_soliciting.title"));
        this.signPos = signPos;
        this.selectedIndex = selectedIndex;
    }

    @Override
    protected void init() {
        int rowWidth = BUTTON_WIDTH * radiusButtons.length + BUTTON_GAP * (radiusButtons.length - 1);
        int startX = (width - rowWidth) / 2;
        int radiusY = height / 2 - 20;

        for (int index = 0; index < radiusButtons.length; index++) {
            final int buttonIndex = index;
            radiusButtons[index] = addRenderableWidget(Button.builder(
                    Component.literal(Integer.toString(NoSolicitingSignBlock.RADII[index])),
                    button -> selectRadius(buttonIndex)
            ).bounds(startX + index * (BUTTON_WIDTH + BUTTON_GAP), radiusY, BUTTON_WIDTH, 20).build());
        }

        addRenderableWidget(Button.builder(
                Component.translatable("screen.no_soliciting.show_boundary"),
                button -> {
                    BoundaryRenderer.show(signPos, NoSolicitingSignBlock.RADII[selectedIndex]);
                    onClose();
                }
        ).bounds(width / 2 - 100, radiusY + 34, 200, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> onClose()
        ).bounds(width / 2 - 100, radiusY + 60, 200, 20).build());

        updateButtons();
    }

    private void selectRadius(int index) {
        selectedIndex = index;
        updateButtons();
        ClientPlayNetworking.send(new UpdateRadiusPayload(signPos, selectedIndex));
    }

    private void updateButtons() {
        for (int index = 0; index < radiusButtons.length; index++) {
            if (radiusButtons[index] != null) {
                radiusButtons[index].active = index != selectedIndex;
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, height / 2 - 72, 0xFFFFFFFF);
        graphics.centeredText(
                font,
                Component.translatable("screen.no_soliciting.radius"),
                width / 2,
                height / 2 - 43,
                0xFFA0A0A0
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}


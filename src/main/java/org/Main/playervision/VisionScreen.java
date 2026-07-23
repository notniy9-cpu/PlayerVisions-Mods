package org.Main.playervision;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class VisionScreen extends Screen {

    protected VisionScreen() {
        super(Component.literal("Player Vision Menu"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(Button.builder(
                Component.literal("Общая подсветка: " + (PlayerVisionMod.isPlayersGlow ? "ВКЛ" : "ВЫКЛ")),
                button -> {
                    PlayerVisionMod.isPlayersGlow = !PlayerVisionMod.isPlayersGlow;
                    button.setMessage(Component.literal("Общая подсветка: " + (PlayerVisionMod.isPlayersGlow ? "ВКЛ" : "ВЫКЛ")));
                }
        ).bounds(centerX - 100, centerY - 60, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Сквозь блоки: " + (PlayerVisionMod.seeThroughBlocks ? "ВКЛ" : "ВЫКЛ")),
                button -> {
                    PlayerVisionMod.seeThroughBlocks = !PlayerVisionMod.seeThroughBlocks;
                    button.setMessage(Component.literal("Сквозь блоки: " + (PlayerVisionMod.seeThroughBlocks ? "ВКЛ" : "ВЫКЛ")));
                }
        ).bounds(centerX - 100, centerY - 35, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Метки и Дистанция: " + (PlayerVisionMod.isTagsEnabled ? "ВКЛ" : "ВЫКЛ")),
                button -> {
                    PlayerVisionMod.isTagsEnabled = !PlayerVisionMod.isTagsEnabled;
                    button.setMessage(Component.literal("Метки: " + (PlayerVisionMod.isTagsEnabled ? "ВКЛ" : "ВЫКЛ")));
                }
        ).bounds(centerX - 100, centerY - 10, 200, 20).build());

        // Кнопка перехода в список игроков для выбора и настройки их цветов
        this.addRenderableWidget(Button.builder(
                Component.literal("Управление игроками и цветом"),
                button -> this.minecraft.setScreen(new PlayerListScreen(this))
        ).bounds(centerX - 100, centerY + 15, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

package org.Main.playervision;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class VisionScreen extends Screen {

    protected VisionScreen() {
        super(Component.literal("Anti-Cheat Audit Tool (ESP)"));
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
        ).bounds(centerX - 100, centerY - 70, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Сквозь блоки: " + (PlayerVisionMod.seeThroughBlocks ? "ВКЛ" : "ВЫКЛ")),
                button -> {
                    PlayerVisionMod.seeThroughBlocks = !PlayerVisionMod.seeThroughBlocks;
                    button.setMessage(Component.literal("Сквозь блоки: " + (PlayerVisionMod.seeThroughBlocks ? "ВКЛ" : "ВЫКЛ")));
                }
        ).bounds(centerX - 100, centerY - 45, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Метки и Дистанция: " + (PlayerVisionMod.isTagsEnabled ? "ВКЛ" : "ВЫКЛ")),
                button -> {
                    PlayerVisionMod.isTagsEnabled = !PlayerVisionMod.isTagsEnabled;
                    button.setMessage(Component.literal("Метки: " + (PlayerVisionMod.isTagsEnabled ? "ВКЛ" : "ВЫКЛ")));
                }
        ).bounds(centerX - 100, centerY - 20, 200, 20).build());

        // Новая кнопка перехода к детальным настройкам меток
        this.addRenderableWidget(Button.builder(
                Component.literal("Настройка вида меток..."),
                button -> this.minecraft.setScreen(new TagConfigScreen(this))
        ).bounds(centerX - 100, centerY + 5, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Дистанция работы: " + PlayerVisionMod.maxDistance + " б."),
                button -> {
                    PlayerVisionMod.maxDistance += 32;
                    if (PlayerVisionMod.maxDistance > 356) PlayerVisionMod.maxDistance = 28;
                    button.setMessage(Component.literal("Дистанция работы: " + PlayerVisionMod.maxDistance + " б."));
                }
        ).bounds(centerX - 100, centerY + 30, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Управление игроками и цветом"),
                button -> this.minecraft.setScreen(new PlayerListScreen(this))
        ).bounds(centerX - 100, centerY + 55, 200, 20).build());
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

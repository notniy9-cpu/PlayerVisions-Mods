package org.Main.playervision;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TagConfigScreen extends Screen {
    private final Screen parent;

    public TagConfigScreen(Screen parent) {
        super(Component.literal("Настройка параметров меток"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Переключатель показа высоты (Y координаты)
        this.addRenderableWidget(Button.builder(
                Component.literal("Показывать высоту: " + (PlayerVisionMod.showHeight ? "ДА" : "НЕТ")),
                button -> {
                    PlayerVisionMod.showHeight = !PlayerVisionMod.showHeight;
                    button.setMessage(Component.literal("Показывать высоту: " + (PlayerVisionMod.showHeight ? "ДА" : "НЕТ")));
                }
        ).bounds(centerX - 100, centerY - 30, 200, 20).build());

        // Циклическое изменение размера шрифта и стрелок
        this.addRenderableWidget(Button.builder(
                Component.literal("Размер меток: " + PlayerVisionMod.tagScale + "x"),
                button -> {
                    if (PlayerVisionMod.tagScale == 1.0f) PlayerVisionMod.tagScale = 1.5f;
                    else if (PlayerVisionMod.tagScale == 1.5f) PlayerVisionMod.tagScale = 2.0f;
                    else if (PlayerVisionMod.tagScale == 2.0f) PlayerVisionMod.tagScale = 0.5f;
                    else PlayerVisionMod.tagScale = 1.0f;
                    button.setMessage(Component.literal("Размер меток: " + PlayerVisionMod.tagScale + "x"));
                }
        ).bounds(centerX - 100, centerY - 5, 200, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Готово"),
                button -> this.minecraft.setScreen(this.parent)
        ).bounds(centerX - 100, centerY + 30, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}

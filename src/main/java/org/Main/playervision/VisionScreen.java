package org.Main.playervision;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class VisionScreen extends Screen {
    private EditBox itemInput;

    protected VisionScreen() {
        super(Component.literal("Player Vision Menu"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Кнопка: Подсветка игроков
        this.addRenderableWidget(Button.builder(
                Component.literal("Игроки: " + (PlayerVisionMod.isPlayersGlow ? "ВКЛ" : "ВЫКЛ")),
                button -> {
                    PlayerVisionMod.isPlayersGlow = !PlayerVisionMod.isPlayersGlow;
                    button.setMessage(Component.literal("Игроки: " + (PlayerVisionMod.isPlayersGlow ? "ВКЛ" : "ВЫКЛ")));
                }
        ).bounds(centerX - 100, centerY - 85, 200, 20).build());

        // Кнопка: Скрывать наблюдателей
        this.addRenderableWidget(Button.builder(
                Component.literal("Скрывать Наблюдателей: " + (PlayerVisionMod.ignoreSpectators ? "ДА" : "НЕТ")),
                button -> {
                    PlayerVisionMod.ignoreSpectators = !PlayerVisionMod.ignoreSpectators;
                    button.setMessage(Component.literal("Скрывать Наблюдателей: " + (PlayerVisionMod.ignoreSpectators ? "ДА" : "НЕТ")));
                }
        ).bounds(centerX - 100, centerY - 60, 200, 20).build());

        // Кнопка: Подсветка предметов
        this.addRenderableWidget(Button.builder(
                Component.literal("Предметы: " + (PlayerVisionMod.isItemsGlow ? "ВКЛ" : "ВЫКЛ")),
                button -> {
                    PlayerVisionMod.isItemsGlow = !PlayerVisionMod.isItemsGlow;
                    button.setMessage(Component.literal("Предметы: " + (PlayerVisionMod.isItemsGlow ? "ВКЛ" : "ВЫКЛ")));
                }
        ).bounds(centerX - 100, centerY - 35, 200, 20).build());

        // Кнопка: Метки
        this.addRenderableWidget(Button.builder(
                Component.literal("Метки и Дистанция: " + (PlayerVisionMod.isTagsEnabled ? "ВКЛ" : "ВЫКЛ")),
                button -> {
                    PlayerVisionMod.isTagsEnabled = !PlayerVisionMod.isTagsEnabled;
                    button.setMessage(Component.literal("Метки: " + (PlayerVisionMod.isTagsEnabled ? "ВКЛ" : "ВЫКЛ")));
                }
        ).bounds(centerX - 100, centerY - 10, 200, 20).build());

        // Переключатель радиуса
        this.addRenderableWidget(Button.builder(
                Component.literal("Радиус: " + (int)PlayerVisionMod.maxDistance + " б."),
                button -> {
                    if (PlayerVisionMod.maxDistance == 32.0) PlayerVisionMod.maxDistance = 64.0;
                    else if (PlayerVisionMod.maxDistance == 64.0) PlayerVisionMod.maxDistance = 128.0;
                    else PlayerVisionMod.maxDistance = 32.0;
                    button.setMessage(Component.literal("Радиус: " + (int)PlayerVisionMod.maxDistance + " б."));
                }
        ).bounds(centerX - 100, centerY + 15, 200, 20).build());

        // КНОПКА ВЫБОРА ИГРОКОВ ИЗ СПИСКА НА СЕРВЕРЕ (Взамен старого ввода)
        this.addRenderableWidget(Button.builder(
                Component.literal("Выбрать игроков из списка"),
                button -> this.minecraft.setScreen(new PlayerListScreen(this))
        ).bounds(centerX - 100, centerY + 40, 200, 20).build());

        // Ввод названий предметов
        this.itemInput = new EditBox(this.font, centerX - 100, centerY + 65, 135, 20, Component.literal("Предмет"));
        this.addRenderableWidget(itemInput);
        this.addRenderableWidget(Button.builder(Component.literal("+Вещь"), button -> {
            String item = itemInput.getValue().trim();
            if (!item.isEmpty()) {
                PlayerVisionMod.targetItems.add(item.toLowerCase());
                itemInput.setValue("");
            }
        }).bounds(centerX + 40, centerY + 65, 60, 20).build());
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

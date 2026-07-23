package org.Main.playervision;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.Collection;

public class PlayerListScreen extends Screen {
    private final Screen parent;
    private PlayerList list;

    // Массив доступных цветов (Красный, Зеленый, Синий, Желтый, Белый)
    private static final int[] COLORS = {0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00, 0xFFFFFF};
    private static final String[] COLOR_NAMES = {"Красный", "Зеленый", "Синий", "Желтый", "Белый"};

    public PlayerListScreen(Screen parent) {
        super(Component.literal("Настройка игроков"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.list = new PlayerList(this.minecraft, this.width, this.height, 32, this.height - 40, 24);
        this.addWidget(this.list);

        this.addRenderableWidget(Button.builder(Component.literal("Назад"),
                button -> this.minecraft.setScreen(this.parent)
        ).bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.list.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    class PlayerList extends ObjectSelectionList<PlayerList.Entry> {
        public PlayerList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
            super(mc, width, height, top, bottom, itemHeight);
            if (mc.getConnection() != null) {
                Collection<PlayerInfo> players = mc.getConnection().getOnlinePlayers();
                for (PlayerInfo info : players) {
                    String name = info.getProfile().getName();
                    if (!name.equalsIgnoreCase(mc.player.getName().getString())) {
                        this.addEntry(new Entry(name));
                    }
                }
            }
        }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final String playerName;
            private final Button selectButton;
            private final Button colorButton;
            private int currentColorIdx = 0;

            public Entry(String name) {
                this.playerName = name;
                String key = name.toLowerCase();

                this.selectButton = Button.builder(
                        Component.literal(PlayerVisionMod.targetPlayers.contains(key) ? "Убрать" : "Выбрать"),
                        button -> {
                            if (PlayerVisionMod.targetPlayers.contains(key)) {
                                PlayerVisionMod.targetPlayers.remove(key);
                                button.setMessage(Component.literal("Выбрать"));
                            } else {
                                PlayerVisionMod.targetPlayers.add(key);
                                button.setMessage(Component.literal("Убрать"));
                            }
                        }
                ).bounds(0, 0, 60, 20).build();

                this.colorButton = Button.builder(
                        Component.literal(getCurrentColorName(key)),
                        button -> {
                            currentColorIdx = (currentColorIdx + 1) % COLORS.length;
                            PlayerVisionMod.playerColors.put(key, COLORS[currentColorIdx]);
                            button.setMessage(Component.literal(COLOR_NAMES[currentColorIdx]));
                        }
                ).bounds(0, 0, 75, 20).build();

                // Инициализация индекса цвета, если он уже был сохранен
                if (!PlayerVisionMod.playerColors.containsKey(key)) {
                    PlayerVisionMod.playerColors.put(key, COLORS[0]); // По умолчанию красный
                }
            }

            private String getCurrentColorName(String key) {
                int color = PlayerVisionMod.playerColors.getOrDefault(key, COLORS[0]);
                for (int i = 0; i < COLORS.length; i++) {
                    if (COLORS[i] == color) {
                        currentColorIdx = i;
                        return COLOR_NAMES[i];
                    }
                }
                return COLOR_NAMES[0];
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
                graphics.drawString(PlayerListScreen.this.font, this.playerName, left + 10, top + 6, 0xFFFFFF);

                this.colorButton.setX(left + width - 150);
                this.colorButton.setY(top);
                this.colorButton.render(graphics, mouseX, mouseY, partialTick);

                this.selectButton.setX(left + width - 70);
                this.selectButton.setY(top);
                this.selectButton.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return this.selectButton.mouseClicked(mouseX, mouseY, button) || this.colorButton.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public Component getNarration() {
                return Component.literal(playerName);
            }
        }
    }
}

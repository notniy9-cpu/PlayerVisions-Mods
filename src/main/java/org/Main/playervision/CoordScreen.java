package org.Main.playervision;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

public class CoordScreen extends Screen {
    private CoordList list;
    public static String lastInterceptedCoords = "";
    public static String lastRequestedPlayer = "";

    public CoordScreen() {
        super(Component.literal("Точные координаты игроков"));
    }

    @Override
    protected void init() {
        this.list = new CoordList(this.minecraft, this.width, this.height, 32, this.height - 40, 24);
        this.addWidget(this.list);

        this.addRenderableWidget(Button.builder(Component.literal("Закрыть"),
                button -> this.minecraft.setScreen(null)
        ).bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.list.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    class CoordList extends ObjectSelectionList<CoordList.Entry> {
        public CoordList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
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

        @Override
        public int getRowWidth() {
            return 320;
        }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final String playerName;
            private final Button actionButton;
            private String displayCoords = "";

            public Entry(String name) {
                this.playerName = name;
                String key = name.toLowerCase();

                this.actionButton = Button.builder(
                        Component.literal("Координаты"),
                        button -> {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.player != null && mc.level != null) {
                                Vec3 localPos = PlayerVisionMod.FarPositionsCache.get(key);

                                // ИСПРАВЛЕНИЕ: Безопасный поиск игрока в мире по его имени, чтобы узнать, загружен ли он рядом
                                Player targetEntity = null;
                                for (Player p : mc.level.players()) {
                                    if (p.getName().getString().equalsIgnoreCase(playerName)) {
                                        targetEntity = p;
                                        break;
                                    }
                                }

                                // Если игрок загружен рядом в мире, берем координаты из кэша рендера кадра
                                if (localPos != null && targetEntity != null) {
                                    this.displayCoords = String.format("X: %d Y: %d Z: %d", (int)localPos.x, (int)localPos.y, (int)localPos.z);
                                } else {
                                    // Если игрока рядом нет (он слишком далеко), отправляем скрытый запрос серверу
                                    lastRequestedPlayer = key;
                                    lastInterceptedCoords = "";
                                    mc.player.connection.sendChat("/data get entity " + playerName + " Pos");
                                    this.displayCoords = "Запрос...";
                                }
                            }
                        }
                ).bounds(0, 0, 95, 20).build();
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
                graphics.drawString(CoordScreen.this.font, this.playerName, left + 5, top + 6, 0xFFFFFF);

                if (this.displayCoords.equals("Запрос...") && lastRequestedPlayer.equals(playerName.toLowerCase()) && !lastInterceptedCoords.isEmpty()) {
                    this.displayCoords = lastInterceptedCoords;
                }

                if (!displayCoords.isEmpty()) {
                    graphics.drawString(CoordScreen.this.font, displayCoords, left + 85, top + 6, 0x55FF55);
                }

                this.actionButton.setX(left + width - 100);
                this.actionButton.setY(top);
                this.actionButton.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return this.actionButton.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public Component getNarration() {
                return Component.literal(playerName);
            }
        }
    }
}

package org.Main.playervision;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RenderEventHandler {

    // Скрытый перехват данных чата сервера для получения координат на любой дистанции
    @SubscribeEvent
    public void onClientChatReceived(ClientChatReceivedEvent event) {
        String msg = event.getMessage().getString();

        if (msg.contains("has the following entity data:") || msg.contains("имеет следующие данные сущности:")) {
            Pattern pattern = Pattern.compile("\\[(-?\\d+\\.\\d+)[d|f]?,\\s*(-?\\d+\\.\\d+)[d|f]?,\\s*(-?\\d+\\.\\d+)[d|f]?\\]");
            Matcher matcher = pattern.matcher(msg);

            if (matcher.find()) {
                try {
                    int x = (int) Double.parseDouble(matcher.group(1));
                    int y = (int) Double.parseDouble(matcher.group(2));
                    int z = (int) Double.parseDouble(matcher.group(3));

                    String coordsText = String.format("X:%d Y:%d Z:%d", x, y, z);
                    CoordScreen.lastInterceptedCoords = coordsText;

                    PlayerVisionMod.FarPositionsCache.put(CoordScreen.lastRequestedPlayer, new Vec3(x, y, z));

                    // Скрываем это сообщение из чата, чтобы не спамить
                    event.setCanceled(true);
                } catch (Exception ignored) {}
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.level == null || mc.player == null) return;

        Set<String> currentOnlinePlayers = new HashSet<>();
        for (Player player : mc.level.players()) {
            if (player == mc.player || !player.isAlive()) continue;

            String nameKey = player.getName().getString().toLowerCase();
            currentOnlinePlayers.add(nameKey);
            PlayerVisionMod.FarPositionsCache.put(nameKey, player.position());
        }
        if (!(mc.screen instanceof CoordScreen)) {
            PlayerVisionMod.FarPositionsCache.keySet().removeIf(key -> !currentOnlinePlayers.contains(key));
        }
    }

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);

        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());

        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            if (PlayerVisionMod.ignoreSpectators && player.isSpectator()) continue;

            double dist = mc.player.distanceTo(player);
            if (dist > PlayerVisionMod.maxDistance) continue;

            String key = player.getName().getString().toLowerCase();
            if (PlayerVisionMod.isPlayersGlow || PlayerVisionMod.targetPlayers.contains(key)) {
                int colorRGB = PlayerVisionMod.playerColors.getOrDefault(key, 0xFF0000);
                float r = ((colorRGB >> 16) & 0xFF) / 255.0F;
                float g = ((colorRGB >> 8) & 0xFF) / 255.0F;
                float b = (colorRGB & 0xFF) / 255.0F;

                AABB box = player.getBoundingBox();
                LevelRenderer.renderLineBox(poseStack, buffer, box, r, g, b, 1.0F);
            }
        }
        bufferSource.endBatch();
        poseStack.popPose();
    }

    // ОТРИСОВКА HUD И СТРЕЛОК НА ЭКРАНЕ
    @SubscribeEvent
    public void onRenderGui(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int centerX = screenWidth / 2;
        int centerY = mc.getWindow().getGuiScaledHeight() / 2;

        // ВЫВОД СОБСТВЕННЫХ КООРДИНАТ В ВЕРХНИЙ ПРАВЫЙ УГОЛ ЭКРАНА
        String myCoordsText = String.format("XYZ: %d, %d, %d", (int)mc.player.getX(), (int)mc.player.getY(), (int)mc.player.getZ());
        int textWidth = font.width(myCoordsText);
        // Рисуем текст с небольшим отступом в 10 пикселей от правого и верхнего краев монитора
        graphics.drawString(font, myCoordsText, screenWidth - textWidth - 10, 10, 0x55FF55, true); // Зеленый цвет

        // Дальнейший рендер стрелочек (если включены)
        if (!PlayerVisionMod.isTagsEnabled) return;

        for (Map.Entry<String, Vec3> entry : PlayerVisionMod.FarPositionsCache.entrySet()) {
            String playerKey = entry.getKey();
            Vec3 targetPos = entry.getValue();

            double distance = mc.player.position().distanceTo(targetPos);
            if (distance > PlayerVisionMod.maxTagDistance) continue;

            if (!PlayerVisionMod.radarPlayers.isEmpty() && !PlayerVisionMod.radarPlayers.contains(playerKey)) {
                continue;
            }

            Vec3 playerLook = mc.player.getLookAngle();
            Vec3 targetDir = targetPos.subtract(mc.player.position()).normalize();

            double angleTargetXZ = Math.atan2(targetDir.z, targetDir.x);
            double angleLookXZ = Math.atan2(playerLook.z, playerLook.x);
            double relativeAngleXZ = angleTargetXZ - angleLookXZ - Math.toRadians(90);

            double pitchTarget = Math.asin(targetDir.y);
            double pitchLook = Math.asin(playerLook.y);
            double relativePitch = pitchTarget - pitchLook;

            int radius = 55;
            int arrowX = centerX + (int) (Math.cos(relativeAngleXZ) * radius);
            int arrowY = centerY + (int) (Math.sin(relativeAngleXZ) * radius) - (int) (relativePitch * 45);

            PoseStack poseStack = graphics.pose();
            poseStack.pushPose();
            poseStack.translate(arrowX, arrowY, 0);
            poseStack.scale(PlayerVisionMod.tagScale, PlayerVisionMod.tagScale, 1.0f);

            float rotationAngle = (float) (relativeAngleXZ + Math.toRadians(90));
            poseStack.mulPose(Axis.ZP.rotation(rotationAngle));

            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            Tesselator tess = Tesselator.getInstance();
            BufferBuilder buf = tess.getBuilder();
            Matrix4f matrix = poseStack.last().pose();

            int r = 0, g = 210, b = 255;
            double heightDiff = targetPos.y - mc.player.getY();
            if (heightDiff > 1.5) { r = 255; g = 30; b = 30; }
            else if (heightDiff < -1.5) { r = 30; g = 80; b = 255; }

            buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            buf.vertex(matrix, 0, -9, 0).color(r, g, b, 255).endVertex();
            buf.vertex(matrix, -3, 4, 0).color((int)(r*0.7), (int)(g*0.7), (int)(b*0.7), 255).endVertex();
            buf.vertex(matrix, 3, 4, 0).color((int)(r*0.7), (int)(g*0.7), (int)(b*0.7), 255).endVertex();
            tess.end();

            RenderSystem.enableCull();
            poseStack.popPose();

            int textX = centerX + (int) (Math.cos(relativeAngleXZ) * (radius + (24 * PlayerVisionMod.tagScale)));
            int textY = centerY + (int) (Math.sin(relativeAngleXZ) * (radius + (24 * PlayerVisionMod.tagScale))) - (int) (relativePitch * 45);

            String textName = playerKey.substring(0, 1).toUpperCase() + playerKey.substring(1);
            String textDist = String.format("%.1f б.", distance);
            if (PlayerVisionMod.showHeight) {
                textDist += String.format(" [Y: %d]", (int)targetPos.y);
            }

            graphics.drawCenteredString(font, textName, textX, textY - 9, 0xFFFFFF);
            graphics.drawCenteredString(font, textDist, textX, textY + 1, 0x55FF55);
        }
    }
}

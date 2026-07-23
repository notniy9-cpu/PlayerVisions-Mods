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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.Map;

public class RenderEventHandler {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.level == null) return;

        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            String nameKey = player.getName().getString().toLowerCase();
            PlayerVisionMod.FarPositionsCache.put(nameKey, player.position());
        }
    }

    // ХАРДКОРНЫЕ 3D ЧАМСЫ: СИЛУЭТ ИГРОКА ПРОСВЕЧИВАЕТ СКВОЗЬ БЛОКИ
    @SubscribeEvent
    public void onRenderPlayerModel(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player) return; // Себя не подсвечиваем
        if (PlayerVisionMod.ignoreSpectators && player.isSpectator()) return;

        double dist = mc.player.distanceTo(player);
        if (dist > PlayerVisionMod.maxDistance) return;

        String key = player.getName().getString().toLowerCase();
        boolean isChosen = PlayerVisionMod.targetPlayers.contains(key);

        if (PlayerVisionMod.isPlayersGlow || isChosen) {
            // Если включена функция "Сквозь блоки", физически убираем препятствия перед рендером скина игрока
            if (PlayerVisionMod.seeThroughBlocks) {
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
            }

            // Получаем и устанавливаем кастомный цвет
            int colorRGB = PlayerVisionMod.playerColors.getOrDefault(key, 0xFF0000);
            float r = ((colorRGB >> 16) & 0xFF) / 255.0F;
            float g = ((colorRGB >> 8) & 0xFF) / 255.0F;
            float b = (colorRGB & 0xFF) / 255.0F;

            // Окрашиваем модельку игрока в неоновый цвет
            RenderSystem.setShaderColor(r, g, b, 1.0F);
        }
    }

    // Сброс графических стейтов ПОСЛЕ того, как модель игрока отрисовалась
    @SubscribeEvent
    public void onRenderPlayerModelPost(RenderLivingEvent.Post<?, ?> event) {
        if (!(event.getEntity() instanceof Player)) return;

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // Возвращаем обычные цвета миру
    }

    // 2. ДАЛЬНОБОЙНЫЕ СТРЕЛКИ РАДАРА
    @SubscribeEvent
    public void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!PlayerVisionMod.isTagsEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;

        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int centerY = mc.getWindow().getGuiScaledHeight() / 2;

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

            double angleTarget = Math.atan2(targetDir.z, targetDir.x);
            double angleLook = Math.atan2(playerLook.z, playerLook.x);
            double relativeAngle = angleTarget - angleLook - Math.toRadians(90);

            int radius = 55;
            int arrowX = centerX + (int) (Math.cos(relativeAngle) * radius);
            int arrowY = centerY + (int) (Math.sin(relativeAngle) * radius);

            PoseStack poseStack = graphics.pose();
            poseStack.pushPose();
            poseStack.translate(arrowX, arrowY, 0);

            poseStack.scale(PlayerVisionMod.tagScale, PlayerVisionMod.tagScale, 1.0f);
            poseStack.mulPose(Axis.ZP.rotation((float) (relativeAngle + Math.toRadians(90))));

            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            Tesselator tess = Tesselator.getInstance();
            BufferBuilder buf = tess.getBuilder();
            Matrix4f matrix = poseStack.last().pose();

            buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            buf.vertex(matrix, 0, -6, 0).color(255, 30, 30, 255).endVertex();
            buf.vertex(matrix, -5, 4, 0).color(180, 20, 20, 255).endVertex();
            buf.vertex(matrix, 5, 4, 0).color(180, 20, 20, 255).endVertex();
            tess.end();

            RenderSystem.enableCull();
            poseStack.popPose();

            int textX = centerX + (int) (Math.cos(relativeAngle) * (radius + (22 * PlayerVisionMod.tagScale)));
            int textY = centerY + (int) (Math.sin(relativeAngle) * (radius + (22 * PlayerVisionMod.tagScale)));

            String textName = playerKey.substring(0, 1).toUpperCase() + playerKey.substring(1);
            double heightDiff = targetPos.y - mc.player.getY();
            String textDist = String.format("%.1f б.", distance);

            if (PlayerVisionMod.showHeight) {
                textDist += String.format(" (Y: %+.1f)", heightDiff);
            }

            graphics.drawCenteredString(font, textName, textX, textY - 9, 0xFFFFFF);
            graphics.drawCenteredString(font, textDist, textX, textY + 1, 0x55FF55);
        }
    }
}

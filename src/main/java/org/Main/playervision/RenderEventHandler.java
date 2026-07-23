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
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

public class RenderEventHandler {

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        // Переключение на этап после рендеринга блочных сущностей (наиболее стабильная точка для перекрытия буфера)
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        if (PlayerVisionMod.seeThroughBlocks) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());

        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            if (PlayerVisionMod.ignoreSpectators && player.isSpectator()) continue;

            double dist = mc.player.distanceTo(player);
            if (dist > PlayerVisionMod.maxDistance) continue;

            String key = player.getName().getString().toLowerCase();
            boolean isChosen = PlayerVisionMod.targetPlayers.contains(key);

            if (PlayerVisionMod.isPlayersGlow || isChosen) {
                // Извлечение индивидуального цвета игрока из карты настроек
                int colorRGB = PlayerVisionMod.playerColors.getOrDefault(key, 0xFF0000);
                float r = ((colorRGB >> 16) & 0xFF) / 255.0F;
                float g = ((colorRGB >> 8) & 0xFF) / 255.0F;
                float b = (colorRGB & 0xFF) / 255.0F;

                AABB box = player.getBoundingBox();
                LevelRenderer.renderLineBox(poseStack, buffer, box, r, g, b, 1.0F);
            }
        }

        bufferSource.endBatch();

        if (PlayerVisionMod.seeThroughBlocks) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        }
        poseStack.popPose();
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!PlayerVisionMod.isTagsEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;

        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int centerY = mc.getWindow().getGuiScaledHeight() / 2;

        for (Player target : mc.level.players()) {
            if (target == mc.player) continue;
            if (PlayerVisionMod.ignoreSpectators && target.isSpectator()) continue;

            double distance = mc.player.distanceTo(target);
            if (distance > PlayerVisionMod.maxDistance) continue;

            String key = target.getName().getString().toLowerCase();
            if (!PlayerVisionMod.targetPlayers.isEmpty() && !PlayerVisionMod.targetPlayers.contains(key)) {
                continue;
            }

            Vec3 playerLook = mc.player.getLookAngle();
            Vec3 targetDir = target.position().subtract(mc.player.position()).normalize();

            double angleTarget = Math.atan2(targetDir.z, targetDir.x);
            double angleLook = Math.atan2(playerLook.z, playerLook.x);
            double relativeAngle = angleTarget - angleLook - Math.toRadians(90);

            int radius = 55;
            int arrowX = centerX + (int) (Math.cos(relativeAngle) * radius);
            int arrowY = centerY + (int) (Math.sin(relativeAngle) * radius);

            PoseStack poseStack = graphics.pose();
            poseStack.pushPose();
            poseStack.translate(arrowX, arrowY, 0);
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

            int textX = centerX + (int) (Math.cos(relativeAngle) * (radius + 22));
            int textY = centerY + (int) (Math.sin(relativeAngle) * (radius + 22));

            String textName = target.getName().getString();
            String textDist = String.format("%.1f б.", distance);

            graphics.drawCenteredString(font, textName, textX, textY - 9, 0xFFFFFF);
            graphics.drawCenteredString(font, textDist, textX, textY + 1, 0x55FF55);
        }
    }
}

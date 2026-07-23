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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

public class RenderEventHandler {

    // 1. ИСПРАВЛЕННЫЙ 3D ESP (СТОПРОЦЕНТНО ВИДНО СКВОЗЬ СТЕНЫ)
    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        // Используем стадию после отрисовки частиц, когда весь мир уже построен
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        // Сдвигаем матрицу мира относительно камеры игрока
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        // Отключаем проверку глубины (Z-буфер) на аппаратном уровне OpenGL
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Создаем независимый буфер для мгновенного вывода линий поверх блоков
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) continue;

            double dist = mc.player.distanceTo(entity);
            if (dist > PlayerVisionMod.maxDistance) continue;

            boolean shouldDraw = false;
            float r = 0.0F, g = 1.0F, b = 0.0F; // Зеленый по умолчанию

            // Проверка игроков
            if (entity instanceof Player player) {
                if (PlayerVisionMod.ignoreSpectators && player.isSpectator()) continue;

                if (PlayerVisionMod.isPlayersGlow || PlayerVisionMod.targetPlayers.contains(player.getName().getString().toLowerCase())) {
                    shouldDraw = true;
                    r = 1.0F; g = 0.0F; b = 0.0F; // Игроки — Красные
                }
            }
            // Проверка предметов (вещей на земле)
            else if (entity instanceof ItemEntity item) {
                String itemName = item.getItem().getDescriptionId().toLowerCase();
                boolean isTargetItem = false;

                for (String savedItem : PlayerVisionMod.targetItems) {
                    if (itemName.contains(savedItem)) {
                        isTargetItem = true;
                        break;
                    }
                }

                if (PlayerVisionMod.isItemsGlow || isTargetItem) {
                    shouldDraw = true;
                    r = 1.0F; g = 1.0F; b = 0.0F; // Предметы — Желтые
                }
            }

            // Отрисовка идеального 3D-бокса вокруг цели
            if (shouldDraw) {
                AABB box = entity.getBoundingBox();
                LevelRenderer.renderLineBox(poseStack, buffer, box, r, g, b, 1.0F);
            }
        }

        // Выводим все нарисованные линии на экран
        bufferSource.endBatch();

        // Восстанавливаем дефолтное состояние графического движка Minecraft
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        poseStack.popPose();
    }

    // 2. ИДЕАЛЬНО РОВНЫЕ ТРЕУГОЛЬНЫЕ СТРЕЛКИ НАПРАВЛЕНИЯ И ДИСТАНЦИЯ ВОКРУГ ПРИЦЕЛА
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

            if (!PlayerVisionMod.targetPlayers.isEmpty() && !PlayerVisionMod.targetPlayers.contains(target.getName().getString().toLowerCase())) {
                continue;
            }

            // Математика векторов для определения направления
            Vec3 playerLook = mc.player.getLookAngle();
            Vec3 targetDir = target.position().subtract(mc.player.position()).normalize();

            double angleTarget = Math.atan2(targetDir.z, targetDir.x);
            double angleLook = Math.atan2(playerLook.z, playerLook.x);
            double relativeAngle = angleTarget - angleLook - Math.toRadians(90);

            int radius = 55; // На каком расстоянии от прицела вращаются маркеры
            int arrowX = centerX + (int) (Math.cos(relativeAngle) * radius);
            int arrowY = centerY + (int) (Math.sin(relativeAngle) * radius);

            PoseStack poseStack = graphics.pose();
            poseStack.pushPose();
            poseStack.translate(arrowX, arrowY, 0);

            // Вращаем координатную сетку HUD точно по вычисленному направлению к игроку
            poseStack.mulPose(Axis.ZP.rotation((float) (relativeAngle + Math.toRadians(90))));

            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            Tesselator tess = Tesselator.getInstance();
            BufferBuilder buf = tess.getBuilder();
            Matrix4f matrix = poseStack.last().pose();

            // Отрисовка векторного треугольника острием к цели
            buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            buf.vertex(matrix, 0, -6, 0).color(255, 30, 30, 255).endVertex();   // Остриё стрелки
            buf.vertex(matrix, -5, 4, 0).color(180, 20, 20, 255).endVertex();   // Левый нижний угол
            buf.vertex(matrix, 5, 4, 0).color(180, 20, 20, 255).endVertex();    // Правый нижний угол
            tess.end();

            RenderSystem.enableCull();
            poseStack.popPose();

            // Рассчитываем координаты текста, чтобы он следовал рядом со стрелочкой
            int textX = centerX + (int) (Math.cos(relativeAngle) * (radius + 22));
            int textY = centerY + (int) (Math.sin(relativeAngle) * (radius + 22));

            String textName = target.getName().getString();
            String textDist = String.format("%.1f б.", distance); // Показываем точную дистанцию в блоках

            // Отрисовка текста
            graphics.drawCenteredString(font, textName, textX, textY - 9, 0xFFFFFF);
            graphics.drawCenteredString(font, textDist, textX, textY + 1, 0x55FF55); // Зеленый цвет для метров
        }
    }
}

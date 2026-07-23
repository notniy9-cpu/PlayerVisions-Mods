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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.Map;

public class RenderEventHandler {

    // Кэшируем позиции игроков каждый тик, чтобы обходить серверные лимиты дальности и приседание на Shift
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.level == null) return;

        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            String nameKey = player.getName().getString().toLowerCase();
            // Записываем точные координаты, которые сервер присылает клиенту (даже на шифте)
            PlayerVisionMod.FarPositionsCache.put(nameKey, player.position());
        }
    }

    // 1. АБСОЛЮТНЫЙ ОБХОД СТЕН ЧЕРЕЗ КАСТОМНЫЙ ПРОХОД ЛИНИЙ (3D BOX ESP)
    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        // Стадия AFTER_PARTICLES выполняется, когда все основные твердые блоки уже отрисованы
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        // Привязываем отрисовку к позиции камеры игрока
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        // Отключаем проверку глубины (Z-буфер) графического движка
        if (PlayerVisionMod.seeThroughBlocks) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // Используем шейдер линий, разработанный специально для прорисовки хитбоксов и контуров
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);

        // Создаем независимый буфер вывода
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.lines());

        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            if (PlayerVisionMod.ignoreSpectators && player.isSpectator()) continue;

            double dist = mc.player.distanceTo(player);
            if (dist > PlayerVisionMod.maxDistance) continue; // Учет динамического радиуса до 356 блоков

            String key = player.getName().getString().toLowerCase();
            boolean isChosen = PlayerVisionMod.targetPlayers.contains(key);

            if (PlayerVisionMod.isPlayersGlow || isChosen) {
                // Извлекаем кастомный цвет игрока из сохраненных настроек
                int colorRGB = PlayerVisionMod.playerColors.getOrDefault(key, 0xFF0000);
                float r = ((colorRGB >> 16) & 0xFF) / 255.0F;
                float g = ((colorRGB >> 8) & 0xFF) / 255.0F;
                float b = (colorRGB & 0xFF) / 255.0F;

                // Получаем хитбокс (AABB), который автоматически сжимается по высоте, если игрок на Shift
                AABB box = player.getBoundingBox();

                // Рендерим 3D-каркас вокруг игрока
                LevelRenderer.renderLineBox(poseStack, buffer, box, r, g, b, 1.0F);
            }
        }

        // Принудительно выводим нарисованные линии поверх кадра игры
        bufferSource.endBatch();

        // Возвращаем графические стейты Minecraft в исходное состояние
        if (PlayerVisionMod.seeThroughBlocks) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        }
        poseStack.popPose();
    }

    // 2. ДАЛЬНОБОЙНЫЕ СТРЕЛКИ С НАСТРОЙКОЙ МАСШТАБА И ВЫВОДОМ ВЫСОТЫ
    @SubscribeEvent
    public void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!PlayerVisionMod.isTagsEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;

        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int centerY = mc.getWindow().getGuiScaledHeight() / 2;

        // Сканируем кэш позиций для обхода серверного лимита дальности
        for (Map.Entry<String, Vec3> entry : PlayerVisionMod.FarPositionsCache.entrySet()) {
            String playerKey = entry.getKey();
            Vec3 targetPos = entry.getValue();

            double distance = mc.player.position().distanceTo(targetPos);
            if (distance > PlayerVisionMod.maxDistance) continue;

            if (!PlayerVisionMod.targetPlayers.isEmpty() && !PlayerVisionMod.targetPlayers.contains(playerKey)) {
                continue;
            }

            // Математический расчет углов относительно взгляда
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

            // Применяем кастомный размер (масштаб) меток и стрелок из подменю настроек
            poseStack.scale(PlayerVisionMod.tagScale, PlayerVisionMod.tagScale, 1.0f);
            poseStack.mulPose(Axis.ZP.rotation((float) (relativeAngle + Math.toRadians(90))));

            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);

            Tesselator tess = Tesselator.getInstance();
            BufferBuilder buf = tess.getBuilder();
            Matrix4f matrix = poseStack.last().pose();

            // Отрисовка векторного треугольника-указателя
            buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            buf.vertex(matrix, 0, -6, 0).color(255, 30, 30, 255).endVertex();   // Наконечник стрелки
            buf.vertex(matrix, -5, 4, 0).color(180, 20, 20, 255).endVertex();   // Левый край
            buf.vertex(matrix, 5, 4, 0).color(180, 20, 20, 255).endVertex();    // Правый край
            tess.end();

            RenderSystem.enableCull();
            poseStack.popPose();

            // Корректируем сдвиг текста с учетом установленного масштаба меток
            int textX = centerX + (int) (Math.cos(relativeAngle) * (radius + (22 * PlayerVisionMod.tagScale)));
            int textY = centerY + (int) (Math.sin(relativeAngle) * (radius + (22 * PlayerVisionMod.tagScale)));

            // Форматируем ник игрока с заглавной буквы
            String textName = playerKey.substring(0, 1).toUpperCase() + playerKey.substring(1);

            // Расчет относительной высоты (Y координата цели минус Y координата вашего персонажа)
            double heightDiff = targetPos.y - mc.player.getY();
            String textDist = String.format("%.1f б.", distance);

            if (PlayerVisionMod.showHeight) {
                textDist += String.format(" (Y: %+.1f)", heightDiff); // Отобразит разницу высоты, например: +5.0 или -2.3
            }

            // Отрисовка текстовых меток
            graphics.drawCenteredString(font, textName, textX, textY - 9, 0xFFFFFF);
            graphics.drawCenteredString(font, textDist, textX, textY + 1, 0x55FF55);
        }
    }
}

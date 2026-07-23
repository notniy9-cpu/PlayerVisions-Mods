package org.Main.playervision;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mod("playervision")
public class PlayerVisionMod {

    public static boolean isPlayersGlow = false;
    public static boolean ignoreSpectators = true;
    public static boolean seeThroughBlocks = true;

    public static int maxDistance = 64;       // Дальность для 3D боксов
    public static int maxTagDistance = 128;   // Отдельная регулируемая дальность для меток (стрелочек)

    public static boolean isTagsEnabled = false;
    public static boolean showHeight = true;
    public static float tagScale = 1.0f;

    public static Map<String, Integer> playerColors = new HashMap<>();
    public static Set<String> targetPlayers = new HashSet<>();       // Игроки для 3D-подсветки
    public static Set<String> radarPlayers = new HashSet<>();        // Игроки специально для меток-стрелок

    public static Map<String, Vec3> FarPositionsCache = new HashMap<>();

    public static final KeyMapping OPEN_MENU_KEY = new KeyMapping(
            "key.playervision.open",
            GLFW.GLFW_KEY_L,
            "key.categories.misc"
    );

    public PlayerVisionMod() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new RenderEventHandler());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().level != null) {
            while (OPEN_MENU_KEY.consumeClick()) {
                Minecraft.getInstance().setScreen(new VisionScreen());
            }
        }
    }

    @Mod.EventBusSubscriber(modid = "playervision", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(OPEN_MENU_KEY);
        }
    }
}

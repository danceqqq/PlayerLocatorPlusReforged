package com.myangel.playerlocatorplus.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.myangel.playerlocatorplus.PlayerLocatorPlus;
import com.myangel.playerlocatorplus.config.ClientValues;
import com.myangel.playerlocatorplus.config.PlayerLocatorConfig;
import com.myangel.playerlocatorplus.network.LocatorDataPacket;
import com.myangel.playerlocatorplus.network.RelativePlayerLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class ClientTracker {
    private static final ResourceLocation EXPERIENCE_BAR_BACKGROUND = new ResourceLocation(PlayerLocatorPlus.MODID, "textures/gui/sprites/hud/empty_bar.png");
    private static final ResourceLocation PLAYER_MARK = new ResourceLocation(PlayerLocatorPlus.MODID, "textures/gui/sprites/hud/player_mark.png");
    private static final ResourceLocation PLAYER_MARK_UP = new ResourceLocation(PlayerLocatorPlus.MODID, "textures/gui/sprites/hud/player_mark_up.png");
    private static final ResourceLocation PLAYER_MARK_DOWN = new ResourceLocation(PlayerLocatorPlus.MODID, "textures/gui/sprites/hud/player_mark_down.png");
    private static final ResourceLocation PLAYER_MARK_WHITE = new ResourceLocation(PlayerLocatorPlus.MODID, "textures/gui/sprites/hud/player_mark_white_outline.png");

    private static final int NAME_PLAQUE_PADDING_X = 4;
    private static final int NAME_PLAQUE_PADDING_Y = 2;
    private static final int NAME_PLAQUE_MARGIN = 2;
    private static final int NAME_PLAQUE_OVERLAP_THRESHOLD = 2;

    private static final float HUD_OFFSET_TOTAL = 14f;
    private static final Animatable HUD_OFFSET = new Animatable(0f);

    private static final Lock POSITION_LOCK = new ReentrantLock();
    private static final Map<UUID, RelativePlayerLocation> RELATIVE_POSITIONS = new HashMap<>();
    private static Vec3 lastUpdatePosition = Vec3.ZERO;

    private ClientTracker() {
    }

    public static void init() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(ClientTracker::registerOverlay);
        modBus.addListener(ClientTracker::onConfigEvent);

        MinecraftForge.EVENT_BUS.addListener(ClientTracker::onClientDisconnect);
        MinecraftForge.EVENT_BUS.register(new OverlayOffsetHandler());

        ClientConfigState.refreshLocal();
    }

    private static void registerOverlay(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.EXPERIENCE_BAR.id(), PlayerLocatorPlus.MODID, (gui, graphics, partialTick, width, height) ->
                render(graphics, partialTick, width, height));
    }

    private static void onConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getSpec() == PlayerLocatorConfig.CLIENT_SPEC) {
            ClientConfigState.refreshLocal();
        }
    }

    private static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        POSITION_LOCK.lock();
        try {
            RELATIVE_POSITIONS.clear();
            lastUpdatePosition = Vec3.ZERO;
        } finally {
            POSITION_LOCK.unlock();
        }
        ClientConfigState.clearServerConfig();
    }

    public static void handleLocations(LocatorDataPacket packet) {
        POSITION_LOCK.lock();
        try {
            if (packet.fullReset()) {
                RELATIVE_POSITIONS.clear();
            } else {
                for (UUID uuid : packet.removeUuids()) {
                    RELATIVE_POSITIONS.remove(uuid);
                }
            }
            for (RelativePlayerLocation update : packet.locationUpdates()) {
                RELATIVE_POSITIONS.put(update.playerUuid(), update);
            }
            Minecraft mc = Minecraft.getInstance();
            lastUpdatePosition = mc.player != null ? mc.player.position() : Vec3.ZERO;
        } finally {
            POSITION_LOCK.unlock();
        }
    }

    public static float getCurrentHudOffset() {
        return HUD_OFFSET.getCurrentValue();
    }

    public static void render(GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        EffectiveClientConfig config = ClientConfigState.effective();
        if (!config.isEnabled()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }

        ClientValues clientValues = config.client();
        if (!clientValues.visible()) {
            return;
        }

        Map<UUID, RelativePlayerLocation> snapshot;
        Vec3 snapshotLastUpdate;
        boolean shouldHideWhenEmpty;
        POSITION_LOCK.lock();
        try {
            snapshot = Map.copyOf(RELATIVE_POSITIONS);
            snapshotLastUpdate = lastUpdatePosition;
            shouldHideWhenEmpty = RELATIVE_POSITIONS.isEmpty() && !clientValues.visibleEmpty();
        } finally {
            POSITION_LOCK.unlock();
        }
        if (shouldHideWhenEmpty) {
            return;
        }

        if (!isHudVisible(mc, player, snapshot, clientValues.visibleEmpty())) {
            return;
        }

        int barWidth = 182;
        int baseX = screenWidth / 2 - 91;
        int baseY = screenHeight - 32 + 3;

        if (player.getJumpRidingScale() <= 0f && mc.gameMode != null && !mc.gameMode.hasExperience()) {
            blit(graphics, EXPERIENCE_BAR_BACKGROUND, baseX, baseY, barWidth, 5, 0xFFFFFFFF);
        }

        List<NamePlaque> namePlaques = new ArrayList<>();
        boolean tabPressed = mc.options.keyPlayerList.isDown();
        List<Map.Entry<UUID, RelativePlayerLocation>> entries = new ArrayList<>(snapshot.entrySet());
        entries.sort(Comparator.comparingDouble(entry -> entry.getValue().distance()));

        for (Map.Entry<UUID, RelativePlayerLocation> entry : entries) {
            UUID uuid = entry.getKey();
            RelativePlayerLocation location = entry.getValue();

            Vec3 directionVec = computeDirectionVector(player, uuid, location, snapshotLastUpdate, partialTick);
            if (directionVec == null) {
                continue;
            }

            double horizontalFov = MathUtils.calculateHorizontalFov(mc.options.fov().get(), screenWidth, screenHeight);
            Vec3 viewVec = player.getViewVector(partialTick);
            double relativeAngle = angleBetween(directionVec.x, directionVec.z, viewVec.x, viewVec.z);
            double progress = (relativeAngle + horizontalFov / 2.0) / horizontalFov;
            if (Double.isNaN(progress) || progress < 0.0 || progress > 1.0) {
                continue;
            }

            int markerX = (int) (baseX + (progress * barWidth)) - 4;
            float opacity = computeMarkerOpacity(config, location.distance());
            int alpha = Mth.clamp((int) (opacity * 255f), 0, 255);
            int color = (alpha << 24) | (location.color() & 0xFFFFFF);

            Optional<PlayerInfo> playerInfo = Optional.ofNullable(mc.getConnection())
                    .flatMap(connection -> Optional.ofNullable(connection.getPlayerInfo(uuid)));
            boolean showHeadIcon = clientValues.alwaysShowHeads() || (clientValues.showHeadsOnTab() && tabPressed);

            if (playerInfo.isPresent() && showHeadIcon) {
                blit(graphics, PLAYER_MARK_WHITE, markerX, baseY - 1, 7, 7, color);
                drawPlayerHead(graphics, playerInfo.get().getSkinLocation(), markerX + 1, baseY, 5);
            } else {
                blit(graphics, PLAYER_MARK, markerX, baseY - 1, 7, 7, color);
            }

            if (clientValues.showHeight()) {
                double normalizedY = directionVec.normalize().y;
                int arrowColor = (alpha << 24) | 0xFFFFFF;
                if (normalizedY > 0.5) {
                    blit(graphics, PLAYER_MARK_UP, markerX + 1, baseY - 5, 5, 4, arrowColor);
                } else if (normalizedY < -0.5) {
                    blit(graphics, PLAYER_MARK_DOWN, markerX + 1, baseY + 7, 5, 4, arrowColor);
                }
            }

            if (playerInfo.isPresent() && clientValues.showNamesOnTab() && tabPressed) {
                namePlaques.add(new NamePlaque(markerX, playerInfo.get().getProfile().getName(), progress));
            }
        }

        HUD_OFFSET.setTargetValue(tabPressed && clientValues.showNamesOnTab() && !namePlaques.isEmpty() ? HUD_OFFSET_TOTAL : 0f);
        HUD_OFFSET.updateValues(mc.getDeltaFrameTime());

        float offset = HUD_OFFSET.getCurrentValue();
        float fadeProgress = offset / HUD_OFFSET_TOTAL;
        if (!namePlaques.isEmpty() && fadeProgress > 0f) {
            renderNamePlaques(graphics, namePlaques, baseY, fadeProgress, offset);
        }
    }

    private static boolean isHudVisible(Minecraft mc, Player player, Map<UUID, RelativePlayerLocation> snapshot, boolean visibleEmpty) {
        if (mc.options.hideGui) {
            return false;
        }
        if (!visibleEmpty && snapshot.isEmpty() && mc.getConnection() != null) {
            boolean anyOther = mc.getConnection().getOnlinePlayers().stream().anyMatch(info -> !info.getProfile().getId().equals(player.getUUID()));
            if (!anyOther) {
                return false;
            }
        }
        if (mc.gameMode != null && mc.gameMode.getPlayerMode() == net.minecraft.world.level.GameType.SPECTATOR && !mc.gui.getSpectatorGui().isMenuActive()) {
            return false;
        }
        return true;
    }

    private static Vec3 computeDirectionVector(Player self, UUID targetUuid, RelativePlayerLocation location, Vec3 lastUpdatePos, float partialTick) {
        Player other = self.level().getPlayerByUUID(targetUuid);
        if (other != null) {
            Vec3 otherPos = other.getPosition(partialTick);
            Vec3 selfPos = self.getPosition(partialTick);
            return otherPos.subtract(selfPos);
        }
        if (location.distance() <= 0f) {
            return new Vec3(location.direction().x(), location.direction().y(), location.direction().z());
        }
        Vec3 projected = lastUpdatePos.add(new Vec3(location.direction().x(), location.direction().y(), location.direction().z()).scale(location.distance()));
        Vec3 selfPos = self.getPosition(partialTick);
        return projected.subtract(selfPos);
    }

    private static float computeMarkerOpacity(EffectiveClientConfig config, float distance) {
        ClientValues client = config.client();
        if (!client.fadeMarkers()) {
            return 1f;
        }
        double fadeStart = client.fadeStart();
        double fadeEnd = client.fadeEnd();
        if (fadeEnd <= fadeStart) {
            return 1f;
        }
        double dist = Mth.clamp(distance, fadeStart, fadeEnd);
        double progress = 1.0 - (dist - fadeStart) / (fadeEnd - fadeStart);
        double minOpacity = client.fadeEndOpacity();
        return (float) (minOpacity + (1.0 - minOpacity) * progress);
    }

    private static void renderNamePlaques(GuiGraphics graphics, List<NamePlaque> plaques, int barY, float fadeProgress, float offset) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        List<NamePlaque> sorted = plaques.stream()
                .sorted(Comparator.comparingDouble(p -> Math.abs(p.progress - 0.5)))
                .toList();

        List<NamePlaque> visible = new ArrayList<>();
        for (NamePlaque plaque : sorted) {
            int textWidth = font.width(plaque.playerName);
            int plaqueWidth = textWidth + NAME_PLAQUE_PADDING_X * 2;
            int xStart = plaque.x - plaqueWidth / 2 + 4;
            int xEnd = xStart + plaqueWidth;
            boolean overlaps = visible.stream().anyMatch(other ->
                    other.rangeStart() - NAME_PLAQUE_OVERLAP_THRESHOLD <= xEnd &&
                            other.rangeEnd() + NAME_PLAQUE_OVERLAP_THRESHOLD >= xStart);
            if (!overlaps) {
                visible.add(plaque.withRange(xStart, xEnd));
            }
        }

        for (NamePlaque plaque : visible) {
            int textWidth = font.width(plaque.playerName);
            int plaqueWidth = textWidth + NAME_PLAQUE_PADDING_X * 2;
            int plaqueHeight = font.lineHeight + NAME_PLAQUE_PADDING_Y * 2;
            int x = plaque.x - plaqueWidth / 2 + 4;
            int y = barY - plaqueHeight - NAME_PLAQUE_MARGIN;

            int bgAlpha = (int) (128 * fadeProgress);
            int textAlpha = (int) (255 * fadeProgress);

            if (bgAlpha > 0) {
                graphics.fill(x, y, x + plaqueWidth, y + plaqueHeight, bgAlpha << 24);
            }
            if (textAlpha > 3) {
                graphics.drawString(font, plaque.playerName, x + NAME_PLAQUE_PADDING_X, y + NAME_PLAQUE_PADDING_Y, (textAlpha << 24) | 0xFFFFFF, false);
            }
        }
    }

    private static double angleBetween(double x1, double z1, double x2, double z2) {
        double angle1 = Math.atan2(z1, x1);
        double angle2 = Math.atan2(z2, x2);
        return Math.toDegrees(angle1 - angle2);
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int color) {
        float a = (color >>> 24 & 255) / 255f;
        float r = (color >>> 16 & 255) / 255f;
        float g = (color >>> 8 & 255) / 255f;
        float b = (color & 255) / 255f;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(r, g, b, a);
        graphics.blit(texture, x, y, 0, 0, width, height, width, height);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private static void drawPlayerHead(GuiGraphics graphics, ResourceLocation texture, int x, int y, int size) {
        RenderSystem.enableBlend();
        graphics.blit(texture, x, y, size, size, 8, 8, 8, 8, 64, 64);
        graphics.blit(texture, x, y, size, size, 40, 8, 8, 8, 64, 64);
        RenderSystem.disableBlend();
    }

    private record NamePlaque(int x, String playerName, double progress, int rangeStart, int rangeEnd) {
        NamePlaque(int x, String playerName, double progress) {
            this(x, playerName, progress, 0, 0);
        }

        NamePlaque withRange(int start, int end) {
            return new NamePlaque(x, playerName, progress, start, end);
        }
    }

    @SuppressWarnings("unused")
    private static final class OverlayOffsetHandler {
        @SubscribeEvent
        public void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
            float offset = ClientTracker.getCurrentHudOffset();
            if (offset <= 0f) {
                return;
            }
            ResourceLocation id = event.getOverlay().id();
            if (offsetTargets(id)) {
                event.getGuiGraphics().pose().pushPose();
                event.getGuiGraphics().pose().translate(0.0f, -offset, 0.0f);
            }
        }

        @SubscribeEvent
        public void onOverlayPost(RenderGuiOverlayEvent.Post event) {
            float offset = ClientTracker.getCurrentHudOffset();
            if (offset <= 0f) {
                return;
            }
            ResourceLocation id = event.getOverlay().id();
            if (offsetTargets(id)) {
                event.getGuiGraphics().pose().popPose();
            }
        }

        private boolean offsetTargets(ResourceLocation id) {
            return id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id())
                    || id.equals(VanillaGuiOverlay.FOOD_LEVEL.id())
                    || id.equals(VanillaGuiOverlay.AIR_LEVEL.id())
                    || id.equals(VanillaGuiOverlay.MOUNT_HEALTH.id());
        }
    }
}

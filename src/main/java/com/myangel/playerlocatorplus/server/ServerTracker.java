package com.myangel.playerlocatorplus.server;

import com.myangel.playerlocatorplus.config.PlayerLocatorConfig;
import com.myangel.playerlocatorplus.config.ServerValues;
import com.myangel.playerlocatorplus.config.ColorMode;
import com.myangel.playerlocatorplus.network.LocatorDataPacket;
import com.myangel.playerlocatorplus.network.NetworkHandler;
import com.myangel.playerlocatorplus.network.RelativePlayerLocation;
import com.myangel.playerlocatorplus.network.ServerConfigPacket;
import com.myangel.playerlocatorplus.util.ColorUtils;
import com.myangel.playerlocatorplus.util.PlayerData;
import com.myangel.playerlocatorplus.util.PlayerDataState;
import com.myangel.playerlocatorplus.util.PlayerLocatorTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class ServerTracker {
    private static final double TWO_PI = Math.PI * 2.0;
    private static final double HEIGHT_ARROW_THRESHOLD = 4.0;

    private static int tickCounter = 0;
    private static boolean configDirty = true;
    private static Map<UUID, StoredPlayerPosition> previousPositions = Map.of();

    private ServerTracker() {
    }

    public static void markConfigDirty() {
        configDirty = true;
    }

    public static void reset() {
        previousPositions = Map.of();
        tickCounter = 0;
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerValues config = PlayerLocatorConfig.getServerValues();
        if (config.sendServerConfig()) {
            NetworkHandler.sendTo(player, new ServerConfigPacket(config));
        }
        fullResend(player);
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }

        ServerValues config = PlayerLocatorConfig.getServerValues();
        if (!config.enabled()) {
            if (configDirty && config.sendServerConfig()) {
                broadcastConfig(players, config);
                configDirty = false;
            }
            previousPositions = Map.of();
            return;
        }

        if (configDirty && config.sendServerConfig()) {
            broadcastConfig(players, config);
            configDirty = false;
        }

        int interval = Math.max(0, config.ticksBetweenUpdates());
        if (tickCounter++ < interval) {
            return;
        }
        tickCounter = 0;

        Map<UUID, StoredPlayerPosition> currentPositions = collectPositions(server, config);

        for (ServerPlayer observer : players) {
            StoredPlayerPosition previousObserver = previousPositions.get(observer.getUUID());
            StoredPlayerPosition currentObserver = currentPositions.get(observer.getUUID());

            if (currentObserver == null) {
                continue;
            }

            List<RelativePlayerLocation> updates = new ArrayList<>();
            Set<UUID> removals = new HashSet<>();

            for (Map.Entry<UUID, StoredPlayerPosition> entry : previousPositions.entrySet()) {
                UUID uuid = entry.getKey();
                if (uuid.equals(observer.getUUID())) {
                    continue;
                }
                StoredPlayerPosition previousTarget = entry.getValue();
                StoredPlayerPosition currentTarget = currentPositions.get(uuid);

                if (currentTarget == null || previousTarget.level != observer.level()) {
                    removals.add(uuid);
                    continue;
                }

                if (previousObserver != null && previousObserver.level != observer.level()) {
                    removals.add(uuid);
                    continue;
                }

                if (config.maxDistance() > 0 && currentTarget.level == observer.level()) {
                    double previousDistance = previousObserver != null ? previousObserver.position.distanceTo(previousTarget.position) : 0.0;
                    double currentDistance = currentObserver.position.distanceTo(currentTarget.position);
                    if (currentDistance > config.maxDistance() && previousDistance <= config.maxDistance()) {
                        removals.add(uuid);
                    }
                }
            }

            for (Map.Entry<UUID, StoredPlayerPosition> entry : currentPositions.entrySet()) {
                UUID uuid = entry.getKey();
                if (uuid.equals(observer.getUUID())) {
                    continue;
                }
                StoredPlayerPosition currentTarget = entry.getValue();
                if (currentTarget.level != observer.level()) {
                    continue;
                }
                double distance = currentObserver.position.distanceTo(currentTarget.position);
                if (config.maxDistance() > 0 && distance > config.maxDistance()) {
                    continue;
                }

                StoredPlayerPosition previousTarget = previousPositions.get(uuid);
                RelativePlayerLocation currentLocation = createRelativeLocation(uuid, currentObserver, currentTarget, config);
                RelativePlayerLocation previousLocation = null;
                if (previousObserver != null && previousTarget != null && previousTarget.level == previousObserver.level) {
                    previousLocation = createRelativeLocation(uuid, previousObserver, previousTarget, config);
                }
                if (!currentLocation.equals(previousLocation)) {
                    updates.add(currentLocation);
                }
            }

            boolean fullReset = previousObserver != null && previousObserver.level != observer.level();
            if (!updates.isEmpty() || !removals.isEmpty() || fullReset) {
                NetworkHandler.sendTo(observer, new LocatorDataPacket(updates, new ArrayList<>(removals), fullReset));
            }
        }

        previousPositions = currentPositions;
    }

    public static void fullResend(ServerPlayer player) {
        ServerValues config = PlayerLocatorConfig.getServerValues();
        List<RelativePlayerLocation> positions = new ArrayList<>();
        StoredPlayerPosition self = new StoredPlayerPosition(player, config);
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other == player) {
                continue;
            }
            StoredPlayerPosition target = new StoredPlayerPosition(other, config);
            if (self.level != target.level) {
                continue;
            }
            if (config.maxDistance() > 0 && self.position.distanceTo(target.position) > config.maxDistance()) {
                continue;
            }
            positions.add(createRelativeLocation(other.getUUID(), self, target, config));
        }
        NetworkHandler.sendTo(player, new LocatorDataPacket(config.enabled() ? positions : List.of(), List.of(), true));
    }

    public static void fullResend(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            fullResend(player);
        }
    }

    public static void sendFakePlayers(ServerPlayer player) {
        Random random = new Random();
        List<RelativePlayerLocation> locations = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Vector3f direction = new Vector3f(random.nextFloat(), random.nextFloat() * 0.75f, random.nextFloat());
            locations.add(new RelativePlayerLocation(UUID.randomUUID(), direction, random.nextFloat() * 750f, ColorUtils.uuidToColor(UUID.randomUUID())));
        }
        NetworkHandler.sendTo(player, new LocatorDataPacket(locations, List.of(), false));
    }

    private static void broadcastConfig(List<ServerPlayer> players, ServerValues values) {
        ServerConfigPacket packet = new ServerConfigPacket(values);
        for (ServerPlayer player : players) {
            NetworkHandler.sendTo(player, packet);
        }
    }

    private static Map<UUID, StoredPlayerPosition> collectPositions(MinecraftServer server, ServerValues config) {
        Map<UUID, StoredPlayerPosition> map = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (shouldHide(player, config)) {
                continue;
            }
            map.put(player.getUUID(), new StoredPlayerPosition(player, config));
        }
        return map;
    }

    private static boolean shouldHide(ServerPlayer player, ServerValues config) {
        if (config.sneakingHides() && player.isShiftKeyDown()) {
            return true;
        }
        if (config.invisibilityHides() && player.hasEffect(MobEffects.INVISIBILITY)) {
            return true;
        }
        if (player.isSpectator()) {
            return true;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) {
            return false;
        }
        if (config.pumpkinHides() && head.is(Items.CARVED_PUMPKIN)) {
            return true;
        }
        if (config.mobHeadsHide() && head.is(PlayerLocatorTags.HIDING_EQUIPMENT)) {
            return true;
        }
        if (config.mobHeadsHide() && head.getItem() instanceof BlockItem block && block.getBlock() instanceof net.minecraft.world.level.block.SkullBlock) {
            return true;
        }
        return false;
    }

    private static RelativePlayerLocation createRelativeLocation(UUID uuid, StoredPlayerPosition self, StoredPlayerPosition other, ServerValues config) {
        Vec3 delta = other.position.subtract(self.position).normalize();
        float precision = (float) config.directionPrecision();
        delta = new Vec3(
                Math.round(delta.x * precision) / precision,
                Math.round(delta.y * precision) / precision,
                Math.round(delta.z * precision) / precision
        );
        float distance = 0f;
        if (config.sendDistance()) {
            double rawDistance = self.position.distanceTo(other.position);
            distance = (float) (rawDistance < 200 ? rawDistance : Math.round(rawDistance / 50.0) * 50.0);
        }
        Vector3f direction = new Vector3f((float) delta.x, (float) delta.y, (float) delta.z);
        return new RelativePlayerLocation(uuid, direction, distance, other.color);
    }

    private record StoredPlayerPosition(Vec3 position, ServerLevel level, int color) {
        StoredPlayerPosition(ServerPlayer player, ServerValues config) {
            this(player.position(), player.serverLevel(), resolveColor(player, config));
        }
    }

    private static int resolveColor(ServerPlayer player, ServerValues config) {
        return switch (config.colorMode()) {
            case UUID -> ColorUtils.uuidToColor(player.getUUID());
            case TEAM_COLOR -> {
                if (player.getTeam() != null) {
                    Integer teamColor = player.getTeam().getColor().getColor();
                    if (teamColor != null) {
                        yield teamColor;
                    }
                }
                yield ColorUtils.uuidToColor(player.getUUID());
            }
            case CONSTANT -> config.constantColor();
            case CUSTOM -> {
                PlayerData data = PlayerDataState.get(player.server).get(player.getUUID());
                yield data.customColor();
            }
        };
    }
}

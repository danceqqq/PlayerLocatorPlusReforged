package com.myangel.playerlocatorplus.network;

import com.myangel.playerlocatorplus.client.ClientConfigState;
import com.myangel.playerlocatorplus.config.ClientValues;
import com.myangel.playerlocatorplus.config.ColorMode;
import com.myangel.playerlocatorplus.config.ServerValues;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ServerConfigPacket(ServerValues values) {
    public static void encode(ServerConfigPacket packet, FriendlyByteBuf buf) {
        ServerValues values = packet.values;
        buf.writeBoolean(values.enabled());
        buf.writeBoolean(values.sendServerConfig());
        buf.writeBoolean(values.sendDistance());
        buf.writeVarInt(values.maxDistance());
        buf.writeDouble(values.directionPrecision());
        buf.writeVarInt(values.ticksBetweenUpdates());
        buf.writeBoolean(values.sneakingHides());
        buf.writeBoolean(values.pumpkinHides());
        buf.writeBoolean(values.mobHeadsHide());
        buf.writeBoolean(values.invisibilityHides());
        buf.writeEnum(values.colorMode());
        buf.writeVarInt(values.constantColor());
        writeClientValues(buf, values.clientOverrides());
    }

    public static ServerConfigPacket decode(FriendlyByteBuf buf) {
        boolean enabled = buf.readBoolean();
        boolean sendServerConfig = buf.readBoolean();
        boolean sendDistance = buf.readBoolean();
        int maxDistance = buf.readVarInt();
        double directionPrecision = buf.readDouble();
        int ticksBetweenUpdates = buf.readVarInt();
        boolean sneakingHides = buf.readBoolean();
        boolean pumpkinHides = buf.readBoolean();
        boolean mobHeadsHide = buf.readBoolean();
        boolean invisibilityHides = buf.readBoolean();
        ColorMode colorMode = buf.readEnum(ColorMode.class);
        int constantColor = buf.readVarInt();
        ClientValues overrides = readClientValues(buf);
        ServerValues values = new ServerValues(
                enabled,
                sendServerConfig,
                sendDistance,
                maxDistance,
                directionPrecision,
                ticksBetweenUpdates,
                sneakingHides,
                pumpkinHides,
                mobHeadsHide,
                invisibilityHides,
                colorMode,
                constantColor,
                overrides
        );
        return new ServerConfigPacket(values);
    }

    public static void handle(ServerConfigPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ClientConfigState.applyServerConfig(packet.values()));
        ctx.setPacketHandled(true);
    }

    private static void writeClientValues(FriendlyByteBuf buf, ClientValues values) {
        buf.writeBoolean(values.visible());
        buf.writeBoolean(values.visibleEmpty());
        buf.writeBoolean(values.acceptServerConfig());
        buf.writeBoolean(values.fadeMarkers());
        buf.writeDouble(values.fadeStart());
        buf.writeDouble(values.fadeEnd());
        buf.writeDouble(values.fadeEndOpacity());
        buf.writeBoolean(values.showHeight());
        buf.writeBoolean(values.alwaysShowHeads());
        buf.writeBoolean(values.showHeadsOnTab());
        buf.writeBoolean(values.showNamesOnTab());
    }

    private static ClientValues readClientValues(FriendlyByteBuf buf) {
        boolean visible = buf.readBoolean();
        boolean visibleEmpty = buf.readBoolean();
        boolean acceptServerConfig = buf.readBoolean();
        boolean fadeMarkers = buf.readBoolean();
        double fadeStart = buf.readDouble();
        double fadeEnd = buf.readDouble();
        double fadeEndOpacity = buf.readDouble();
        boolean showHeight = buf.readBoolean();
        boolean alwaysShowHeads = buf.readBoolean();
        boolean showHeadsOnTab = buf.readBoolean();
        boolean showNamesOnTab = buf.readBoolean();
        return new ClientValues(visible, visibleEmpty, acceptServerConfig, fadeMarkers, fadeStart, fadeEnd, fadeEndOpacity, showHeight, alwaysShowHeads, showHeadsOnTab, showNamesOnTab);
    }
}

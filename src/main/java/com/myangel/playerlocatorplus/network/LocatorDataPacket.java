package com.myangel.playerlocatorplus.network;

import com.myangel.playerlocatorplus.client.ClientTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record LocatorDataPacket(List<RelativePlayerLocation> locationUpdates, List<UUID> removeUuids, boolean fullReset) {
    public static void encode(LocatorDataPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.locationUpdates.size());
        for (RelativePlayerLocation update : packet.locationUpdates) {
            update.write(buf);
        }
        buf.writeVarInt(packet.removeUuids.size());
        for (UUID uuid : packet.removeUuids) {
            buf.writeUUID(uuid);
        }
        buf.writeBoolean(packet.fullReset);
    }

    public static LocatorDataPacket decode(FriendlyByteBuf buf) {
        int updateCount = buf.readVarInt();
        List<RelativePlayerLocation> updates = new ArrayList<>(updateCount);
        for (int i = 0; i < updateCount; i++) {
            updates.add(RelativePlayerLocation.read(buf));
        }
        int removeCount = buf.readVarInt();
        List<UUID> remove = new ArrayList<>(removeCount);
        for (int i = 0; i < removeCount; i++) {
            remove.add(buf.readUUID());
        }
        boolean fullReset = buf.readBoolean();
        return new LocatorDataPacket(updates, remove, fullReset);
    }

    public static void handle(LocatorDataPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ClientTracker.handleLocations(packet));
        ctx.setPacketHandled(true);
    }
}

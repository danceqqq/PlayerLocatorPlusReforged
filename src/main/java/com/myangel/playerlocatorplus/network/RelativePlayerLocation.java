package com.myangel.playerlocatorplus.network;

import net.minecraft.network.FriendlyByteBuf;
import org.joml.Vector3f;

import java.util.UUID;

public record RelativePlayerLocation(UUID playerUuid, Vector3f direction, float distance, int color) {
    public static RelativePlayerLocation read(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        Vector3f dir = buf.readVector3f();
        float distance = buf.readFloat();
        int color = buf.readInt();
        return new RelativePlayerLocation(uuid, dir, distance, color);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(playerUuid);
        buf.writeVector3f(direction);
        buf.writeFloat(distance);
        buf.writeInt(color);
    }
}

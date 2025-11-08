package com.myangel.playerlocatorplus.network;

import com.myangel.playerlocatorplus.PlayerLocatorPlus;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.server.level.ServerPlayer;

public final class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(PlayerLocatorPlus.rl("main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static int nextPacketId = 0;

    private NetworkHandler() {
    }

    public static void init() {
        CHANNEL.messageBuilder(ServerConfigPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ServerConfigPacket::encode)
                .decoder(ServerConfigPacket::decode)
                .consumerMainThread(ServerConfigPacket::handle)
                .add();

        CHANNEL.messageBuilder(LocatorDataPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LocatorDataPacket::encode)
                .decoder(LocatorDataPacket::decode)
                .consumerMainThread(LocatorDataPacket::handle)
                .add();
    }

    private static int nextId() {
        return nextPacketId++;
    }

    public static void sendTo(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}

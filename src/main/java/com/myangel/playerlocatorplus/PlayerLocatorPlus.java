package com.myangel.playerlocatorplus;

import com.myangel.playerlocatorplus.client.ClientTracker;
import com.myangel.playerlocatorplus.command.PLPCommand;
import com.myangel.playerlocatorplus.config.PlayerLocatorConfig;
import com.myangel.playerlocatorplus.network.NetworkHandler;
import com.myangel.playerlocatorplus.server.ServerTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(PlayerLocatorPlus.MODID)
public class PlayerLocatorPlus {
    public static final String MODID = "playerlocatorplus";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }

    public PlayerLocatorPlus() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, PlayerLocatorConfig.SERVER_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, PlayerLocatorConfig.CLIENT_SPEC);

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        PlayerLocatorConfig.register(modBus);
        NetworkHandler.init();

        MinecraftForge.EVENT_BUS.addListener(ServerTracker::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(ServerTracker::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);

        if (FMLEnvironment.dist.isClient()) {
            ClientTracker.init();
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        PLPCommand.register(event);
    }

    private void onServerStopped(ServerStoppedEvent event) {
        ServerTracker.reset();
    }
}

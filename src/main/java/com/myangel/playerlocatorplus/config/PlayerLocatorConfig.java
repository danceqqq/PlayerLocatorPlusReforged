package com.myangel.playerlocatorplus.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.myangel.playerlocatorplus.PlayerLocatorPlus;
import com.myangel.playerlocatorplus.server.ServerTracker;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.config.ModConfig;

public final class PlayerLocatorConfig {
    public static final Server SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        var serverPair = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER_SPEC = serverPair.getRight();
        SERVER = serverPair.getLeft();

        var clientPair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = clientPair.getRight();
        CLIENT = clientPair.getLeft();
    }

    private PlayerLocatorConfig() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(PlayerLocatorConfig::onConfigLoad);
        modBus.addListener(PlayerLocatorConfig::onConfigReload);
    }

    private static void onConfigLoad(ModConfigEvent.Loading event) {
        apply(event.getConfig());
    }

    private static void onConfigReload(ModConfigEvent.Reloading event) {
        apply(event.getConfig());
    }

    private static void apply(ModConfig config) {
        if (config.getSpec() == SERVER_SPEC) {
            SERVER.bake(config.getConfigData());
        } else if (config.getSpec() == CLIENT_SPEC) {
            CLIENT.bake(config.getConfigData());
        }
    }

    public static ServerValues getServerValues() {
        return SERVER.getValues();
    }

    public static ClientValues getClientValues() {
        return CLIENT.getValues();
    }

    public static final class Server {
        private final ForgeConfigSpec.BooleanValue enabled;
        private final ForgeConfigSpec.BooleanValue sendServerConfig;
        private final ForgeConfigSpec.BooleanValue sendDistance;
        private final ForgeConfigSpec.IntValue maxDistance;
        private final ForgeConfigSpec.DoubleValue directionPrecision;
        private final ForgeConfigSpec.IntValue ticksBetweenUpdates;
        private final ForgeConfigSpec.BooleanValue sneakingHides;
        private final ForgeConfigSpec.BooleanValue pumpkinHides;
        private final ForgeConfigSpec.BooleanValue mobHeadsHide;
        private final ForgeConfigSpec.BooleanValue invisibilityHides;
        private final ForgeConfigSpec.EnumValue<ColorMode> colorMode;
        private final ForgeConfigSpec.IntValue constantColor;

        private final ForgeConfigSpec.BooleanValue overrideVisible;
        private final ForgeConfigSpec.BooleanValue overrideVisibleEmpty;
        private final ForgeConfigSpec.BooleanValue overrideFadeMarkers;
        private final ForgeConfigSpec.DoubleValue overrideFadeStart;
        private final ForgeConfigSpec.DoubleValue overrideFadeEnd;
        private final ForgeConfigSpec.DoubleValue overrideFadeEndOpacity;
        private final ForgeConfigSpec.BooleanValue overrideShowHeight;
        private final ForgeConfigSpec.BooleanValue overrideAlwaysShowHeads;
        private final ForgeConfigSpec.BooleanValue overrideShowHeadsOnTab;
        private final ForgeConfigSpec.BooleanValue overrideShowNamesOnTab;

        private volatile ServerValues values = ServerValues.defaults();

        public Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Server-side configuration for Player Locator Plus").push("server");

            enabled = builder.comment("Is the mod enabled server-side?")
                    .define("enabled", true);
            sendServerConfig = builder.comment("Send the server config to clients that opt in?")
                    .define("sendServerConfig", true);
            sendDistance = builder.comment("Send distance information together with direction?")
                    .define("sendDistance", true);
            maxDistance = builder.comment("Maximum distance at which markers are visible (0 = unlimited).")
                    .defineInRange("maxDistance", 0, 0, 1_000_000);
            directionPrecision = builder.comment("Precision factor for far-away players. Higher = smoother but easier to triangulate.")
                    .defineInRange("directionPrecision", 300.0, 2.0, 2048.0);
            ticksBetweenUpdates = builder.comment("Ticks between network updates. Lower = more frequent updates.")
                    .defineInRange("ticksBetweenUpdates", 5, 0, 200);
            sneakingHides = builder.comment("Should sneaking hide players from the locator?")
                    .define("sneakingHides", true);
            pumpkinHides = builder.comment("Should wearing a carved pumpkin hide players?")
                    .define("pumpkinHides", true);
            mobHeadsHide = builder.comment("Should wearing a mob/player head hide players?")
                    .define("mobHeadsHide", true);
            invisibilityHides = builder.comment("Should invisibility hide players?")
                    .define("invisibilityHides", true);
            colorMode = builder.comment("How to determine marker colors.")
                    .defineEnum("colorMode", ColorMode.UUID);
            constantColor = builder.comment("Hex color (0xRRGGBB) used when color mode is CONSTANT.")
                    .defineInRange("constantColor", 0xFFFFFF, 0x000000, 0xFFFFFF);

            builder.comment("Client style overrides applied when sendServerConfig is enabled.").push("clientOverrides");
            overrideVisible = builder.define("visible", true);
            overrideVisibleEmpty = builder.define("visibleEmpty", false);
            overrideFadeMarkers = builder.define("fadeMarkers", true);
            overrideFadeStart = builder.defineInRange("fadeStart", 100.0, 0.0, 1_000_000.0);
            overrideFadeEnd = builder.defineInRange("fadeEnd", 1000.0, 0.0, 1_000_000.0);
            overrideFadeEndOpacity = builder.defineInRange("fadeEndOpacity", 0.3, 0.0, 1.0);
            overrideShowHeight = builder.define("showHeight", true);
            overrideAlwaysShowHeads = builder.define("alwaysShowHeads", false);
            overrideShowHeadsOnTab = builder.define("showHeadsOnTab", true);
            overrideShowNamesOnTab = builder.define("showNamesOnTab", true);
            builder.pop();

            builder.pop();
        }

        private void bake(CommentedConfig configData) {
            ClientValues overrideValues = new ClientValues(
                    overrideVisible.get(),
                    overrideVisibleEmpty.get(),
                    true,
                    overrideFadeMarkers.get(),
                    overrideFadeStart.get(),
                    overrideFadeEnd.get(),
                    overrideFadeEndOpacity.get(),
                    overrideShowHeight.get(),
                    overrideAlwaysShowHeads.get(),
                    overrideShowHeadsOnTab.get(),
                    overrideShowNamesOnTab.get()
            );

            values = new ServerValues(
                    enabled.get(),
                    sendServerConfig.get(),
                    sendDistance.get(),
                    maxDistance.get(),
                    directionPrecision.get(),
                    ticksBetweenUpdates.get(),
                    sneakingHides.get(),
                    pumpkinHides.get(),
                    mobHeadsHide.get(),
                    invisibilityHides.get(),
                    colorMode.get(),
                    constantColor.get(),
                    overrideValues
            );
            ServerTracker.markConfigDirty();
            PlayerLocatorPlus.LOGGER.debug("Baked server config: {}", values);
        }

        public ServerValues getValues() {
            return values;
        }
    }

    public static final class Client {
        private final ForgeConfigSpec.BooleanValue visible;
        private final ForgeConfigSpec.BooleanValue visibleEmpty;
        private final ForgeConfigSpec.BooleanValue acceptServerConfig;
        private final ForgeConfigSpec.BooleanValue fadeMarkers;
        private final ForgeConfigSpec.DoubleValue fadeStart;
        private final ForgeConfigSpec.DoubleValue fadeEnd;
        private final ForgeConfigSpec.DoubleValue fadeEndOpacity;
        private final ForgeConfigSpec.BooleanValue showHeight;
        private final ForgeConfigSpec.BooleanValue alwaysShowHeads;
        private final ForgeConfigSpec.BooleanValue showHeadsOnTab;
        private final ForgeConfigSpec.BooleanValue showNamesOnTab;

        private volatile ClientValues values = ClientValues.defaults();

        public Client(ForgeConfigSpec.Builder builder) {
            builder.comment("Client-side configuration for Player Locator Plus").push("client");

            visible = builder.comment("Show the player locator overlay?")
                    .define("visible", true);
            visibleEmpty = builder.comment("Show the locator even when no markers are present?")
                    .define("visibleEmpty", false);
            acceptServerConfig = builder.comment("Allow the server to override client settings?")
                    .define("acceptServerConfig", true);
            fadeMarkers = builder.comment("Fade markers of far away players?")
                    .define("fadeMarkers", true);
            fadeStart = builder.comment("Distance at which markers start fading.")
                    .defineInRange("fadeStart", 100.0, 0.0, 1_000_000.0);
            fadeEnd = builder.comment("Distance at which markers reach final opacity.")
                    .defineInRange("fadeEnd", 1000.0, 0.0, 1_000_000.0);
            fadeEndOpacity = builder.comment("Opacity at fade end (0-1).")
                    .defineInRange("fadeEndOpacity", 0.3, 0.0, 1.0);
            showHeight = builder.comment("Show arrows indicating large height differences?")
                    .define("showHeight", true);
            alwaysShowHeads = builder.comment("Always show player heads on markers? (overrides TAB behaviour)")
                    .define("alwaysShowHeads", false);
            showHeadsOnTab = builder.comment("Show player heads when the Tab list key is pressed?")
                    .define("showHeadsOnTab", true);
            showNamesOnTab = builder.comment("Show player name plaques when the Tab list key is pressed?")
                    .define("showNamesOnTab", true);

            builder.pop();
        }

        private void bake(CommentedConfig configData) {
            values = new ClientValues(
                    visible.get(),
                    visibleEmpty.get(),
                    acceptServerConfig.get(),
                    fadeMarkers.get(),
                    fadeStart.get(),
                    fadeEnd.get(),
                    fadeEndOpacity.get(),
                    showHeight.get(),
                    alwaysShowHeads.get(),
                    showHeadsOnTab.get(),
                    showNamesOnTab.get()
            );
            PlayerLocatorPlus.LOGGER.debug("Baked client config: {}", values);
        }

        public ClientValues getValues() {
            return values;
        }
    }
}

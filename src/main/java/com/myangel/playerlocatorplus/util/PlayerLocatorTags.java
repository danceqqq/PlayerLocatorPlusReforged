package com.myangel.playerlocatorplus.util;

import com.myangel.playerlocatorplus.PlayerLocatorPlus;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class PlayerLocatorTags {
    public static final TagKey<Item> HIDING_EQUIPMENT = TagKey.create(Registries.ITEM, new ResourceLocation(PlayerLocatorPlus.MODID, "hiding_equipment"));

    private PlayerLocatorTags() {
    }
}

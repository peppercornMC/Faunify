package com.pepper.faunify.event;

import com.pepper.faunify.registry.FaunifyItems;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreativeTabHandler {

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.getEntries().putAfter(
                Items.TADPOLE_BUCKET.getDefaultInstance(),
                FaunifyItems.LEAF_SHEEP_BUCKET.get().getDefaultInstance(),
                CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
        }
    }
}
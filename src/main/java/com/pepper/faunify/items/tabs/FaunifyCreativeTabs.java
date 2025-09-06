package com.pepper.faunify.items.tabs;

import com.pepper.faunify.Faunify;
import com.pepper.faunify.registry.FaunifyItems;
import com.pepper.faunify.registry.FaunifyBlocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class FaunifyCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Faunify.MODID);

    public static final RegistryObject<CreativeModeTab> FAUNIFY_TAB = CREATIVE_MODE_TABS.register("faunify_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(FaunifyItems.WEASEL_SPAWN_EGG.get()))
                    .title(Component.translatable("faunify.faunify_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(FaunifyItems.WEASEL_SPAWN_EGG.get());
                        pOutput.accept(FaunifyItems.FENNEC_SPAWN_EGG.get());
                        pOutput.accept(FaunifyItems.CHINCHILLA_SPAWN_EGG.get());
                        pOutput.accept(FaunifyItems.HEDGEHOG_SPAWN_EGG.get());
                        pOutput.accept(FaunifyItems.RINGTAIL_SPAWN_EGG.get());
                        pOutput.accept(FaunifyItems.OPOSSUM_SPAWN_EGG.get());
                        pOutput.accept(FaunifyItems.MOUSE_SPAWN_EGG.get());
                        pOutput.accept(FaunifyItems.SILKMOTH_SPAWN_EGG.get());
                        pOutput.accept(FaunifyItems.LEAFSHEEP_SPAWN_EGG.get());
                        pOutput.accept(FaunifyBlocks.DUST_BLOCK.get());
                        pOutput.accept(FaunifyItems.DUST_POWDER.get());
                        pOutput.accept(FaunifyItems.BLOWPIPE.get());
                        pOutput.accept(FaunifyItems.QUILL.get());
                        pOutput.accept(FaunifyItems.FUR.get());
                    })
                    .build());


    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
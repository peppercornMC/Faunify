package com.pepper.faunify;

import com.pepper.faunify.entity.client.model.QuillModel;
import com.pepper.faunify.entity.client.render.ChinchillaRenderer;
import com.pepper.faunify.entity.client.render.FennecRenderer;
import com.pepper.faunify.entity.client.render.HedgehogRenderer;
import com.pepper.faunify.entity.client.render.LeafSheepRenderer;
import com.pepper.faunify.entity.client.render.MouseRenderer;
import com.pepper.faunify.entity.client.render.OpossumRenderer;
import com.pepper.faunify.entity.client.render.QuillRenderer;
import com.pepper.faunify.entity.client.render.RingtailCatRenderer;
import com.pepper.faunify.entity.client.render.SilkMothRenderer;
import com.pepper.faunify.entity.client.render.WeaselRenderer;
import com.pepper.faunify.registry.FaunifyEntities;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Faunify.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FaunifyEntities.WEASEL.get(), WeaselRenderer::new);
        event.registerEntityRenderer(FaunifyEntities.FENNEC.get(), FennecRenderer::new);
        event.registerEntityRenderer(FaunifyEntities.CHINCHILLA.get(), ChinchillaRenderer::new);
        event.registerEntityRenderer(FaunifyEntities.HEDGEHOG.get(), HedgehogRenderer::new);
        event.registerEntityRenderer(FaunifyEntities.RINGTAIL.get(), RingtailCatRenderer::new);
        event.registerEntityRenderer(FaunifyEntities.OPOSSUM.get(), OpossumRenderer::new);
        event.registerEntityRenderer(FaunifyEntities.MOUSE.get(), MouseRenderer::new);
        event.registerEntityRenderer(FaunifyEntities.SILKMOTH.get(), SilkMothRenderer::new);
        event.registerEntityRenderer(FaunifyEntities.LEAFSHEEP.get(), LeafSheepRenderer::new);
        event.registerEntityRenderer(FaunifyEntities.DUST_POWDER_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(FaunifyEntities.QUILL_PROJECTILE.get(), QuillRenderer::new);
    }
    
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(QuillModel.LAYER_LOCATION, QuillModel::createBodyLayer);
    }
}

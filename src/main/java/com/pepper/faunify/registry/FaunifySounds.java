package com.pepper.faunify.registry;

import com.pepper.faunify.Faunify;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FaunifySounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Faunify.MODID);

    public static final RegistryObject<SoundEvent> WEASEL_IDLE_1 = registerSoundEvents("weasel_idle_1");
    public static final RegistryObject<SoundEvent> WEASEL_IDLE_2 = registerSoundEvents("weasel_idle_2");
    public static final RegistryObject<SoundEvent> WEASEL_IDLE_3 = registerSoundEvents("weasel_idle_3");
    public static final RegistryObject<SoundEvent> WEASEL_HURT = registerSoundEvents("weasel_hurt");
    public static final RegistryObject<SoundEvent> WEASEL_BITE = registerSoundEvents("weasel_bite");
    public static final RegistryObject<SoundEvent> FENNEC_IDLE_1 = registerSoundEvents("fennec_idle_1");
    public static final RegistryObject<SoundEvent> FENNEC_IDLE_2 = registerSoundEvents("fennec_idle_2");
    public static final RegistryObject<SoundEvent> FENNEC_HURT = registerSoundEvents("fennec_hurt");
    public static final RegistryObject<SoundEvent> FENNEC_ALERT = registerSoundEvents("fennec_alert");
    public static final RegistryObject<SoundEvent> CHINCHILLA_IDLE_1 = registerSoundEvents("chinchilla_idle_1");
    public static final RegistryObject<SoundEvent> CHINCHILLA_IDLE_2 = registerSoundEvents("chinchilla_idle_2");
    public static final RegistryObject<SoundEvent> CHINCHILLA_IDLE_3 = registerSoundEvents("chinchilla_idle_3");
    public static final RegistryObject<SoundEvent> CHINCHILLA_HURT = registerSoundEvents("chinchilla_hurt");
    public static final RegistryObject<SoundEvent> HEDGEHOG_IDLE_1 = registerSoundEvents("hedgehog_idle_1");
    public static final RegistryObject<SoundEvent> HEDGEHOG_IDLE_2 = registerSoundEvents("hedgehog_idle_2");
    public static final RegistryObject<SoundEvent> HEDGEHOG_IDLE_3 = registerSoundEvents("hedgehog_idle_3");
    public static final RegistryObject<SoundEvent> HEDGEHOG_HURT = registerSoundEvents("hedgehog_hurt");
    public static final RegistryObject<SoundEvent> RINGTAILCAT_IDLE_1 = registerSoundEvents("ringtailcat_idle_1");
    public static final RegistryObject<SoundEvent> RINGTAILCAT_IDLE_2 = registerSoundEvents("ringtailcat_idle_2");
    public static final RegistryObject<SoundEvent> RINGTAILCAT_HURT = registerSoundEvents("ringtailcat_hurt");
    public static final RegistryObject<SoundEvent> RINGTAILCAT_ANGRY = registerSoundEvents("ringtailcat_angry");
    public static final RegistryObject<SoundEvent> RINGTAILCAT_GROWL = registerSoundEvents("ringtailcat_growl");
    public static final RegistryObject<SoundEvent> RINGTAILCAT_SHRIEK = registerSoundEvents("ringtailcat_shriek");
    public static final RegistryObject<SoundEvent> RINGTAILCAT_ALERT = registerSoundEvents("ringtailcat_alert");
    public static final RegistryObject<SoundEvent> OPOSSUM_IDLE_1 = registerSoundEvents("opossum_idle_1");
    public static final RegistryObject<SoundEvent> OPOSSUM_IDLE_2 = registerSoundEvents("opossum_idle_2");
    public static final RegistryObject<SoundEvent> OPOSSUM_HURT = registerSoundEvents("opossum_hurt");
    public static final RegistryObject<SoundEvent> OPOSSUM_ANGRY = registerSoundEvents("opossum_angry");
    public static final RegistryObject<SoundEvent> MOUSE_IDLE_1 = registerSoundEvents("mouse_idle_1");
    public static final RegistryObject<SoundEvent> MOUSE_IDLE_2 = registerSoundEvents("mouse_idle_2");
    public static final RegistryObject<SoundEvent> MOUSE_HURT = registerSoundEvents("mouse_hurt");
    public static final RegistryObject<SoundEvent> MOUSE_STEAL = registerSoundEvents("mouse_steal");
    public static final RegistryObject<SoundEvent> SILKMOTH_HURT = registerSoundEvents("silkmoth_hurt");
    public static final RegistryObject<SoundEvent> LEAFSHEEP_HURT = registerSoundEvents("leafsheep_hurt");


    private static RegistryObject<SoundEvent> registerSoundEvents(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Faunify.MODID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
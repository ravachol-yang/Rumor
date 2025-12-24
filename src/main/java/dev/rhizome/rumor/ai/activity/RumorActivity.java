package dev.rhizome.rumor.ai.activity;

import dev.rhizome.rumor.Rumor;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RumorActivity {
    public static final DeferredRegister<Activity> ACTIVITIES =
            DeferredRegister.create(ForgeRegistries.ACTIVITIES, Rumor.MODID);

    public static final RegistryObject<Activity> CHAT =
            ACTIVITIES.register("chat", () -> new Activity("chat"));

    public static void register(IEventBus eventBus) {
        ACTIVITIES.register(eventBus);
    }
}

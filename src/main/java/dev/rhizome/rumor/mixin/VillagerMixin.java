package dev.rhizome.rumor.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import dev.rhizome.rumor.ai.activity.RumorActivity;
import dev.rhizome.rumor.ai.behavior.VillagerChat;
import dev.rhizome.rumor.ai.memory.RumorMemoryTypes;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public class VillagerMixin {

    @Shadow @Final private static ImmutableList<SensorType<? extends Sensor<? super Villager>>> SENSOR_TYPES;

    @Shadow @Final private static ImmutableList<MemoryModuleType<?>> MEMORY_TYPES;

    @Inject(method = "brainProvider", at = @At("RETURN"), cancellable = true)
    void injectMemoryModuleType(CallbackInfoReturnable<Brain.Provider<Villager>> cir) {
        cir.setReturnValue(Brain.provider(
                new ImmutableList.Builder<MemoryModuleType<?>>()
                        .addAll(MEMORY_TYPES)
                        .add(RumorMemoryTypes.CHAT_CONTEXT.get())
                        .add(RumorMemoryTypes.LAST_CHAT_TIME.get())
                        .build(),
                SENSOR_TYPES));
    }

    @Inject(method = "registerBrainGoals", at = @At("TAIL"))
    private void injectBehavior (Brain<Villager> pVillagerBrain, CallbackInfo ci) {
        pVillagerBrain.addActivity(RumorActivity.CHAT.get(), ImmutableList.of(
                Pair.of(0, new VillagerChat()),
                Pair.of(1, new DoNothing(20,40))
        ));
        pVillagerBrain.addActivity(Activity.IDLE,5, ImmutableList.of(new VillagerChat()));
    }
}

package dev.rhizome.rumor.ai.memory;

import dev.rhizome.rumor.Rumor;
import dev.rhizome.rumor.chat.ChatStatus;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Optional;

public class RumorMemoryTypes {

    public static final DeferredRegister<MemoryModuleType<?>> MEMORY_TYPES =
            DeferredRegister.create(ForgeRegistries.MEMORY_MODULE_TYPES, Rumor.MODID);

    // 用于存储状态枚举
    public static final RegistryObject<MemoryModuleType<ChatStatus>> CHAT_STATUS =
            MEMORY_TYPES.register("chat_status", () -> new MemoryModuleType<>(Optional.empty()));

    // 用于存储对话的目标实体（也就是对方村民）
    public static final RegistryObject<MemoryModuleType<Villager>> CHAT_TARGET =
            MEMORY_TYPES.register("chat_target", () -> new MemoryModuleType<>(Optional.empty()));

    // 聊天的发起者
    public static final RegistryObject<MemoryModuleType<Boolean>> IS_CHAT_LEADER =
            MEMORY_TYPES.register("is_chat_leader", () -> new MemoryModuleType<>(Optional.empty()));

    public static final RegistryObject<MemoryModuleType<Villager>> CHAT_LEADER =
            MEMORY_TYPES.register("chat_leader", () -> new MemoryModuleType<>(Optional.empty()));

    // 记录上次聊天的时间，用于控制冷却
    public static final RegistryObject<MemoryModuleType<Long>> LAST_CHAT_TIME =
            MEMORY_TYPES.register("last_chat_time", () -> new MemoryModuleType<>(Optional.empty()));

    public static void register(IEventBus eventBus) {
        MEMORY_TYPES.register(eventBus);
    }
}

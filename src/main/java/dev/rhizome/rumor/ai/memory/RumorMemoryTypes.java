package dev.rhizome.rumor.ai.memory;

import dev.rhizome.rumor.Rumor;
import dev.rhizome.rumor.chat.ChatContext;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Optional;

public class RumorMemoryTypes {

    public static final DeferredRegister<MemoryModuleType<?>> MEMORY_TYPES =
            DeferredRegister.create(ForgeRegistries.MEMORY_MODULE_TYPES, Rumor.MODID);

    /** 对话上下文 */
    public static final RegistryObject<MemoryModuleType<ChatContext>> CHAT_CONTEXT =
            MEMORY_TYPES.register("chat_context", () -> new MemoryModuleType<>(Optional.empty()));

    /** 记录上次聊天的时间，用于控制冷却 */
    public static final RegistryObject<MemoryModuleType<Long>> LAST_CHAT_TIME =
            MEMORY_TYPES.register("last_chat_time", () -> new MemoryModuleType<>(Optional.empty()));

    public static void register(IEventBus eventBus) {
        MEMORY_TYPES.register(eventBus);
    }
}

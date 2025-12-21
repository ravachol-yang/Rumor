package dev.rhizome.rumor;

import com.mojang.logging.LogUtils;
import dev.rhizome.rumor.ai.memory.RumorMemoryTypes;
import dev.rhizome.rumor.chat.ChatScriptManager;
import dev.rhizome.rumor.config.RumorConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Rumor.MODID)
public class Rumor {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "rumor";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public Rumor() {

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        RumorMemoryTypes.register(modEventBus);

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // 注册资源加载监听器
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);

        // 注册配置加载器
        FMLJavaModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RumorConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    // 在游戏启动和运行 /reload 命令时触发
    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        // 注册剧本管理器
        event.addListener(new ChatScriptManager());
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        }
    }
}

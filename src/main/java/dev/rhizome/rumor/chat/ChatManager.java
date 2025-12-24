package dev.rhizome.rumor.chat;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChatManager {
    private static final Set<ChatContext> ACTIVE_CONTEXTS = ConcurrentHashMap.newKeySet();

    public static void registerContext(ChatContext ctx) {
        ACTIVE_CONTEXTS.add(ctx);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<ChatContext> ctxIterator = ACTIVE_CONTEXTS.iterator();

        // 遍历所有正在活动的上下文
        while (ctxIterator.hasNext()) {
            ChatContext ctx = ctxIterator.next();

            if (!ctx.isValid()) {
                ctx.restoreNames();
                // ctxIterator.remove();
                continue;
            }

            ctx.tick();

            if (ctx.getStatus() == ChatContext.Status.FINISHED) {
                ctxIterator.remove();
            }
        }
    }
}

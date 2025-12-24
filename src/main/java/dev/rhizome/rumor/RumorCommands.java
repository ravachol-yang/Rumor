package dev.rhizome.rumor;

import com.mojang.brigadier.CommandDispatcher;
import dev.rhizome.rumor.ai.activity.RumorActivity;
import dev.rhizome.rumor.ai.memory.RumorMemoryTypes;
import dev.rhizome.rumor.chat.ChatContext;
import dev.rhizome.rumor.chat.ChatManager;
import dev.rhizome.rumor.chat.script.ChatScript;
import dev.rhizome.rumor.chat.script.ChatScriptManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class RumorCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("rumor")
                .requires(source -> source.hasPermission(2)) // 仅管理员可用
                .then(Commands.literal("start")
                        .then(Commands.argument("script", ResourceLocationArgument.id())
                                // 自动补全：列出 ChatScriptManager 中加载的所有剧本 ID
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        ChatScriptManager.getRegisteredIds(), builder))
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    ServerPlayer player = source.getPlayerOrException();
                                    ResourceLocation location = ResourceLocationArgument.getId(context, "script");

                                    // 查找玩家准星指向的村民
                                    Villager villager = getTargetVillager(player);
                                    if (villager == null) {
                                        source.sendFailure(Component.literal("§c请指向一名村民！"));
                                        return 0;
                                    }

                                    // 获取剧本
                                    ChatScript script = ChatScriptManager.getScript(location);
                                    if (script == null) {
                                        source.sendFailure(Component.literal("§c未找到 ID 为 " + location + " 的剧本"));
                                        return 0;
                                    }

                                    // 组长由准星指向的村民担当，初始成员列表仅包含组长
                                    ChatContext ctx = new ChatContext((ServerLevel) villager.level(), new ArrayList<>(List.of(villager)), script);

                                    // 注入
                                    villager.getBrain().setMemory(RumorMemoryTypes.CHAT_CONTEXT.get(), ctx);
                                    villager.getBrain().setActiveActivityIfPossible(RumorActivity.CHAT.get());

                                    // 注册到管理器开始全局调度
                                    ChatManager.registerContext(ctx);

                                    return 1;
                                })
                        )
                )
        );
    }

    /**
     * 获取玩家准星指向的实体是否为村民
     */
    private static Villager getTargetVillager(ServerPlayer player) {
        double pickRange = 5.0D;

        Vec3 eyePosition = player.getEyePosition();
        Vec3 viewVector = player.getViewVector(1.0F);
        Vec3 reachVector = eyePosition.add(viewVector.scale(pickRange));

        AABB searchBox = player.getBoundingBox().expandTowards(viewVector.scale(pickRange)).inflate(1.0D);

        EntityHitResult hitResult = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player.level(),
                player,
                eyePosition,
                reachVector,
                searchBox,
                (entity) -> entity instanceof Villager && entity.isAlive()
        );

        if (hitResult != null && hitResult.getEntity() instanceof Villager villager) {
            return villager;
        }

        return null;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }
}

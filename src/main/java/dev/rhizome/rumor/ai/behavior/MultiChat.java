package dev.rhizome.rumor.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import dev.rhizome.rumor.ai.memory.RumorMemoryTypes;
import dev.rhizome.rumor.chat.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public class MultiChat extends Behavior<Villager> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final double INTERACT_DIST = 2.5D; // 开始对话的距离

    // 对话总时长
    private static final int MAX_CHAT_DURATION = 3000;
    // 冷却时间
    private static final int COOLDOWN_DURATION = 6000;

    /**
     * 检查行为启动条件
     * 按照状态进行检查和更新
     * 依次为冷却，聊天，等待，搜索
     * TODO 对于状态的判断不够优雅
     */
    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel pLevel, @NotNull Villager pOwner) {

        // 获取状态，如果没有，设置为等待
        ChatStatus status = pOwner.getBrain().getMemory(RumorMemoryTypes.CHAT_STATUS.get()).orElse(ChatStatus.WAITING);

        // 冷却状态，时间足够，更新为等待，否则继续冷却
        if (status == ChatStatus.COOLDOWN) {
            Optional<Long> lastChatTime = pOwner.getBrain().getMemory(RumorMemoryTypes.LAST_CHAT_TIME.get());
            if (lastChatTime.isPresent() && pLevel.getGameTime() - lastChatTime.get() >= COOLDOWN_DURATION) {
                // 时间足够，设置为等待
                pOwner.getBrain().setMemory(RumorMemoryTypes.CHAT_STATUS.get(),ChatStatus.WAITING);
            }
            return false;
        }

        // 聊天状态，则继续
        if (status == ChatStatus.CHATTING) return true;

        // 等待状态，随机抽取进入搜索，否则继续等待
        // 这个状态的存在是为了区分主动发起对话者和参与者
        // 否则如果出现所有人都发起对话并搜索，谁也找不到参与者
        if (status == ChatStatus.WAITING) {

            // 有极低的概率，选中此人为对话发起者
            // 每100个tick（5秒）允许进入一次判定，否则被选中的概率还是太高
            if (pLevel.getGameTime() % 100 == 0 && pLevel.random.nextFloat() < 0.01F) {
                // 改变状态为搜寻
                pOwner.getBrain().setMemory(RumorMemoryTypes.CHAT_STATUS.get(), ChatStatus.SEARCHING);
            }
            return false;
        }

        // 搜索状态，按剧本人数搜索附近等待状态的人员，搜索到之后，进入对话状态
        if (status == ChatStatus.SEARCHING) {
            // 抽取随机剧本
            ChatScript script = ChatScriptManager.getRandomScript(pLevel.random);
            if (script == null) return false;

            LOGGER.debug("Select script: [{}], require {} participants: [{}], search range: [{}]",
                    script.id(),
                    script.uniqueIds().size(),
                    script.uniqueIds(),
                    script.searchRange());

            // 获取剧本所需人数-1的村民（排除自己）
            Optional<List<Villager>> participants = findWaitingVillagers(pLevel,
                    pOwner,
                    script.uniqueIds().size() - 1,
                    script.searchRange());

            if (participants.isPresent()) {
                // 成功则发起聊天
                participants.get().add(0,pOwner);
                initChat(participants.get(), script);
                return true;
            } else {
                // 搜寻失败，退回等待状态
                pOwner.getBrain().setMemory(RumorMemoryTypes.CHAT_STATUS.get(), ChatStatus.WAITING);
                return false;
            }
        }

        // 其他未知状态返回false
        return false;
    }

    /**
     * 检查行为是否能继续
     * 如果参与者全部存活就返回true
     */
    @Override
    protected boolean canStillUse(@NotNull ServerLevel pLevel, Villager pEntity, long pGameTime) {
        Optional<ChatContext> ctxOpt = pEntity.getBrain().getMemory(RumorMemoryTypes.CHAT_CONTEXT.get());
        if (ctxOpt.isPresent()) {
            for (Villager v: ctxOpt.get().getMembers()) {
                if (!v.isAlive()) return false;
            }
            return true;
        }
        return false;
    }

    /**
     * 行为开始时初始化
     * 目前的初始行为是全体走向并看向组长
     * TODO 未来实现可配置的初始状态
     */
    @Override
    protected void start(@NotNull ServerLevel pLevel, @NotNull Villager pEntity, long pGameTime) {
        ChatContext ctx = pEntity.getBrain().getMemory(RumorMemoryTypes.CHAT_CONTEXT.get()).orElse(null);
        if (ctx == null) return;

        if (pEntity.distanceToSqr(ctx.getMembers().get(0)) > INTERACT_DIST * INTERACT_DIST) {
            BehaviorUtils.setWalkAndLookTargetMemories(pEntity, ctx.getMembers().get(0), 0.5F, 1);
        }
    }

    /**
     * 每一个tick的行为
     * 先由组长判断初始状态是否达成，并决定是否开始剧本
     * 通过检查上下文中的当前tick与当前步骤的tick是否一致进行对话
     * 对话的第0个成员负责调用上下文的tick()推进时间
     * 在时间到达后结束并各自进行清理
     */
    @Override
    protected void tick(@NotNull ServerLevel pLevel, @NotNull Villager pOwner, long pGameTime) {
        // 获取上下文
        ChatContext ctx = pOwner.getBrain().getMemory(RumorMemoryTypes.CHAT_CONTEXT.get()).orElse(null);
        if (ctx == null) return;

        // 上下文显示剧本已结束或超时，开始清理
        if (ctx.isFinished() || ctx.getCurrentTick() >= MAX_CHAT_DURATION) {
            stop(pLevel, pOwner, pGameTime);
            return;
        }

        boolean isLeader = pOwner == ctx.getMembers().get(0);

        // 开始对话前，组长检查初始状态
        if (!ctx.isActive() && isLeader) {
            boolean ready = true;
            // 距离足够，停止走路，否则继续走并重置ready为false
            for (Villager v: ctx.getMembers()) {
                if (v.distanceToSqr(pOwner) <= INTERACT_DIST * INTERACT_DIST) {
                    BehaviorUtils.lookAtEntity(v,pOwner);
                } else ready = false;
            }

            if (ready) ctx.start();
        }

        // 对话过程
        if (ctx.isActive()) {

            // TODO 暂时全部保持固定方向
            BehaviorUtils.lookAtEntity(pOwner, ctx.getMembers().get(0));

            if (pOwner.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
                pOwner.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            }

            ChatScriptStep step = ctx.getCurrentStep();

            // 当前步骤的tick为当前tick，需要推进
            if (step != null && step.tickNode() == ctx.getCurrentTick()) {

                // 如果轮到自己，就发言
                if (pOwner == ctx.getMemberMap().get(step.speakerId())) {
                    broadcastMessage(pLevel, pOwner, step.text(), step.broadcastRange());
                    ctx.nextStep(); // 推进剧本
                }
            }

            // 组长（第0个成员）负责控制计时器
            if (isLeader) {
                ctx.tick();
            }
        }
    }

    /**
     * 停止行为，擦除记忆并设置冷却
     */
    @Override
    protected void stop(@NotNull ServerLevel pLevel, Villager pEntity, long pGameTime) {
        LOGGER.debug("Chat stopped, clean up");
        // 清理自身的记忆
        pEntity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        pEntity.getBrain().eraseMemory(RumorMemoryTypes.CHAT_CONTEXT.get());

        LOGGER.debug("setting cooldown {} tick(s) with last_chat_time: {}",COOLDOWN_DURATION,pGameTime);
        // 设置冷却
        pEntity.getBrain().setMemory(RumorMemoryTypes.LAST_CHAT_TIME.get(), pGameTime);
        pEntity.getBrain().setMemory(RumorMemoryTypes.CHAT_STATUS.get(), ChatStatus.COOLDOWN);
    }

    /**
     * 搜寻聊天对象
     * @param level 目前所在的维度
     * @param self 发起搜寻的村民
     * @param count 所需查找的人数
     * @param range 查找范围
     */
    private Optional<List<Villager>> findWaitingVillagers (ServerLevel level,
                                                           Villager self,
                                                           int count,
                                                           Double range) {

        List<Villager> villagers = level.getEntitiesOfClass(Villager.class,
                self.getBoundingBox().inflate(range),
                (v) -> v != self && v.isAlive() && !v.isBaby() && isAvailable(v));

        LOGGER.debug("found {} available villagers within range {}", villagers.size(), range);

        if (villagers.size() >= count) {
            LOGGER.debug("require {}, found {} available", count, villagers.size());
            // 截取需要的人数
            return Optional.of(villagers.subList(0,count));
        }

        LOGGER.debug("not enough villagers, returning none");
        return Optional.empty();
    }

    /**
     * 判断目标村民可以聊天
     * @param villager 需要被判断的村民
     */
    private boolean isAvailable(Villager villager) {
        // 获取状态, 默认为等待
        ChatStatus status = villager.getBrain().getMemory(RumorMemoryTypes.CHAT_STATUS.get()).orElse(ChatStatus.WAITING);
        return status == ChatStatus.WAITING;
    }

    /**
     * 基于现有的成员和剧本发起聊天
     * @param participants 聊天参与者, 第一个会成为Leader
     * @param script 剧本
     */
    private void initChat(List<Villager> participants, ChatScript script) {

        // 初始化对话上下文
        ChatContext ctx = new ChatContext(participants,script);

        LOGGER.debug("Chat context initiated, script: [{}]; participants count: {}", script.id(), participants.size());

        ctx.getMembers().forEach(v -> {
            v.getBrain().setMemory(RumorMemoryTypes.CHAT_CONTEXT.get(), ctx);
            v.getBrain().setMemory(RumorMemoryTypes.CHAT_STATUS.get(), ChatStatus.CHATTING); // 更新状态
        });
    }

    /**
     * 向指定距离内的玩家广播消息
     * @param level 所在的维度
     * @param speaker 说话者，用于判断广播的中心位置
     * @param message 说话内容
     */
    private void broadcastMessage(ServerLevel level, Entity speaker, String message, Double range) {

        // MC的消息需要拼接
        Component text = Component.literal(message);

        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class,
                speaker.getBoundingBox().inflate(range));

        LOGGER.debug("broadcasting message content [{}] with range [{}] to [{}] player(s)",
                message, range, players.size());

        for (ServerPlayer player : players) {
            player.sendSystemMessage(text);
        }
    }

    public MultiChat() {

        // 该行为需要的Memory状态 (pEntryCondition) 和最大行为时长
        super(ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
                        MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
                        RumorMemoryTypes.CHAT_STATUS.get(), MemoryStatus.REGISTERED,
                        RumorMemoryTypes.CHAT_CONTEXT.get(), MemoryStatus.REGISTERED,
                        RumorMemoryTypes.LAST_CHAT_TIME.get(),MemoryStatus.REGISTERED),
                MAX_CHAT_DURATION * 2);
    }
}

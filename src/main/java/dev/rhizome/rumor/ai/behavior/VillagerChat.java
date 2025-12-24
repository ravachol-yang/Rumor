package dev.rhizome.rumor.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import dev.rhizome.rumor.ai.activity.RumorActivity;
import dev.rhizome.rumor.ai.memory.RumorMemoryTypes;
import dev.rhizome.rumor.chat.*;
import dev.rhizome.rumor.chat.script.ChatScript;
import dev.rhizome.rumor.chat.script.ChatScriptManager;
import dev.rhizome.rumor.RumorConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VillagerChat extends Behavior<Villager> {

    private static final Logger LOGGER = LogUtils.getLogger();

    // 村民可随机被选为搜索状态的间隔，单位: tick
    private static final int ELECTION_INTERVAL_TICK = RumorConfig.ELECTION_INTERVAL.get() * 20;

    // 对话总时长，单位: tick
    private static final int MAX_CHAT_DURATION = 3000;
    // 冷却时间, 单位: tick
    private static final int COOLDOWN_TICKS = RumorConfig.COOLDOWN.get() * 20;

    /**
     * 检查行为启动条件
     * 按照状态进行检查和更新
     * 依次为冷却，聊天，等待，搜索
     * TODO 对于状态的判断不够优雅
     */
    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel pLevel, @NotNull Villager pOwner) {
        Optional<Long> lastChatTime = pOwner.getBrain().getMemory(RumorMemoryTypes.LAST_CHAT_TIME.get());

        // 正在冷却
        if (lastChatTime.isPresent() &&
                pLevel.getGameTime() - lastChatTime.get() < COOLDOWN_TICKS) return false;

        // 已有上下文，进入执行逻辑
        if (pOwner.getBrain().getMemory(RumorMemoryTypes.CHAT_CONTEXT.get()).isPresent()) return true;

        // 抽中作为队长
        if (pLevel.getGameTime() % ELECTION_INTERVAL_TICK == 0 && pLevel.random.nextFloat() < 0.01F) {
            ChatScript script = ChatScriptManager.getRandomScript(pLevel.random);

            if (script != null) {
                LOGGER.debug("selected script: {}, require {} participants: [{}]",
                        script.location(),
                        script.roles().size(),
                        script.roles());

                // 初始化上下文并注册到自己的记忆以及全局管理器
                ChatContext ctx = new ChatContext(pLevel, new ArrayList<>(List.of(pOwner)), script);

                pOwner.getBrain().setMemory(RumorMemoryTypes.CHAT_CONTEXT.get(), ctx);
                ChatManager.registerContext(ctx);
                return true;
            }
        }

        return false;
    }

    /**
     * 检查上下文是否可用
     * 决定是否继续执行
     */
    @Override
    protected boolean canStillUse(@NotNull ServerLevel pLevel, Villager pEntity, long pGameTime) {
        Optional<ChatContext> ctxOpt = pEntity.getBrain().getMemory(RumorMemoryTypes.CHAT_CONTEXT.get());
        return ctxOpt.isPresent();
    }

    /**
     * 行为开始时初始化
     * 切换到我们自己的Activity，防止与原版行为竞争
     */
    @Override
    protected void start(@NotNull ServerLevel pLevel, @NotNull Villager pEntity, long pGameTime) {
        pEntity.getBrain().setActiveActivityIfPossible(RumorActivity.CHAT.get());
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

        // 如果上下文已不可用
        if (!ctx.isValid()) {
            pLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    pOwner.getX(),pOwner.getY()+0.5,pOwner.getZ(),
                    5,0.2,0.2,0.2,0);
            pOwner.getBrain().eraseMemory(RumorMemoryTypes.CHAT_CONTEXT.get());
            return;
        }

        ChatContext.Status status = ctx.getStatus();

        // 正在招募
        if (status.equals(ChatContext.Status.RECRUITING)) {

            // 每半秒发送粒子效果
            if (pGameTime % 10 == 0) {
                pLevel.sendParticles(ParticleTypes.NOTE,
                        pOwner.getX(), pOwner.getY() + 2.5, pOwner.getZ(),
                        1, 0, 0, 0, 1.0);
            }
        }

        // 正在靠拢
        if (status.equals(ChatContext.Status.GATHERING)) {
            // 设置高亮效果提示玩家
            pOwner.setGlowingTag(true);
            if (pOwner.blockPosition().distSqr(ctx.getCenterPos()) > 3.0D * 3.0D ) {
                BehaviorUtils.setWalkAndLookTargetMemories(pOwner, ctx.getCenterPos(), 0.5F, 1);
            }
            return;
        }


        // 防止东张西望
        pOwner.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);

        // 从上下文获取当前村民视线状态并更新
        Villager lookTarget = ctx.getLookAt(pOwner);
        Villager currentSpeaker = ctx.getCurrentSpeaker();
        if (lookTarget != null) BehaviorUtils.lookAtEntity(pOwner, lookTarget);
        pOwner.setGlowingTag(pOwner == currentSpeaker);

        // 对话阶段防止走动
        pOwner.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        pOwner.getBrain().eraseMemory(MemoryModuleType.PATH);

        // 检查对话是否已完成
        if (status.equals(ChatContext.Status.FINISHED)) stop(pLevel, pOwner, pGameTime);
    }

    /**
     * 停止行为，擦除记忆并设置冷却
     */
    @Override
    protected void stop(@NotNull ServerLevel pLevel, Villager pEntity, long pGameTime) {
        LOGGER.debug("Chat stopped, clean up");
        pEntity.setGlowingTag(false);
        // 清理自身的记忆
        pEntity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        pEntity.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        pEntity.getBrain().eraseMemory(RumorMemoryTypes.CHAT_CONTEXT.get());

        LOGGER.debug("setting cooldown {} tick(s) with last_chat_time: {}", COOLDOWN_TICKS,pGameTime);
        // 设置冷却
        pEntity.getBrain().setMemory(RumorMemoryTypes.LAST_CHAT_TIME.get(), pGameTime);
        pEntity.getBrain().updateActivityFromSchedule(pLevel.getDayTime(),pGameTime);
    }

    public VillagerChat() {

        // 该行为需要的Memory状态 (pEntryCondition) 和最大行为时长
        super(ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
                        MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
                        RumorMemoryTypes.CHAT_CONTEXT.get(), MemoryStatus.REGISTERED,
                        RumorMemoryTypes.LAST_CHAT_TIME.get(),MemoryStatus.REGISTERED),
                MAX_CHAT_DURATION * 2);
    }
}

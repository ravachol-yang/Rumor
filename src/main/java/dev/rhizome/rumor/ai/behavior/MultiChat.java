package dev.rhizome.rumor.ai.behavior;

import com.google.common.collect.ImmutableMap;
import dev.rhizome.rumor.ai.memory.RumorMemoryTypes;
import dev.rhizome.rumor.chat.ChatStatus;
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

import java.util.List;
import java.util.Optional;

public class MultiChat extends Behavior<Villager> {

    private static final double SEARCH_RANGE = 10D; // 寻找队友的范围
    private static final double INTERACT_DIST = 2.5D; // 开始对话的距离
    private static final double PLAYER_LISTEN_RANGE = 8.0D; // 玩家偷听范围

    // 计时器,用于控制对话持续时间
    // 单位为tick, 会在每一次执行tick()时更新
    private int chatTimer = 0;
    // 对话总时长
    private static final int MAX_CHAT_DURATION = 60;
    // 冷却时间
    private static final int COOLDOWN_DURATION = 6000;

    /**
     * 检查行为启动条件
     * 先判断是否在冷却
     * 如果Memory有聊天目标，就继续执行
     * 没有就找一个目标开始聊天
     * TODO 对于状态的判断不够优雅
     */
    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel pLevel, @NotNull Villager pOwner) {

        // 如果有聊天对象，就继续
        if (pOwner.getBrain().hasMemoryValue(RumorMemoryTypes.CHAT_TARGET.get())) {
            return true;
        }

        // 如果正在搜寻，就找一个
        if (pOwner.getBrain().isMemoryValue(RumorMemoryTypes.CHAT_STATUS.get(), ChatStatus.SEARCHING)) {
            Optional<Villager> target = findChatTarget(pLevel, pOwner);

            if (target.isPresent()) {

                Villager targetVillager = target.get();

                // 设置己方的聊天状态和对象
                pOwner.getBrain().setMemory(RumorMemoryTypes.CHAT_STATUS.get(), ChatStatus.APPROACHING);
                pOwner.getBrain().setMemory(RumorMemoryTypes.CHAT_TARGET.get(), targetVillager);
                pOwner.getBrain().setMemory(RumorMemoryTypes.IS_CHAT_LEADER.get(), true);

                // 设置对方的聊天状态和对象
                targetVillager.getBrain().setMemory(RumorMemoryTypes.CHAT_STATUS.get(), ChatStatus.APPROACHING);
                targetVillager.getBrain().setMemory(RumorMemoryTypes.CHAT_TARGET.get(), pOwner);

                return true;
            }
        }

        // 冷却检查
        if (pOwner.getBrain().isMemoryValue(RumorMemoryTypes.CHAT_STATUS.get(),ChatStatus.COOLDOWN)) {
            Optional<Long> lastChatTimeOpt = pOwner.getBrain().getMemory(RumorMemoryTypes.LAST_CHAT_TIME.get());
            if (lastChatTimeOpt.isPresent()) {
                long lastChatTime = lastChatTimeOpt.get();
                long currentTime = pLevel.getGameTime();
                if (currentTime - lastChatTime >= COOLDOWN_DURATION) {
                    // 冷却时间足够，状态更新为搜寻
                    pOwner.getBrain().setMemory(RumorMemoryTypes.CHAT_STATUS.get(), ChatStatus.SEARCHING);
                }
            }
        } else {
            // 如果既不是在靠近，聊天也不是在搜寻或冷却，就直接设置为搜寻
            pOwner.getBrain().setMemory(RumorMemoryTypes.CHAT_STATUS.get(), ChatStatus.SEARCHING);
        }

        // 其他未知状态返回false
        return false;
    }

    /**
     * 检查行为是否能继续
     * 如果目标存活就返回true
     */
    @Override
    protected boolean canStillUse(@NotNull ServerLevel pLevel, Villager pEntity, long pGameTime) {
        return pEntity.getBrain().hasMemoryValue(RumorMemoryTypes.CHAT_TARGET.get()) &&
                pEntity.getBrain().getMemory(RumorMemoryTypes.CHAT_TARGET.get()).get().isAlive();
    }

    /**
     * 行为开始时初始化
     * 重置计时器
     */
    @Override
    protected void start(@NotNull ServerLevel pLevel, @NotNull Villager pEntity, long pGameTime) {
        this.chatTimer = 0;
    }

    /**
     * 每一个tick的行为，分为靠近，聊天，结束三个阶段
     */
    @Override
    protected void tick(@NotNull ServerLevel pLevel, @NotNull Villager pOwner, long pGameTime) {

        // 获取聊天目标
        Villager target = pOwner.getBrain().getMemory(RumorMemoryTypes.CHAT_TARGET.get()).orElse(null);
        if (target == null) return;

        // 判断自己的角色
        boolean isLeader = pOwner.getBrain().hasMemoryValue(RumorMemoryTypes.IS_CHAT_LEADER.get());

        // 靠近
        double distance = pOwner.distanceToSqr(target);

        // 如果距离过远，就走向目标
        if (distance > INTERACT_DIST * INTERACT_DIST) {
            BehaviorUtils.setWalkAndLookTargetMemories(pOwner, target, 0.5F, 1);
        }
        // 聊天
        else {

            // 距离足够，停止走路
            pOwner.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            // 看向对方
            BehaviorUtils.lookAtEntity(pOwner, target);

            // 更新状态
            pOwner.getBrain().setMemory(RumorMemoryTypes.CHAT_STATUS.get(), ChatStatus.CHATTING);

            // 聊天过程暂时由Leader来模拟
            if (isLeader) {
                this.chatTimer++;

                // 在指定时刻开始广播消息
                if (this.chatTimer == 10) { // 聊天开始后第10个tick
                    broadcastMessage(pLevel, pOwner, "Hello, World");
                } else if (this.chatTimer == 50) { // 到第20个tick有回应
                    broadcastMessage(pLevel, target, "Hello");
                }

                // 结束
                if (this.chatTimer >= MAX_CHAT_DURATION) {
                    // 调用stop()清理记忆
                    stop(pLevel, target, pGameTime);
                    stop(pLevel, pOwner, pGameTime);
                }
            }
        }
    }

    /**
     * 停止行为，擦除记忆并设置冷却
     */
    @Override
    protected void stop(@NotNull ServerLevel pLevel, Villager pEntity, long pGameTime) {

        // 清理自身的记忆
        pEntity.getBrain().eraseMemory(RumorMemoryTypes.CHAT_TARGET.get());
        pEntity.getBrain().eraseMemory(RumorMemoryTypes.IS_CHAT_LEADER.get());
        pEntity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

        // 设置冷却
        pEntity.getBrain().setMemory(RumorMemoryTypes.LAST_CHAT_TIME.get(), pGameTime);
        pEntity.getBrain().setMemory(RumorMemoryTypes.CHAT_STATUS.get(), ChatStatus.COOLDOWN);
    }

    /**
     * 搜寻聊天对象
     */
    private Optional<Villager> findChatTarget (ServerLevel level, Villager self) {
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class,
                self.getBoundingBox().inflate(SEARCH_RANGE),
                (v) -> v != self && v.isAlive() && !v.isBaby() && isAvailable(v));

        if (!villagers.isEmpty()) {
            // 返回其中第一个
            return Optional.of(villagers.get(0));
        }
        return Optional.empty();
    }

    /**
     * 判断目标村民是否空闲
     */
    private boolean isAvailable(Villager v) {
        // 对方没有在聊天状态，且没有在忙其他高优先级的事情
        return !v.getBrain().hasMemoryValue(RumorMemoryTypes.CHAT_TARGET.get());
    }

    /**
     * 向指定距离内的玩家广播消息
     */
    private void broadcastMessage(ServerLevel level, Entity speaker, String message) {
        // MC的消息需要拼接
        Component text = Component.literal(message);

        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class,
                speaker.getBoundingBox().inflate(PLAYER_LISTEN_RANGE));

        for (ServerPlayer player : players) {
            player.sendSystemMessage(text);
        }
    }

    public MultiChat() {

        // 该行为需要的Memory状态 (pEntryCondition) 和最大行为时长
        super(ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
                        MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
                        RumorMemoryTypes.CHAT_TARGET.get(), MemoryStatus.REGISTERED,
                        RumorMemoryTypes.CHAT_STATUS.get(), MemoryStatus.REGISTERED,
                        RumorMemoryTypes.LAST_CHAT_TIME.get(),MemoryStatus.REGISTERED),
                MAX_CHAT_DURATION * 2);
    }
}

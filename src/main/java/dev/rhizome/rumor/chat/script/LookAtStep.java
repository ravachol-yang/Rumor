package dev.rhizome.rumor.chat.script;

import dev.rhizome.rumor.chat.ChatContext;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;

/**
 * 一个角色看向另一个角色的步骤
 * @param actorId 当前步骤主体的id
 * @param targetId 看向的目标id
 * @param triggerTick 当前步骤执行的时间节点
 */
public record LookAtStep(String actorId,
                         String targetId,
                         int triggerTick)
        implements IScriptStep {
    @Override
    public String getActorId() {
        return actorId;
    }

    @Override
    public int getTriggerTick() {
        return triggerTick;
    }

    @Override
    public void exec(Villager actor, ChatContext ctx) {
        Villager target = ctx.getMemberMap().get(targetId);
        if (target != null) {
            // 设置看向目标
            actor.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        }
    }
}

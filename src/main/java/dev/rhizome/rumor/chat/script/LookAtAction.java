package dev.rhizome.rumor.chat.script;

import dev.rhizome.rumor.chat.ChatContext;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;

/**
 * 一个角色看向另一个角色的步骤
 * @param actorId 当前步骤主体的id
 * @param targetId 看向的目标id
 * @param durationTicks 当前步骤持续的时长
 */
public record LookAtAction(String actorId,
                           String targetId,
                           int durationTicks)
        implements IAction {
    @Override
    public String getActorId() {
        return actorId;
    }

    @Override
    public int getDurationTicks() {
        return durationTicks;
    }

    @Override
    public void apply(ChatContext ctx) {
        Villager actor = ctx.getMemberMap().get(actorId);
        Villager target = ctx.getMemberMap().get(targetId);
        if (actor != null && target != null) {
            // 设置看向目标
            actor.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        }
    }
}

package dev.rhizome.rumor.chat.script;

import dev.rhizome.rumor.chat.ChatContext;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.npc.Villager;

/**
 * 给玩家奖励经验球
 * 目前是直接投放，任何人都可以领到
 * @param actorId 当前步骤主体的id
 * @param amount 经验数量
 * @param durationTicks 当前步骤持续的时长
 */
public record ExpAction(String actorId,
                        int amount,
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
        if (amount <= 0) return;

        Villager actor = ctx.getMemberMap().get(actorId);

        // 创建一个位置和当前村民相同的经验球并投放
        ExperienceOrb expOrb = new ExperienceOrb(ctx.getLevel(),
                actor.getX(),actor.getY(),actor.getZ(),
                amount);

        ctx.getLevel().addFreshEntity(expOrb);
    }
}

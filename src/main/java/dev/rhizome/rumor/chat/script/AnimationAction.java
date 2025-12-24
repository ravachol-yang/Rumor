package dev.rhizome.rumor.chat.script;

import dev.rhizome.rumor.chat.ChatContext;
import net.minecraft.world.entity.npc.Villager;

public record AnimationAction(String actorId,
                              byte eventId,
                              int durationTicks) implements IAction {
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

        ctx.getLevel().broadcastEntityEvent(actor, eventId);
    }
}

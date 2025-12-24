package dev.rhizome.rumor.chat.script;

import dev.rhizome.rumor.chat.ChatContext;
import dev.rhizome.rumor.RumorConfig;
import net.minecraft.world.entity.npc.Villager;

/**
 * 对话剧本中的一个步骤
 * @param actorId 当前步骤主体的id
 * @param message 说话内容
 * @param durationTicks 当前步骤持续的时长
 * @param broadcastRange 广播范围 (输入null标记为默认)
 */
public record MessageAction(String actorId,
                            String message,
                            int durationTicks,
                            Double broadcastRange)
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
        if (actor != null) {
            ctx.setCurrentSpeaker(actor);
            ctx.broadcastActionBar(actorId,message,broadcastRange);
        }
    }

    public MessageAction {

        // 可选参数，标记为使用默认
        if (broadcastRange == null || broadcastRange <= 0) {
            broadcastRange = RumorConfig.DEFAULT_BROADCAST_RANGE.get();
        }
    }
}

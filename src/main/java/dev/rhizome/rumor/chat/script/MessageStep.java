package dev.rhizome.rumor.chat.script;

import dev.rhizome.rumor.chat.ChatContext;
import dev.rhizome.rumor.config.RumorConfig;
import net.minecraft.world.entity.npc.Villager;

/**
 * 对话剧本中的一个步骤
 * @param actorId 当前步骤主体的id
 * @param message 说话内容
 * @param triggerTick 当前步骤执行的时间节点
 * @param broadcastRange 广播范围 (输入null标记为默认)
 */
public record MessageStep(String actorId,
                          String message,
                          int triggerTick,
                          Double broadcastRange)
        implements IScriptStep{

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

    }

    public MessageStep {

        // 可选参数，标记为使用默认
        if (broadcastRange == null || broadcastRange <= 0) {
            broadcastRange = RumorConfig.DEFAULT_BROADCAST_RANGE.get();
        }
    }
}

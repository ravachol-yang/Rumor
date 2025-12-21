package dev.rhizome.rumor.chat;

import dev.rhizome.rumor.config.RumorConfig;

/**
 * 对话剧本中的一个步骤
 * @param speakerId 当前说话者的id
 * @param text 说话内容
 * @param tickNode 当前步骤执行的时间节点
 * @param broadcastRange 广播范围 (输入null标记为默认)
 */
public record ChatScriptStep(String speakerId,
                             String text,
                             int tickNode,
                             Double broadcastRange) {

    public ChatScriptStep {

        // 可选参数，标记为使用默认
        if (broadcastRange == null || broadcastRange <= 0) {
            broadcastRange = RumorConfig.DEFAULT_BROADCAST_RANGE.get();
        }
    }
}

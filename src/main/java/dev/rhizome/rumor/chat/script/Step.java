package dev.rhizome.rumor.chat.script;

import dev.rhizome.rumor.chat.ChatContext;

import java.util.List;

public record Step(List<IAction> actions, int durationTicks) {

    /**
     * 执行该步骤下所有操作，应当在上下文中调用
     * @param ctx 当前对话的上下文
     */
    public void exec(ChatContext ctx) {
        actions.forEach(action -> {
            action.apply(ctx);
        });
    }
}

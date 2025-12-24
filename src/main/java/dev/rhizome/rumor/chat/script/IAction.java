package dev.rhizome.rumor.chat.script;

import dev.rhizome.rumor.chat.ChatContext;

public interface IAction {

    String getActorId();

    int getDurationTicks();

    void apply(ChatContext ctx);
}

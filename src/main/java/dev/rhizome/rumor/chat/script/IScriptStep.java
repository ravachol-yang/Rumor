package dev.rhizome.rumor.chat.script;

import dev.rhizome.rumor.chat.ChatContext;
import net.minecraft.world.entity.npc.Villager;

public interface IScriptStep {

    String getActorId();

    int getTriggerTick();

    void exec(Villager actor, ChatContext ctx);
}

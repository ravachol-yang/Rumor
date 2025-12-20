package dev.rhizome.rumor.chat;

import net.minecraft.world.entity.npc.Villager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatContext {

    private final List<Villager> members; // 聊天人员列表
    private final Map<String, Villager> memberMap; // 人员与剧本内id对应关系，用于分配角色
    private final ChatScript script; // 聊天所使用的剧本

    // 计数器，记录当前步骤
    private int chatTimer = 0;
    private int currentStepIndex = 0; // 步骤执行索引
    private boolean active = false; // 标记对话是否正在活动
    private boolean finished = false; // 标记对话是否已经完成

    /** 开始对话 */
    public void start() {
        active = true;
    }

    /** 获取当前步骤*/
    public ChatScriptStep getCurrentStep () {
        return script.steps().get(currentStepIndex);
    }

    /** 步骤推进 */
    public void nextStep () {
        currentStepIndex++;
        if (currentStepIndex > script.steps().size() - 1) {
            active = false;
            finished = true;
        }
    }

    /** 计时器，从0开始tick，在快Behavior中只有leader执行 */
    public void tick() {
        chatTimer++;
        // 时间到达后标记结束
        if (chatTimer >= script.totalDuration()) {
            active = false;
            finished = true;
        }
    }

    /** 获取当前tick */
    public int getCurrentTick() {
        return chatTimer;
    }

    /** 获取当前对话中的成员 */
    public List<Villager> getMembers() { return members; }

    public Map<String, Villager> getMemberMap() {
        return memberMap;
    }

    /** 获取当前对话是否正在活动 */
    public boolean isActive() { return active; }
    /** 获取当前对话是否已经结束 */
    public boolean isFinished() { return finished; }

    /**
     * 同一个对话过程中所有成员共用的上下文
     * 应当有组长创建并分配，最后各自销毁
     * @param script 使用的剧本
     */
    public ChatContext(List<Villager> members, ChatScript script) {
        this.members = members;
        this.script = script;

        Map<String, Villager> memberMap = new HashMap<>();

        // 根据剧本分配角色
        int i = 0;
        for (String id : script.uniqueIds()) {
            memberMap.put(id, members.get(i));
            i++;
        }

        this.memberMap = memberMap;
    }
}

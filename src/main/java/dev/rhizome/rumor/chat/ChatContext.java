package dev.rhizome.rumor.chat;

import dev.rhizome.rumor.ai.memory.RumorMemoryTypes;
import dev.rhizome.rumor.chat.script.ChatScript;
import dev.rhizome.rumor.chat.script.Step;
import dev.rhizome.rumor.RumorConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;

import java.util.*;

public class ChatContext {

    public enum Status {
        RECRUITING,
        GATHERING,
        ACTIVE,
        FINISHED,
    }

    private final ServerLevel level;

    private final List<Villager> members; // 聊天人员列表
    private Map<String, Villager> memberMap; // 人员与剧本内id对应关系，用于分配角色
    private final Map<Villager, Component> originalNames= new HashMap<>(); // 记录村民原本的名称，用于恢复
    private final ChatScript script; // 聊天所使用的剧本

    private Status status = Status.RECRUITING;

    // 步骤计数和计时
    private int currentStepIndex = 0;
    private int stepTicks = 0;

    private Villager currentSpeaker = null;
    private final Map<Villager, LookAtOverride> lookAtOverrides = new HashMap<>();
    private BlockPos centerPos;

    public ServerLevel getLevel() {return level;}

    public List<Villager> getMembers() { return members; }

    public Map<String, Villager> getMemberMap() {
        return memberMap;
    }

    public BlockPos getCenterPos () {return centerPos;}

    public Status getStatus () {return status;}

    public void start() {
        status = Status.ACTIVE;
    }

    /**
     * 负责更新上下文状态
     * 应当每tick执行一次 */
    public void tick() {
        if (status.equals(Status.RECRUITING)) recruit();

        if (status.equals(Status.GATHERING)) checkReady();

        if (!status.equals(Status.ACTIVE)) return;

        // 获取当前步骤
        Step step = script.steps().get(currentStepIndex);

        // 在step开始时，由各个action更新上下文状态
        if (stepTicks == 0) {
            step.exec(this);
        }

        stepTicks++;

        // 下面是每tick需要维护与更新的上下文状态

        // 遍历lookAt覆盖，管理计时器，并移除过期的
        lookAtOverrides.entrySet().removeIf(e -> {
            e.getValue().decrement();
            return e.getValue().isExpired();
        });

        // 检测step与上下文的结束
        if (stepTicks >= step.durationTicks()) {

            stepTicks = 0;
            currentStepIndex++;

            if (currentStepIndex >= script.steps().size()) {
                restoreNames();
                status = Status.FINISHED;
            }
        }
    }

    /** 判断成员是否搜存活且存在 */
    public boolean isValid () {
        return members.stream().allMatch(v ->
                v != null && v.isAlive() && !v.isRemoved());
    }

    /** 判断所有成员已聚集后开始对话 */
    public void checkReady() {
        boolean ready = members.stream().allMatch(m ->
                m.blockPosition().distSqr(centerPos) <= 3.0D * 3.0D);
        if (ready) {
            members.forEach(m -> m.setGlowingTag(false));
            start();
        }
    }

    /**
     * 在action中调用，设置当前说话者
     * @param speaker 当前说话者
     */
    public void setCurrentSpeaker(Villager speaker) {currentSpeaker = speaker;}

    public Villager getCurrentSpeaker() {return currentSpeaker;}

    /**
     * 向一定范围内玩家的Action Bar发送内容
     * @param speakerId 发送者id
     * @param msg 消息内容
     * @param range 能听到的范围
     */
    public void broadcastActionBar (String speakerId, String msg, double range) {
        Component text = Component.literal("["+script.getRoleName(speakerId)+"]"+msg);

        level.players().stream()
                .filter(p -> p.blockPosition().distSqr(centerPos) <= range * range)
                .forEach(p -> p.sendSystemMessage(text,true));
    }

    /**
     * 获取当前村民看向的目标
     * @param v 需要查询看向的村民
     */
    public Villager getLookAt(Villager v) {
        // 如果有强制覆盖，就返回强制覆盖
        if (lookAtOverrides.containsKey(v)) return lookAtOverrides.get(v).getTarget();

        // 如果有人在说话且不是自己，看他
        if (currentSpeaker != null && currentSpeaker != v) {
            return currentSpeaker;
        }

        // 如果组长不是自己，看组长
        Villager leader = members.get(0);
        return (leader == v) ? null: leader;
    }

    /** 恢复原名，如果原名为空，设置为隐藏 */
    public void restoreNames() {
        originalNames.forEach((villager, originalName) -> {
            if (villager.isAlive() && !villager.isRemoved()) {
                villager.setCustomName(originalName);
                villager.setCustomNameVisible(originalName != null);
            }
        });
    }

    /** 招募成员，每一个tick会查找附近空闲村民 */
    private void recruit () {
        if (status != Status.RECRUITING) return;

        int required = script.getRoleIds().size() - members.size();

        // 人数足够，启动！
        if (required <= 0) {
            initChat();
        }

        List<Villager> candidates = level.getEntitiesOfClass(Villager.class,
                members.get(0).getBoundingBox().inflate(script.searchRange()),
                this::isAvailable);

        // 招募满了，启动！
        if (candidates.size() >= required) {
            members.addAll(candidates.subList(0,required));
            initChat();
        }
    }

    /** 初始化对话，计算相关信息 */
    private void initChat() {
        if (status != Status.RECRUITING) return;

        Map<String, Villager> memberMap = new HashMap<>();

        List<String> roleIds = new ArrayList<>(script.getRoleIds());

        // 根据剧本分配角色
        for (int i = 0; i < members.size(); i++) {

            String roleId = roleIds.get(i);
            Villager member = members.get(i);

            memberMap.put(roleId, member);

            if (member.hasCustomName()) {
                originalNames.put(member, member.getCustomName());
            } else {
                // 如果没有自定义名字
                originalNames.put(member, null);
            }

            member.getBrain().setMemory(RumorMemoryTypes.CHAT_CONTEXT.get(), this);

            member.setCustomName(Component.literal(script.getRoleName(roleId)));
            member.setCustomNameVisible(true);
        }

        this.memberMap = memberMap;

        // 计算对话中心点
        double x = members.stream().mapToDouble(Villager::getX).average().orElse(0);
        double z = members.stream().mapToDouble(Villager::getZ).average().orElse(0);
        this.centerPos = new BlockPos((int)x, (int)members.get(0).getY(), (int)z);

        status = Status.GATHERING;
    }

    /** 确认村民可以加入新对话 */
    private boolean isAvailable (Villager v) {

        if (members.contains(v) || !v.isAlive() || v.isRemoved() || v.isBaby()) return false;

        if (v.getBrain().getMemory(RumorMemoryTypes.CHAT_CONTEXT.get()).isPresent()) return false;

        Optional<Long> lastChatTime = v.getBrain().getMemory(RumorMemoryTypes.LAST_CHAT_TIME.get());
        return lastChatTime.isEmpty() ||
                level.getGameTime() - lastChatTime.get() > RumorConfig.COOLDOWN.get() * 20;
    }

    /**
     * 同一个对话过程中所有成员共用的上下文
     * @param level 当前世界
     * @param members 对话成员
     * @param script 使用的剧本
     */
    public ChatContext(ServerLevel level, List<Villager> members, ChatScript script) {
        this.level = level;
        this.members = members;
        this.script = script;
    }

    // 剧本手动设置视线方向
    // 覆盖默认行为
    private static class LookAtOverride {
        private final Villager target;
        private int remainingTicks;

        public void decrement() {remainingTicks--;}
        public boolean isExpired() {return remainingTicks<=0;}
        public Villager getTarget() {return target;}

        /**
         * 剧本手动设置视线方向
         * 覆盖默认行为
         * @param target 看向的目标
         * @param duration 持续时长
         */
        public LookAtOverride(Villager target, int duration) {
            this.target = target;
            this.remainingTicks = duration;
        }
    }
}

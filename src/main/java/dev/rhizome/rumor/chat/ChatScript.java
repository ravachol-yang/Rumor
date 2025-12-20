package dev.rhizome.rumor.chat;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 对话剧本，主要一个步骤列表
 * 自动计算相关信息
 * @param steps 步骤列表
 * @param uniqueIds 不重复的角色列表，用于判断所需人数和分配角色
 * @param totalDuration 总时长，用于判断何时结束
 */
public record ChatScript(List<ChatScriptStep> steps,
                         Set<String> uniqueIds,
                         int totalDuration) {

    /**
     * 构建从剧本步骤的列表一个剧本
     * 其他参数都是自动计算的
     * @param steps 剧本步骤的列表
     */
    public ChatScript (List<ChatScriptStep> steps){
        this(new java.util.ArrayList<>(steps),null, 0);
    }

    // 进行初始化，计算其他参数
    public ChatScript {
        // 根据tick序列进行排序
        // 不重要但是也许真的会有人把顺序乱排()
        // 还是重新加工一下吧
        steps.sort(Comparator.comparingInt(ChatScriptStep::tickNode));

        // 计算不重复的角色数量
        if (uniqueIds == null) {
            uniqueIds = steps.stream()
                    .map(ChatScriptStep::speakerId)
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        }

        // 计算总时长，加一点点缓冲
        if (!steps.isEmpty() && totalDuration <= 0) {
            totalDuration = steps.get(steps.size() - 1).tickNode() + 30;
        }
    }
}

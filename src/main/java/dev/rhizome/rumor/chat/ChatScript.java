package dev.rhizome.rumor.chat;

import dev.rhizome.rumor.config.RumorConfig;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 对话剧本，主要一个步骤列表
 * 自动计算相关信息
 * @param id 剧本id
 * @param steps 步骤列表
 * @param weight 随机选择剧本时的权重
 * @param searchRange 搜索成员的范围 (输入null为默认)
 * @param uniqueIds 不重复的角色列表，用于判断所需人数和分配角色 (输入null自动计算)
 * @param totalTicks 总时长，用于判断何时结束 (输入null自动计算)
 */
public record ChatScript(String id,
                         List<ChatScriptStep> steps,
                         int weight,
                         Double searchRange,
                         Set<String> uniqueIds,
                         int totalTicks) {

    /**
     * 构建从剧本步骤的列表一个剧本
     * 其他参数都是自动计算的
     * @param id 剧本id
     * @param steps 步骤列表
     * @param weight 随机选择剧本时的权重
     * @param searchRange 搜索成员的范围, 输入null为默认
     */
    public ChatScript (String id,
                       List<ChatScriptStep> steps,
                       int weight,
                       Double searchRange){

        this(id,new java.util.ArrayList<>(steps),weight,searchRange,null, 0);
    }

    // 进行初始化，计算其他参数
    public ChatScript {

        // 可选参数，标记为使用默认
        if (searchRange == null || searchRange <= 0) {
            searchRange = RumorConfig.DEFAULT_SEARCH_RANGE.get();
        }

        // 根据tick序列进行排序
        // 不重要但是也许真的会有人把顺序乱排()
        // 还是重新加工一下吧
        steps.sort(Comparator.comparingInt(ChatScriptStep::triggerTick));

        // 计算不重复的角色数量
        if (uniqueIds == null) {
            uniqueIds = steps.stream()
                    .map(ChatScriptStep::speakerId)
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        }

        // 计算总时长，加一点点缓冲
        if (!steps.isEmpty() && totalTicks <= 0) {
            totalTicks = steps.get(steps.size() - 1).triggerTick() + 30;
        }
    }
}

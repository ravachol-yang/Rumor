package dev.rhizome.rumor.chat.script;

import dev.rhizome.rumor.config.RumorConfig;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 对话剧本，主要一个步骤列表
 * 自动计算相关信息
 * @param id 剧本id
 * @param steps 步骤列表
 * @param stepMap tick与多个步骤的映射
 * @param weight 随机选择剧本时的权重
 * @param searchRange 搜索成员的范围 (输入null为默认)
 * @param uniqueIds 不重复的角色列表，用于判断所需人数和分配角色 (输入null自动计算)
 * @param totalTicks 总时长，用于判断何时结束 (输入null自动计算)
 */
public record ChatScript(String id,
                         List<IScriptStep> steps,
                         Map<Integer, List<IScriptStep>> stepMap,
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
                       List<IScriptStep> steps,
                       int weight,
                       Double searchRange){

        this(id,new java.util.ArrayList<>(steps),null,weight,searchRange,null, 0);
    }

    // 进行初始化，计算其他参数
    public ChatScript {

        // 可选参数，标记为使用默认
        if (searchRange == null || searchRange <= 0) {
            searchRange = RumorConfig.DEFAULT_SEARCH_RANGE.get();
        }

        // 把tick和其对应的多个步骤分组
        if (stepMap == null) {
            stepMap = steps.stream()
                    .collect(Collectors.groupingBy(IScriptStep::getTriggerTick));
        }

        // 计算不重复的角色数量
        if (uniqueIds == null) {
            uniqueIds = steps.stream()
                    .map(IScriptStep::getActorId)
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        }

        // 计算总时长，加一点点缓冲
        if (!steps.isEmpty() && totalTicks <= 0) {
            totalTicks = stepMap.keySet().stream()
                    .max(Integer::compare).orElse(0)
            + 20;
        }
    }
}

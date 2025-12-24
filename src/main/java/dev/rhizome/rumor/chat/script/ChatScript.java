package dev.rhizome.rumor.chat.script;

import dev.rhizome.rumor.RumorConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对话剧本，主要一个步骤列表
 * 自动计算相关信息
 * @param location 剧本id，由数据包加载器自动获取
 * @param roles 剧本中的角色id与角色名称对应关系
 * @param steps 步骤列表
 * @param weight 随机选择剧本时的权重
 * @param searchRange 搜索成员的范围 (输入null为默认)
 */
public record ChatScript(ResourceLocation location,
                         Map<String,String> roles,
                         List<Step> steps,
                         int weight,
                         Double searchRange) {

    public Set<String> getRoleIds() { return roles.keySet(); }
    public String getRoleName(String id) { return roles.getOrDefault(id, id); }

    // 重新创建一份带id的
    public ChatScript withLocation(ResourceLocation location) {
        return new ChatScript(location,roles, steps,weight, searchRange);
    }

    // 进行初始化，计算其他参数
    public ChatScript {

        // 可选参数，标记为使用默认
        if (searchRange == null || searchRange <= 0) {
            searchRange = RumorConfig.DEFAULT_SEARCH_RANGE.get();
        }
    }
}

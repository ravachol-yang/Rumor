package dev.rhizome.rumor;

import net.minecraftforge.common.ForgeConfigSpec;

public class RumorConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // 配置项
    public static final ForgeConfigSpec.ConfigValue<Integer> ELECTION_INTERVAL;
    public static final ForgeConfigSpec.ConfigValue<Integer> COOLDOWN;
    public static final ForgeConfigSpec.ConfigValue<Double> DEFAULT_SEARCH_RANGE;
    public static final ForgeConfigSpec.ConfigValue<Double> DEFAULT_BROADCAST_RANGE;

    static {

        ELECTION_INTERVAL = BUILDER.comment("村民可随机被选为搜索状态的间隔, 单位: 秒")
                .defineInRange("electionInterval", 30, 5, 600);

        COOLDOWN = BUILDER.comment("冷却时间, 单位: 秒")
                .defineInRange("cooldown", 300, 60, 1200);

        DEFAULT_SEARCH_RANGE = BUILDER.comment("搜索对话成员时默认范围")
                .defineInRange("defaultSearchRange", 10.0, 2.0, 64.0);

        DEFAULT_BROADCAST_RANGE = BUILDER.comment("广播消息时默认范围")
                .defineInRange("defaultBroadcastRange", 8.0, 2.0, 64.0);

        // 初始化配置
        SPEC = BUILDER.build();
    }
}

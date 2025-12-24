package dev.rhizome.rumor.chat.script;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import dev.rhizome.rumor.util.gson.RuntimeTypeAdapterFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatScriptManager  extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();

    // gson默认不支持多态，需要使用gson-extras中的内容
    private static final RuntimeTypeAdapterFactory<IAction> STEP_ADAPTER =
            RuntimeTypeAdapterFactory.of(IAction.class, "type")
                    .registerSubtype(MessageAction.class, "message")
                    .registerSubtype(LookAtAction.class,"look_at")
                    .registerSubtype(ExpAction.class,"exp")
                    .registerSubtype(AnimationAction.class, "animation")
                    .registerSubtype(JumpAction.class, "jump");

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(STEP_ADAPTER)
            .create(); // 使用Gson, 官方自带的方案

    private static final List<ChatScript> REGISTERED_SCRIPTS = new ArrayList<>(); // 剧本注册到一个列表中

    public ChatScriptManager() {
        // 数据包的路径
        super(GSON, "scripts");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        LOGGER.info("loading scripts, found {} files", pObject.size());
        REGISTERED_SCRIPTS.clear();
        pObject.forEach((location, json) -> {
            try {
                ChatScript rawScript = GSON.fromJson(json, ChatScript.class);
                ChatScript script = rawScript.withLocation(location);
                REGISTERED_SCRIPTS.add(script);
                LOGGER.info("registered script: {}", script.location());
            } catch (Exception e) {
                LOGGER.error("failed processing json file: {}", location, e);
            }
        });
        LOGGER.info("Loaded {} scripts", REGISTERED_SCRIPTS.size());
    }

    /** 获取已经注册的剧本列表 */
    public static Iterable<ResourceLocation> getRegisteredIds() {
        return REGISTERED_SCRIPTS.stream()
                .map(ChatScript::location)
                .toList();
    }

    /** 根据 id 查找剧本 */
    public static ChatScript getScript(ResourceLocation location) {
        return REGISTERED_SCRIPTS.stream()
                .filter(s -> s.location().equals(location))
                .findFirst()
                .orElse(null);
    }

    /**
     * 从已经注册的剧本列表里随机选取一个
     * @param random 随机
     */
    public static ChatScript getRandomScript(RandomSource random) {
        if (REGISTERED_SCRIPTS.isEmpty()) return null;

        int totalWeight = REGISTERED_SCRIPTS.stream().mapToInt(ChatScript::weight).sum();
        if (totalWeight <= 0) return REGISTERED_SCRIPTS.get(0);

        int r = random.nextInt(totalWeight);
        int current = 0;
        for (ChatScript s : REGISTERED_SCRIPTS) {
            current += s.weight();
            if (r < current) return s;
        }
        return REGISTERED_SCRIPTS.get(0);
    }
}

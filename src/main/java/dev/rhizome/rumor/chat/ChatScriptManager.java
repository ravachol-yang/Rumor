package dev.rhizome.rumor.chat;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
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

    private static final Gson GSON = new Gson(); // 使用Gson, 官方自带的方案
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
                ChatScript script = GSON.fromJson(json, ChatScript.class);
                REGISTERED_SCRIPTS.add(script);
            } catch (Exception e) {
                LOGGER.error("failed processing json file: {}", location, e);
            }
        });
        LOGGER.info("Loaded {} scripts", REGISTERED_SCRIPTS.size());
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

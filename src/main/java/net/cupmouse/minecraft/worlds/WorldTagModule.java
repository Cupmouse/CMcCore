package net.cupmouse.minecraft.worlds;

import net.cupmouse.minecraft.CMcCore;
import net.cupmouse.minecraft.PluginModule;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class WorldTagModule implements PluginModule {

    private static Map<UUID, WorldTag> uuidTagMap = new HashMap<>();
    private static Map<WorldTag, World> tagWorldMap = new HashMap<>();

    @Override
    public void onInitializationProxy() {
        Sponge.getEventManager().registerListeners(CMcCore.getPlugin(), this);

        CommentedConfigurationNode nodeWorlds = CMcCore.getCommonConfigNode().getNode("worlds");
        Map<Object, ? extends CommentedConfigurationNode> childrenMap = nodeWorlds.getChildrenMap();

        for (Map.Entry<Object, ? extends CommentedConfigurationNode> entry : childrenMap.entrySet()) {
            CommentedConfigurationNode nodeUUID = entry.getValue().getNode("uuid");
            UUID uuid = UUID.fromString(nodeUUID.getString());

            uuidTagMap.put(uuid, WorldTag.byName((String) entry.getKey()));
        }
    }

    @Override
    public void onServerStartingProxy() {


        // ワールド関連付けのチェック
        if (tagWorldMap.size() != uuidTagMap.size()) {
            CMcCore.getLogger().warn(
                    "読み込まれていないワールドがあります！");
        }
    }

    @Listener
    public void onWorldLoad(LoadWorldEvent event) {
        World targetWorld = event.getTargetWorld();

        UUID uniqueId = targetWorld.getUniqueId();

        if (uuidTagMap.containsKey(uniqueId)) {
            WorldTag worldTag = uuidTagMap.get(uniqueId);
            tagWorldMap.put(worldTag, targetWorld);

            CMcCore.getLogger().info(
                    "ワールドが読み込まれ、次のように関連付けられました　" + uniqueId + " / " + worldTag);
        } else {
            CMcCore.getLogger().warn("予期されていないワールドがロードされます UUID : " + uniqueId);
        }
    }

    public static Optional<WorldTag> whatIsThisWorld(World world) {
        return Optional.ofNullable(uuidTagMap.get(world.getUniqueId()));
    }

    /**
     * タグとワールドを指定して、指定されたワールドインスタンスが、タグと一致するかを返す。
     * つまり、指定されたワールドインスタンスが目的のものかチェックする。
     *
     * @param tag
     * @param world
     * @return
     */
    public static boolean isThis(WorldTag tag, World world) {
        World worldTagged = tagWorldMap.get(tag);

        return worldTagged != null && worldTagged == world;
    }

    /**
     * 事前に予期しているワールドのタグを使ってワールドのインスタンスを取得する
     *
     * @param tag
     * @return
     */
    public static Optional<World> getTaggedWorld(WorldTag tag) {
        return Optional.ofNullable(tagWorldMap.get(tag));
    }

}

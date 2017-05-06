package net.cupmouse.minecraft.worlds;

import net.cupmouse.minecraft.CMcCore;
import net.cupmouse.minecraft.PluginModule;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.GameModeData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class WorldsModule implements PluginModule {

    private Path configWorlds;
    private HoconConfigurationLoader configLoader;

    private static Map<Tag, World> WorldMap = new HashMap<>();
    private static Map<UUID, Tag> UuidTagMap = new HashMap<>();

    @Override
    public void onPreInitializationProxy() {
        this.configWorlds = CMcCore.getConfigDir().resolve("worlds.conf");

    }

    @Override
    public void onInitializationProxy() {
        Sponge.getEventManager().registerListeners(CMcCore.getPlugin(), this);

        // ここから設定読み込み

        CommentedConfigurationNode nodeWorlds = CMcCore.getCommonConfigNode().getNode("worlds");
        Map<Object, ? extends CommentedConfigurationNode> childrenMap = nodeWorlds.getChildrenMap();

        for (Map.Entry<Object, ? extends CommentedConfigurationNode> entry : childrenMap.entrySet()) {
            CommentedConfigurationNode nodeUUID = entry.getValue().getNode("uuid");
            UUID uuid = UUID.fromString(nodeUUID.getString());
            UuidTagMap.put(uuid, Tag.valueOf(((String) entry.getKey()).toUpperCase()));
        }

        CommandSpec flyCommand = CommandSpec.builder()
                .description(Text.of("スペクテイターモードとサバイバルモードを切り替えます。"))
                .permission("cmc.spectorinmainworld")
                .executor((src, args) -> {
                    if (!(src instanceof Player)) {
                        src.sendMessage(Text.of("プレイヤーが実行するコマンドです"));
                    }

                    Player player = (Player) src;
                    GameModeData gameModeData = player.getGameModeData();
                    Optional<GameMode> gameMode = gameModeData.get(Keys.GAME_MODE);

                    if (gameMode.get() == GameModes.SURVIVAL) {
                        player.offer(Keys.GAME_MODE, GameModes.SPECTATOR);

                    } else if (gameMode.get() == GameModes.SPECTATOR) {
                        player.offer(Keys.GAME_MODE, GameModes.SURVIVAL);
                    }


                    return CommandResult.success();
                }).build();
        Sponge.getCommandManager().register(CMcCore.getPlugin(), flyCommand, "fly");
    }

    @Override
    public void onServerStartingProxy() {
        if (Tag.values().length != UuidTagMap.size()) {
            CMcCore.getLogger().warn(
                    "読み込まれていないワールドがあるか、読み込まれたが予期されていないワールドがあります！");
        }
    }

    @Listener
    public void onWorldLoad(LoadWorldEvent event) {
        World targetWorld = event.getTargetWorld();

        UUID uniqueId = targetWorld.getUniqueId();

        if (UuidTagMap.containsKey(uniqueId)) {
            Tag tag = UuidTagMap.get(uniqueId);
            WorldMap.put(tag, targetWorld);

            CMcCore.getLogger().info(
                    "ワールドが読み込まれ、次のように関連付けられました　" + uniqueId + " / " + tag);
        } else {
            CMcCore.getLogger().warn("予期されていないワールドがロードされます UUID : " + uniqueId);
        }
    }

    public static Optional<Tag> whatIsThisWorld(World world) {
        return Optional.ofNullable(UuidTagMap.get(world.getUniqueId()));
    }

    /**
     * タグとワールドを指定して、指定されたワールドインスタンスが、タグと一致するかを返す。
     * つまり、指定されたワールドインスタンスが目的のものかチェックする。
     *
     * @param tag
     * @param world
     * @return
     */
    public static boolean isThis(Tag tag, World world) {
        World worldTagged = WorldMap.get(tag);

        return worldTagged != null && world == world;
    }

    /**
     * 事前に予期しているワールドのタグを使ってワールドのインスタンスを取得する
     *
     * @param tag
     * @return
     */
    public static Optional<World> getTaggedWorld(Tag tag) {
        return Optional.ofNullable(WorldMap.get(tag));
    }

    @Listener
    public void onPlayerDamagedInMainWorld(DamageEntityEvent event) {
        // メインワールドで受けたダメージは0！

        Entity targetEntity = event.getTargetEntity();

        if (!(targetEntity instanceof Player)) {
            return;
        }

        if (!isThis(Tag.MAIN, targetEntity.getLocation().getExtent())) {
            return;
        }

        // ダメージをなかったコトにするが、ダメージを受けたアニメーションは出る
        event.setBaseDamage(0);
    }

    @Listener
    public void onPlayerTeleportedToAnotherWorld(MoveEntityEvent.Teleport event) {
        // テレポート元がメインでテレポート先がメインでない時、スペクターモードが継続しないようにする

        if (!(event.getTargetEntity() instanceof Player)) {
            return;
        }

        World fromWorld = event.getFromTransform().getExtent();
        World toWorld = event.getToTransform().getExtent();

        if (isThis(Tag.MAIN, fromWorld) && fromWorld != toWorld) {
            // プレイヤーのゲームモードをセットする
            ((Player) event.getTargetEntity()).offer(Keys.GAME_MODE, GameModes.SURVIVAL);
        }
    }

    public enum Tag {
        MAIN,
        RESOURCE_DEFAULT,
        RESOURCE_NETHER,
        RESOURCE_THE_END
    }
}

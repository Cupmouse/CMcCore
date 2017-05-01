package net.cupmouse.minecraft;

import com.google.inject.Inject;
import net.cupmouse.minecraft.beam.BeamModule;
import net.cupmouse.minecraft.data.user.UserDataModule;
import net.cupmouse.minecraft.db.DatabaseModule;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.TickBlockEvent;
import org.spongepowered.api.event.game.state.*;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.server.ClientPingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

@Plugin(id = "cmcplugin", name = "CMcPlugin", version = "alpha-0.1", description = "CMc Minecraft server plugin",
        authors = "Cupmouse", url = "www.cupmouse.net")
public class CMcPlugin {

    private final Game game;
    private final Logger logger;
    private final Path configDir;
    private final Path configCommon;
    private CommentedConfigurationNode commonConfigNode;
    private HoconConfigurationLoader commonConfigLoader;

    private final List<PluginModule> modules;
    private final DatabaseModule dbm;
    private final UserDataModule userm;
    private final BeamModule rs;

    @Inject
    public CMcPlugin(Game game, Logger logger, @ConfigDir(sharedRoot = false) Path configDir) {
        this.game = game;
        this.logger = logger;
        this.configDir = configDir;
        this.configCommon = configDir.resolve("common.conf");

        PluginModule[] moduleArray = {
                this.dbm = new DatabaseModule(this),
                new HeartbeatModule(this),
                new PongPingModule(this),
                this.userm = new UserDataModule(this),
                this.rs = new BeamModule(this)
        };

        this.modules = Collections.unmodifiableList(Arrays.asList(moduleArray));
    }

    public Game getGame() {
        return game;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getConfigDir() {
        return configDir;
    }

    public CommentedConfigurationNode getCommonConfigNode() {
        return commonConfigNode;
    }

    public DatabaseModule getDbm() {
        return dbm;
    }

    public UserDataModule getUserm() {
        return userm;
    }

    public void stopEternally() {
        // スレッドを永遠とスリープさせる。致命的なエラーが有った場合に呼ぶ。

        while (true) {
            try {
                Thread.sleep(2^31);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // TODO 適切な通知を行う
        }
    }

    // Handling initialization state events

    @Listener
    public void onPreInitialization(GamePreInitializationEvent event) {
        logger.debug("PreInit");
        // logger onInitializationProxy

        // サーバーの設定が正常がチェックする
        // TODO このチェックアホくさい
        if (!TimeZone.getDefault().getID().equals(TimeZone.getTimeZone("Asia/Tokyo").getID())) {
            // ローカルの時間地域設定がおかしい
            logger.error("時間地域を[Asia/Tokyo]に設定してください");
            stopEternally();
        }

        try {
            for (PluginModule module : modules) {
                module.onPreInitializationProxy();
            }
        } catch (Exception e) {
            e.printStackTrace();
            stopEternally();
        }
    }

    @Listener
    public void onInitialization(GameInitializationEvent event) {
        logger.debug("Init");
        // plugin fuc onInitializationProxy

        // 設定の読み込み

        // 設定ファイルが存在しない場合、jarファイル内のアセットフォルダからコピーする。
        if (!Files.exists(configCommon)) {
            try {
                Files.createDirectories(configDir);
                Sponge.getAssetManager().getAsset(this, "common.conf").get().copyToFile(configCommon);
            } catch (IOException e) {
                e.printStackTrace();
                stopEternally();
            }
        }

        // 設定をロードする
        this.commonConfigLoader = HoconConfigurationLoader.builder().setPath(configCommon).build();

        try {
            this.commonConfigNode = commonConfigLoader.load();
        } catch (IOException e) {
            // 設定が読み込まれないのは致命的だから、管理人が調査できるまでサーバーをストップする

            e.printStackTrace();
            stopEternally();
        }

        logger.info("設定を読み込みました！");

        try {
            for (PluginModule module : modules) {
                module.onInitializationProxy();
            }
        } catch (Exception e) {
            e.printStackTrace();
            stopEternally();
        }
    }

    // Handling running state event

    @Listener
    public void onAboutToStartServer(GameAboutToStartServerEvent event) {
        logger.debug("ServerStarting");
        // onInitializationProxy for server (worlds are not loaded yet)

        for (PluginModule module : modules) {
            module.onAboutToStartServerProxy();
        }
    }

    @Listener
    public void onStartedServer(GameStartedServerEvent event) {
        logger.debug("ServerStarted");
        // onInitializationProxy for worlds
    }

    @Listener
    public void onStoppingServer(GameStoppingServerEvent event) {
        logger.debug("ServerStopping");
        // deconstruct before worlds will be unloaded
    }

    @Listener
    public void onStoppedServer(GameStoppedServerEvent event) {
        logger.debug("ServerStopped");
        // no players are connected. world are saved.

        // モジュールを停止

        try {
            // 逆順で停止
            for (int i = modules.size() - 1; i >= 0; i--) {
                modules.get(i).onStoppedServerProxy();
            }
        } catch (Exception e) {
            e.printStackTrace();
            stopEternally();
        }

        try {
            commonConfigLoader.save(commonConfigNode);
        } catch (IOException e) {
            e.printStackTrace();
            logger.warn("設定が保存できませんでした。[共通設定]");
        }
    }

    // Handling stopping state event (may not be called if server has stopped via force quitting or something)

    @Listener
    public void onStopped(GameStoppedEvent event) {
        // called immediate before closing java
        logger.debug("GameStopped");
    }

    @Listener(order = Order.DEFAULT)
    public void onLogin(ClientConnectionEvent.Join event) {
        Player targetEntity = event.getTargetEntity();

//        DataTransactionResult cupmouse = targetEntity.offer(Keys.DISPLAY_NAME, Text.of("Cupmouse"));
//        logger.info(cupmouse.toString());
//        logger.info(targetEntity.get(Keys.DISPLAY_NAME).get().toString());
    }
}

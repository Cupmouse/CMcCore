package net.cupmouse.minecraft;

import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.*;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.SpongeExecutorService;

import java.io.IOException;
import java.nio.file.Path;

@Plugin(id = "cmcplugin", name = "CMcPlugin", version = "alpha-0.1", description = "CMc Minecraft server plugin",
        authors = "Cupmouse", url = "www.cupmouse.net")
public class CMcPlugin {

    private final Game game;
    private final Logger logger;
    private final Path configDir;
    private final Path configCommon;
    private CommentedConfigurationNode commonConfigNode;
    private HoconConfigurationLoader commonConfigLoader;

    @Inject
    public CMcPlugin(Game game, Logger logger, @ConfigDir(sharedRoot = false) Path configDir) {
        this.game = game;
        this.logger = logger;
        this.configDir = configDir;
        this.configCommon = configDir.resolve("common.conf");
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
        // logger init
    }

    @Listener
    public void onInitialization(GameInitializationEvent event) {
        logger.debug("Init");
        // plugin fuc init

        // 設定の読み込み
        this.commonConfigLoader = HoconConfigurationLoader.builder().setPath(configCommon).build();

        // 設定をロードする、ファイルがなければ勝手に作られるので考えなくて良い

        try {
            this.commonConfigNode = commonConfigLoader.load();
        } catch (IOException e) {
            // 設定が読み込まれないのは致命的だから、管理人が調査できるまでサーバーをストップする

            e.printStackTrace();
            stopEternally();
        }


    }

    // Handling running state event

    @Listener
    public void onAboutToStartServer(GameAboutToStartServerEvent event) {
        logger.debug("ServerStarting");
        // init for server (worlds are not loaded yet)
    }

    @Listener
    public void onStartedServer(GameStartedServerEvent event) {
        logger.debug("ServerStarted");
        // init for worlds
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
    }

    // Handling stopping state event (may not be called if server has stopped via force quitting or something)

    @Listener
    public void onStopped(GameStoppedEvent event) {
        // called immediate before closing java
        logger.debug("GameStopped");

        try {
            commonConfigLoader.save(commonConfigNode);
        } catch (IOException e) {
            e.printStackTrace();
            logger.warn("設定が保存できませんでした。[共通設定]");
        }
    }
}

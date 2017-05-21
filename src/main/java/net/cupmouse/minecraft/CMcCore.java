package net.cupmouse.minecraft;

import net.cupmouse.minecraft.data.user.UserDataModule;
import net.cupmouse.minecraft.db.DatabaseModule;
import net.cupmouse.minecraft.util.ModuleNotLoadedException;
import net.cupmouse.minecraft.worlds.WorldTag;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.*;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CMcCore {

    private static Object plugin;
    private static Logger logger;
    private static Path configDir;
    private final Path configCommon;
    private static CommentedConfigurationNode commonConfigNode;
    private HoconConfigurationLoader commonConfigLoader;

    private final List<PluginModule> modules;
    private static UserDataModule userm;
    private static DatabaseModule dbm;

    public CMcCore(Object plugin, Logger logger, Path configDir, PluginModule[] moduleArray) {
        CMcCore.plugin = plugin;
        CMcCore.logger = logger;
        CMcCore.configDir = configDir;
        this.configCommon = configDir.resolve("common.conf");

        this.modules = Collections.unmodifiableList(Arrays.asList(moduleArray));

        for (PluginModule pluginModule : moduleArray) {
            if (pluginModule instanceof DatabaseModule) {
                CMcCore.dbm = (DatabaseModule) pluginModule;
            } else if (pluginModule instanceof UserDataModule) {
                CMcCore.userm = ((UserDataModule) pluginModule);
            }
        }

        Sponge.getEventManager().registerListeners(plugin, this);
    }

    public static Logger getLogger() {
        return logger;
    }

    public static Object getPlugin() {
        return plugin;
    }

    public static DatabaseModule getDbm() {
        if (dbm == null) {
            throw new ModuleNotLoadedException();
        }
        return dbm;
    }

    public static UserDataModule getUserm() {
        if (userm == null) {
            throw new ModuleNotLoadedException();
        }
        return userm;
    }

    public static Path getConfigDir() {
        return configDir;
    }

    public static CommentedConfigurationNode getCommonConfigNode() {
        return commonConfigNode;
    }

    public static void stopEternally() {
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
        // logger onPreInitializationProxy

        // サーバーの設定が正常がチェックする
        // TODO このチェックアホくさい
        if (!TimeZone.getDefault().getID().equals(TimeZone.getTimeZone("Asia/Tokyo").getID())) {
            // ローカルの時間地域設定がおかしい
            logger.error("時間地域を[Asia/Tokyo]に設定してください");
            stopEternally();
        }

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
            commonConfigNode = commonConfigLoader.load();
        } catch (IOException e) {
            // 設定が読み込まれないのは致命的だから、管理人が調査できるまでサーバーをストップする

            e.printStackTrace();
            stopEternally();
        }

        logger.info("共通設定を読み込みました！");

        try {
            for (PluginModule module : modules) {
                logger.info("前初期化/" + module.getClass().getCanonicalName());
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
        try {
            for (PluginModule module : modules) {
                logger.info("初期化/" + module.getClass().getCanonicalName());
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

        try {
            for (PluginModule module : modules) {
                logger.info("サーバー準備中/" + module.getClass().getCanonicalName());
                module.onAboutToStartServerProxy();
            }
        } catch (Exception e) {
            e.printStackTrace();
            stopEternally();
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
                logger.info("サーバー終了/" + modules.get(i).getClass().getCanonicalName());
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

    // ここから普通

    @Listener(order = Order.DEFAULT)
    public void onLogin(ClientConnectionEvent.Join event) {
        Player targetEntity = event.getTargetEntity();

//        DataTransactionResult cupmouse = targetEntity.offer(Keys.DISPLAY_NAME, Text.of("Cupmouse"));
//        logger.info(cupmouse.toString());
//        logger.info(targetEntity.get(Keys.DISPLAY_NAME).get().toString());
    }
}

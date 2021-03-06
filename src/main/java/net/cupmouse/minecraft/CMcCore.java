package net.cupmouse.minecraft;

import net.cupmouse.minecraft.cmd.CmdWorldTeleport;
import net.cupmouse.minecraft.data.user.UserDataModule;
import net.cupmouse.minecraft.db.DatabaseModule;
import net.cupmouse.minecraft.util.ModuleNotLoadedException;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.*;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.PluginContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class CMcCore {

    private static Object plugin;
    private static PluginContainer pluginContainer;
    private static Logger logger;
    private static Path configDir;
    private final Path configCommon;
    private static CommentedConfigurationNode commonConfigNode;
    private HoconConfigurationLoader commonConfigLoader;

    private final List<PluginModule> modules;
    private static UserDataModule userm;
    private static DatabaseModule dbm;

    public CMcCore(Object plugin, PluginContainer pluginContainer,
                   Logger logger, Path configDir, PluginModule[] moduleArray) {
        CMcCore.plugin = plugin;
        CMcCore.pluginContainer = pluginContainer;
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
    }

    public static Logger getLogger() {
        return logger;
    }

    public static Object getPlugin() {
        return plugin;
    }

    public static PluginContainer getPluginContainer() {
        return pluginContainer;
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
        // ???????????????????????????????????????????????????????????????????????????????????????????????????

        while (true) {
            try {
                Thread.sleep(2^31);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // TODO ????????????????????????
        }
    }

    // Handling initialization state events

    public void onPreInitialization(GamePreInitializationEvent event) {
        logger.debug("PreInit");
        // logger onPreInitializationProxy

        Sponge.getEventManager().registerListeners(plugin, this);

        // ???????????????????????????????????????????????????
        // TODO ?????????????????????????????????
        if (!TimeZone.getDefault().getID().equals(TimeZone.getTimeZone("UTC").getID())) {
            // ????????????????????????????????????????????????
            logger.error("???????????????[UTC]???????????????????????????");
            stopEternally();
        }

        // ?????????????????????

        // ?????????????????????????????????????????????jar??????????????????????????????????????????????????????????????????
        if (!Files.exists(configCommon)) {
            try {
                Files.createDirectories(configDir);
                Sponge.getAssetManager().getAsset(plugin, "common.conf").get().copyToFile(configCommon);
            } catch (IOException e) {
                e.printStackTrace();
                stopEternally();
            }
        }

        // ????????????????????????
        this.commonConfigLoader = HoconConfigurationLoader.builder().setPath(configCommon).build();

        try {
            commonConfigNode = commonConfigLoader.load();
        } catch (IOException e) {
            // ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????

            e.printStackTrace();
            stopEternally();
        }

        logger.info("???????????????????????????????????????");
    }

    public void onPrePostInitialization() {
        for (PluginModule module : modules) {
            logger.info("????????????/" + module.getClass().getCanonicalName());
            try {
                module.onPreInitializationProxy();
            } catch (Exception e) {
                e.printStackTrace();
                stopEternally();
            }
        }
    }

    @Listener
    public void onInitialization(GameInitializationEvent event) {
        logger.debug("Init");

        // plugin fuc onInitializationProxy
        for (PluginModule module : modules) {
            logger.info("?????????/" + module.getClass().getCanonicalName());
            try {
                module.onInitializationProxy();
            } catch (Exception e) {
                e.printStackTrace();
                stopEternally();
            }
        }

        Sponge.getCommandManager().register(CMcCore.getPlugin(), CmdWorldTeleport.CALLABLE, "wtp");
    }

    // Handling running state event

    @Listener
    public void onAboutToStartServer(GameAboutToStartServerEvent event) {
        logger.debug("ServerStarting");
        // onInitializationProxy for server (worlds are not loaded yet)

        for (PluginModule module : modules) {
            logger.info("?????????????????????/" + module.getClass().getCanonicalName());
            try {
                module.onAboutToStartServerProxy();
            } catch (Exception e) {
                e.printStackTrace();
                stopEternally();
            }
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

        // ????????????????????????

        // ???????????????
        for (int i = modules.size() - 1; i >= 0; i--) {
            logger.info("?????????????????????/" + modules.get(i).getClass().getCanonicalName());
            try {
                modules.get(i).onStoppingServerProxy();
            } catch (Exception e) {
                e.printStackTrace();
                stopEternally();
            }
        }
    }

    @Listener
    public void onStoppedServer(GameStoppedServerEvent event) {
        logger.debug("ServerStopped");
        // no players are connected. world are saved.

        // ????????????????????????

        // ???????????????
        for (int i = modules.size() - 1; i >= 0; i--) {
            logger.info("??????????????????/" + modules.get(i).getClass().getCanonicalName());
            try {
                modules.get(i).onStoppedServerProxy();
            } catch (Exception e) {
                e.printStackTrace();
                stopEternally();
            }
        }

        try {
            commonConfigLoader.save(commonConfigNode);
        } catch (IOException e) {
            e.printStackTrace();
            logger.warn("??????????????????????????????????????????[????????????]");
        }
    }

    // Handling stopping state event (may not be called if server has stopped via force quitting or something)

    @Listener
    public void onStopped(GameStoppedEvent event) {
        // called immediate before closing java
        logger.debug("GameStopped");
    }
}

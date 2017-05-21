package net.cupmouse.minecraft;

public interface PluginModule {

    default void onPreInitializationProxy() throws Exception {

    }

    default void onInitializationProxy() throws Exception {

    }

    default void onAboutToStartServerProxy() throws Exception {

    }

    default void onServerStartingProxy() throws Exception {

    }

    default void onStoppingServerProxy() throws Exception {

    }

    default void onStoppedServerProxy() throws Exception {

    }
}

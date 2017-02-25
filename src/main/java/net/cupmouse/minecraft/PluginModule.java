package net.cupmouse.minecraft;

public interface PluginModule {

    default void onPreInitializationProxy() {

    }

    default void onInitializationProxy() {

    }

    default void onAboutToStartServerProxy() {

    }

    default void onStoppedServerProxy() {

    }
}

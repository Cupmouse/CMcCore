package net.cupmouse.minecraft;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.server.ClientPingServerEvent;
import org.spongepowered.api.text.Text;

public class PongPingModule implements PluginModule {

    @Listener
    public void onClientPingServer(ClientPingServerEvent event) {
        // TODO ここで他スレッドを待機することになるので、もしデータベースのクエリが満ぱんだと処理されずに詰まる！
//
//        Future<String> future = plugin.getDbm().queueQueryTask(() -> {
//            Connection connection = plugin.getDbm().getConnection();
//
//            PreparedStatement prepStmt = connection.prepareStatement(
//                    "SELECT name FROM users WHERE address = ?");
//            prepStmt.setBytes(1, event.getClient().getAddress().getAddress().getAddress());
//
//            ResultSet resultSet = prepStmt.executeQuery();
//            if (resultSet.next()) {
//                return resultSet.getString(1);
//            } else {
//                throw new IllegalStateException("結果が帰ってきません");
//            }
//        });
//
//        String name = null;
//
//        try {
//            // TODO ここでfuture.get()を使うな！結果が帰るまでロックされ、最悪サーバークラッシュ
//            name = future.get();
//        } catch (InterruptedException | ExecutionException e) {
//            e.printStackTrace();
//        }

//        event.getResponse().setDescription(Text.of("CMc Minecraft Server", "\n", "Hi, ", name, "!"));
        event.getResponse().setDescription(Text.of("CMc Minecraft Server", "\n", "Hi, ", event.getClient().getAddress(), "!"));

    }

    @Override
    public void onInitializationProxy() {
        // TODO 今後、ファビコンを変えたくなったら変える
//        plugin.getGame().getRegistry().loadFavicon();
        Sponge.getEventManager().registerListeners(CMcCore.getPlugin(), this);
    }
}

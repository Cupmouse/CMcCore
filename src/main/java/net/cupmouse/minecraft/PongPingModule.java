package net.cupmouse.minecraft;

import org.spongepowered.api.GameRegistry;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.server.ClientPingServerEvent;
import org.spongepowered.api.network.status.Favicon;
import org.spongepowered.api.text.Text;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class PongPingModule implements PluginModule {

    private CMcPlugin plugin;

    public PongPingModule(CMcPlugin plugin) {
        this.plugin = plugin;
    }

    @Listener
    public void onClientPingServer(ClientPingServerEvent event) {
        // TODO ここで他スレッドを待機することになるので、もしデータベースのクエリが満ぱんだと処理されずに詰まる！

        Future<String> future = plugin.getDbm().queueQueryTask(() -> {
            Connection connection = plugin.getDbm().getConnection();

            PreparedStatement prepStmt = connection.prepareStatement(
                    "SELECT name FROM users WHERE address = ?");
            prepStmt.setBytes(1, event.getClient().getAddress().getAddress().getAddress());

            ResultSet resultSet = prepStmt.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString(1);
            } else {
                throw new IllegalStateException("結果が帰ってきません");
            }
        });

        String name = null;

        try {
            name = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        event.getResponse().setDescription(Text.of("CMc Minecraft Server", "\n", "Hi, ", name, "!"));

    }

    @Override
    public void onInitializationProxy() {
        // TODO 今後、ファビコンを変えたくなったら変える
//        plugin.getGame().getRegistry().loadFavicon();
    }
}

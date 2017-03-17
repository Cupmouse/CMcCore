package net.cupmouse.minecraft.realtimestream;

import net.cupmouse.minecraft.CMcPlugin;
import net.cupmouse.minecraft.PluginModule;
import org.json.JSONArray;
import org.json.JSONObject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class RealtimeStreamModule implements PluginModule {

    private static byte[] MAGIC_ACCEPT = new byte[]{(byte) 246, (byte) 129, (byte) 155, 116, (byte) 128};
    private static byte[] MAGIC_REPLY = new byte[]{105, (byte) 213, 79, 25, (byte) 180};
    private CMcPlugin plugin;
    private ServerSocketChannel serverSocketChannel;
    private final List<JSONArray> queue = new ArrayList<>();
    private Sender senderTask;

    public RealtimeStreamModule(CMcPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onInitializationProxy() {
        senderTask = new Sender();
        senderTask.start();
        Sponge.getGame().getEventManager().registerListeners(plugin, this);
        plugin.getLogger().info("リアルタイムストリームを開始しました！");
    }

    @Override
    public void onStoppedServerProxy() {
        plugin.getLogger().info("リアルタイムストリームを終了します");
        try {
            serverSocketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Join event) {
        queue(RealtimeMessageFactory.createJoin(event.getTargetEntity().getName()));

    }

    public void queue(JSONArray jsonArray) {
        synchronized (queue) {
            // TODO 様子見で変更
            if (queue.size() >= 10) {
                queue.set(9, jsonArray);
            } else {
                queue.add(jsonArray);
            }

            synchronized (senderTask) {
                senderTask.notify();
            }
        }
    }

    private class Sender extends Thread {

        public Sender() {
            super("Realtime stream thread");
        }

        @Override
        public void run() {
            serverSocketChannel = null;

            try {
                serverSocketChannel = ServerSocketChannel.open();

                serverSocketChannel.bind(new InetSocketAddress(35324));
            } catch (IOException e) {
                e.printStackTrace();

                if (serverSocketChannel != null) {
                    try {
                        serverSocketChannel.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }

                return;
            }

            Charset utf8 = Charset.forName("UTF-8");

            while (serverSocketChannel.isOpen()) {
                SocketChannel socketChannel = null;

                try {
                    plugin.getLogger().info("待機中...");
                    socketChannel = serverSocketChannel.accept();
                    plugin.getLogger().info("接続要求がありました。 : " + socketChannel.socket().getInetAddress());

                    // タイムアウト設定
                    socketChannel.socket().setSoTimeout(1000);

                    // 接続時にマジック確認する
                    ByteBuffer magic = ByteBuffer.allocate(5);
                    socketChannel.read(magic);

                    if (!Arrays.equals(magic.array(), MAGIC_ACCEPT)) {
                        throw new IOException("マジック失敗");
                    }
                    plugin.getLogger().info("マジックパス");

                    socketChannel.write(ByteBuffer.wrap(MAGIC_REPLY));

                    ByteBuffer reply = ByteBuffer.allocate(1);
                    socketChannel.read(reply);
                    reply.position(0);

                    if (reply.get() != 100) {
                        throw new IOException("失敗");
                    }
                    plugin.getLogger().info("リターンマジックパス");
                    plugin.getLogger().info("送信を開始");

                    while (socketChannel.isConnected()) {
                        ArrayList<JSONArray> queueCopy;

                        synchronized (queue) {
                            queueCopy = new ArrayList<>(queue);
                            queue.clear();
                        }

                        ByteBuffer lengthBuffer = ByteBuffer.allocate(4);

                        // サイズが０ならハートビートを送る。（内容が0バイトとして送る。）
                        if (queue.size() == 0) {
                            socketChannel.write(ByteBuffer.wrap(new byte[] {0, 0, 0, 0}));
                        } else {
                            for (JSONArray jsonArray : queueCopy) {
                                // パースに失敗したら切断
                                String str = jsonArray.toString();

                                int length = str.length();
                                lengthBuffer.putInt(length);
                                socketChannel.write(lengthBuffer);
                                socketChannel.write(utf8.encode(str));
                                lengthBuffer.clear();
                            }
                        }

                        synchronized (senderTask) {
                            senderTask.wait(3000);
                        }
                    }

                    plugin.getLogger().info("接続が切れました。送信を終了");
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    if (socketChannel != null) {
                        try {
                            socketChannel.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                plugin.getLogger().info("切断");
            }
        }
    }
}

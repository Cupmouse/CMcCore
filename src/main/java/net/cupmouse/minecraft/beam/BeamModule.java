package net.cupmouse.minecraft.beam;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.cupmouse.minecraft.CMcCore;
import net.cupmouse.minecraft.PluginModule;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.weather.Lightning;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.achievement.GrantAchievementEvent;
import org.spongepowered.api.event.action.FishingEvent;
import org.spongepowered.api.event.action.LightningEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.scheduler.Task;

import static net.cupmouse.minecraft.beam.BeamDataFactory.*;

public class BeamModule implements PluginModule {

    private Channel channel;
    private boolean stop;
    private Task task;
    private boolean enabled;

    @Override
    public void onInitializationProxy() {
        CommentedConfigurationNode nodeBeam = CMcCore.getCommonConfigNode().getNode("beam");

        if (!nodeBeam.getNode("enabled").getBoolean()) {
            enabled = false;
            return;
        }

        enabled = true;

        String url = nodeBeam.getNode("url").getString();
        int port = nodeBeam.getNode("port").getInt();

        // TODO beamの接続先設定

        task = Sponge.getScheduler().createTaskBuilder().async()
                .execute(() -> {
                    while (!stop) {
                        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

                        try {
                            Bootstrap bs = new Bootstrap();
                            bs.group(workerGroup);
                            bs.channel(NioSocketChannel.class);
                            bs.option(ChannelOption.SO_KEEPALIVE, true);
                            bs.handler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel ch) throws Exception {
                                    ch.pipeline().addLast(new BeamHandler());
                                }
                            });

                            channel = bs.connect(url, port).sync().channel();

                            channel.closeFuture().sync();
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            workerGroup.shutdownGracefully();
                        }

                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .submit(CMcCore.getPlugin());

        Sponge.getEventManager().registerListeners(CMcCore.getPlugin(), this);
        CMcCore.getLogger().info("リアルタイムストリームを開始しました！");
    }

    @Override
    public void onStoppedServerProxy() {
        if (!enabled) {
            return;
        }

        CMcCore.getLogger().info("リアルタイムストリームを終了します");

        try {
            stop = true;
            channel.close();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        task.cancel();
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Join event) {
        ByteBuf byteBuf = channel.alloc().buffer();

        queue(createJoin(byteBuf, event.getTargetEntity()));
    }

    @Listener
    public void onDisconnect(ClientConnectionEvent.Disconnect event) {
        ByteBuf byteBuf = channel.alloc().buffer();

        queue(createDisconnect(byteBuf, event.getTargetEntity()));
    }

    @Listener
    public void onLightningStrike(LightningEvent.Strike event) {
        event.getCause().first(Lightning.class).ifPresent(lightning -> {
            ByteBuf byteBuf = channel.alloc().buffer();

            queue(createLightningStrike(byteBuf, lightning));
        });
    }

    @Listener
    public void onPlayerDied(DestructEntityEvent.Death event) {
        if (event.getTargetEntity() instanceof Player) {
            ByteBuf byteBuf = channel.alloc().buffer();

            queue(createDied(byteBuf, (Player) event.getTargetEntity()));
        }
    }

    @Listener
    public void onChat(MessageChannelEvent.Chat event, @First Player player) {
        ByteBuf byteBuf = channel.alloc().buffer();

        queue(createChat(byteBuf, player, event.getRawMessage()));
    }

    @Listener
    public void onAchievementEarn(GrantAchievementEvent.TargetPlayer event) {
        ByteBuf byteBuf = channel.alloc().buffer();

        queue(createEarnAchievement(byteBuf, event.getTargetEntity(), event.getAchievement()));
    }

    @Listener
    public void onFishingEvent(FishingEvent.Stop event) {
        event.getFishHook().getHookedEntity().map(entity -> {
            if (entity instanceof Item) {
                return (Item) entity;
            } else {
                return null;
            }
        }).ifPresent(item -> {
            ByteBuf byteBuf = channel.alloc().buffer();

            queue(createFishing(byteBuf, item));
        });
    }

    public synchronized void queue(ByteBuf byteBuf) {
        // TODO 様子見で変更

        ByteBuf dataBytes = channel.alloc().buffer(2);
        dataBytes.writeShort(byteBuf.writerIndex());

        channel.write(dataBytes);
        channel.write(byteBuf);
        channel.flush();
    }
}

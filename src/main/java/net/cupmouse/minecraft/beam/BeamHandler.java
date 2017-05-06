package net.cupmouse.minecraft.beam;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.cupmouse.minecraft.CMcCore;

import java.io.IOException;

public class BeamHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        CMcCore.getLogger().info("ストリーム接続");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        CMcCore.getLogger().info("ストリーム切断");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        throw new IOException("何らかのデータが送信されています。");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}

package net.cupmouse.minecraft.beam;

import io.netty.buffer.ByteBuf;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.UUID;

public final class BeamDataUtil {

    public static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");
    public static final Charset CHARSET_ASCII = Charset.forName("ASCII");
    //    private static final int UTF8_MAX_BYTE_PAR_CHAR = (int) CharsetUtil.encoder(CHARSET_UTF8).maxBytesPerChar();

    private BeamDataUtil() {

    }

    public static ByteBuf writePlayer(ByteBuf byteBuf, Player player) {
        writeUUID(byteBuf, player.getUniqueId());
        writeAsciiBytePrefixed(byteBuf, player.getName());

        return byteBuf;
    }

    public static ByteBuf writeLocation(ByteBuf byteBuf, Location<World> location) {
        writeAsciiBytePrefixed(byteBuf, location.getExtent().getName());
        byteBuf.writeInt(location.getBlockX());
        byteBuf.writeInt(location.getBlockY());
        byteBuf.writeInt(location.getBlockZ());

        return byteBuf;
    }

    public static void writeUUID(ByteBuf byteBuf, UUID uuid) {
        byteBuf.writeLong(uuid.getMostSignificantBits());
        byteBuf.writeLong(uuid.getLeastSignificantBits());
    }

    public static ByteBuf writeAsciiBytePrefixed(ByteBuf byteBuf, String string) {
        ByteBuffer encode = CHARSET_ASCII.encode(string);

        byteBuf.writeByte(encode.capacity());
        byteBuf.writeBytes(encode);

        return byteBuf;
    }

    public static ByteBuf writeUTF8ShortPrefixed(ByteBuf byteBuf, String string) {
        // TODO NETTYバージョンアップ待ち
//        ByteBuf tempBuf = Unpooled.buffer(ByteBufUtil.utf8MaxBytes(charSequence));
//        int charSequenceBytes = tempBuf.writeCharSequence(charSequence, BeamDataUtil.CHARSET_UTF8);

        ByteBuffer encoded = CHARSET_UTF8.encode(string);

        byteBuf.writeShort(encoded.capacity());
        byteBuf.writeBytes(encoded);

//        // 一時的なバッファをリリースする
//        tempBuf.release();

        return byteBuf;
    }
}

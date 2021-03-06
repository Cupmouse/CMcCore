package net.cupmouse.minecraft.beam;

import io.netty.buffer.ByteBuf;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.weather.Lightning;
import org.spongepowered.api.statistic.achievement.Achievement;
import org.spongepowered.api.text.Text;

import static net.cupmouse.minecraft.beam.BeamDataUtil.*;

public final class BeamDataFactory {

    private BeamDataFactory() {
    }

    public static ByteBuf createJoin(ByteBuf byteBuf, Player player) {
        writeDataId(byteBuf, DataType.JOIN);

        writePlayer(byteBuf, player);
        writeLocation(byteBuf, player.getLocation());

        return byteBuf;
    }

    public static ByteBuf createDisconnect(ByteBuf byteBuf, Player player) {
        writeDataId(byteBuf, DataType.DISCONNECT);

        writePlayer(byteBuf, player);
        writeLocation(byteBuf, player.getLocation());

        return byteBuf;
    }

    public static ByteBuf createChat(ByteBuf byteBuf, Player player, Text chatMessage) {
        writeDataId(byteBuf, DataType.CHAT);

        writePlayer(byteBuf, player);
        writeUTF8ShortPrefixed(byteBuf, chatMessage.toPlain());

        return byteBuf;
    }

    public static ByteBuf createDied(ByteBuf byteBuf, Player player) {
        writeDataId(byteBuf, DataType.DIED);

        writePlayer(byteBuf, player);
        writeLocation(byteBuf, player.getLocation());

        return byteBuf;
    }

    public static ByteBuf createEarnAchievement(ByteBuf byteBuf, Player player, Achievement achievement) {
        writeDataId(byteBuf, DataType.EARN_ACHIEVEMENT);

        writePlayer(byteBuf, player);

        return byteBuf;
    }

    public static ByteBuf createLightningStrike(ByteBuf byteBuf, Lightning lightning) {
        writeDataId(byteBuf, DataType.LIGHTNING_STRIKE);

        // TODO
        writeLocation(byteBuf, lightning.getLocation());

        return byteBuf;
    }

    private static void writeDataId(ByteBuf byteBuf, DataType dataType) {
        byteBuf.writeShort(dataType.dataId);
    }

    public static ByteBuf createFishing(ByteBuf byteBuf, Item item) {
        return null;
    }

    private enum DataType {
        // ??????????????????ID???????????????SHORT

        // 0x0  : ?????????????????????????????????
        // 0x1  : ????????????????????????????????????
        JOIN(0x1000),
        DISCONNECT(0x1001),
        CHAT(0x1002),
        DIED(0x1003),
        EARN_ACHIEVEMENT(0x1004),
        SLEEP(0x1005),
        DAMAGED(0x1006),

        // 0x2  : ?????????????????????????????????
        // 0x21 : ???????????????????????????
        LIGHTNING_STRIKE(0x2100)
        ;

        private final short dataId;

        DataType(int dataId) {
            this.dataId = (short) dataId;
        }
    }
}


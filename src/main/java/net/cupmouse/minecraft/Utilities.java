package net.cupmouse.minecraft;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class Utilities {
    public static final DateTimeFormatter LOCALDATETIME_MYSQL_FORMAT_NONANO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter LOCALDATETIME_MYSQL_FORMAT_NANO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private Utilities() {
    }

    public static byte[] convertUUIDtoBytes(UUID uuid) {
        // TODO 最適化
        long leastSignificantBits = uuid.getLeastSignificantBits();
        long mostSignificantBits = uuid.getMostSignificantBits();

        return new byte[] {
                (byte) (mostSignificantBits >>> 56),
                (byte) (mostSignificantBits >>> 48),
                (byte) (mostSignificantBits >>> 40),
                (byte) (mostSignificantBits >>> 32),
                (byte) (mostSignificantBits >>> 24),
                (byte) (mostSignificantBits >>> 16),
                (byte) (mostSignificantBits >>> 8),
                (byte) mostSignificantBits,
                (byte) (leastSignificantBits >>> 56),
                (byte) (leastSignificantBits >>> 48),
                (byte) (leastSignificantBits >>> 40),
                (byte) (leastSignificantBits >>> 32),
                (byte) (leastSignificantBits >>> 24),
                (byte) (leastSignificantBits >>> 16),
                (byte) (leastSignificantBits >>> 8),
                (byte) leastSignificantBits,
        };
    }

    // TODO
//    public static Vector3d loadVector3dFromConfig(ConfigurationNode configurationNode) {
//        double x = configurationNode.getNode("x").getDouble();
//        double y = configurationNode.getNode("y").getDouble();
//        double z = configurationNode.getNode("z").getDouble();
//
//        return new Vector3d(x, y, z);
//    }
}

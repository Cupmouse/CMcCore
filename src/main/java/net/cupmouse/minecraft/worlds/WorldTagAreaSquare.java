package net.cupmouse.minecraft.worlds;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

import java.util.ArrayList;

public class WorldTagAreaSquare extends WorldTagArea {

    public final Vector3d minPos;
    public final Vector3d maxPos;

    private WorldTagAreaSquare(WorldTag worldTag, double x1, double y1, double z1, double x2, double y2, double z2) {
        super(worldTag);

        this.minPos = new Vector3d(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2));
        this.maxPos = new Vector3d(Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }

    private WorldTagAreaSquare(WorldTag worldTag, Vector3d minPos, Vector3d maxPos) {
        super(worldTag);

        this.minPos = minPos;
        this.maxPos = maxPos;
    }

    @Override
    public boolean isInArea(Vector3d position) {
        return isInArea(position.getX(), position.getY(), position.getZ());
    }

    @Override
    public boolean isInArea(double x, double y, double z) {
        return (x >= minPos.getX() && x <= maxPos.getX())
                && (y >= minPos.getY() && y <= maxPos.getY())
                && (z >= minPos.getZ() && z <= maxPos.getZ());
    }

    @Override
    public boolean isInArea(int x, int y, int z) {
        return (x >= minPos.getX() && x <= maxPos.getX())
                && (y >= minPos.getY() && y <= maxPos.getY())
                && (z >= minPos.getZ() && z <= maxPos.getZ());
    }

    @Override
    public BlockLocSequence getOutlineBlocks() {
        ArrayList<Vector3i> blockLocs = new ArrayList<>();

        int x, z;

        // 四角を書く
        int minX = minPos.getFloorX();
        int maxX = maxPos.getFloorX();
        int minY = minPos.getFloorY();
        int maxY = maxPos.getFloorY();
        int minZ = minPos.getFloorZ();
        int maxZ = maxPos.getFloorZ();

        for (int y = minY; y <= maxY; y++) {
            x = minX;
            z = minZ;

            // FIXME

            // 下段と上段なら四角を描く
            if (y == minY || y == maxY) {
                for (; x <= maxX; x++) {
                    blockLocs.add(new Vector3i(x, y, z));
                }

                for (; z <= maxZ; z++) {
                    blockLocs.add(new Vector3i(x, y, z));
                }

                for (; x >= minX; x--) {
                    blockLocs.add(new Vector3i(x, y, z));
                }

                for (; z > minZ; z--) {
                    blockLocs.add(new Vector3i(x, y, z));
                }

            } else {
                // でないなら、横のフレームを作る。
                blockLocs.add(new Vector3i(minX, y, minZ));
                blockLocs.add(new Vector3i(maxX, y, minZ));
                blockLocs.add(new Vector3i(minX, y, maxZ));
                blockLocs.add(new Vector3i(maxX, y, maxZ));
            }
        }

        return new BlockLocSequence(worldTag, blockLocs);
    }

    @Override
    public BlockLocSequence getCornerBlocks() {
        ArrayList<Vector3i> blockLocs = new ArrayList<>();
        int minX = minPos.getFloorX();
        int maxX = maxPos.getFloorX();

        int minY = minPos.getFloorY();
        int maxY = maxPos.getFloorY();

        int minZ = minPos.getFloorZ();
        int maxZ = maxPos.getFloorZ();

        blockLocs.add(new Vector3i(minX, minY, minZ));
        blockLocs.add(new Vector3i(maxX, minY, minZ));
        blockLocs.add(new Vector3i(minX, minY, maxZ));
        blockLocs.add(new Vector3i(maxX, minY, maxZ));
        blockLocs.add(new Vector3i(minX, maxY, minZ));
        blockLocs.add(new Vector3i(maxX, maxY, minZ));
        blockLocs.add(new Vector3i(minX, maxY, maxZ));
        blockLocs.add(new Vector3i(maxX, maxY, maxZ));

        return new BlockLocSequence(worldTag, blockLocs);
    }

    @Override
    public BlockLocSequence getEveryBlocks() {
        ArrayList<Vector3i> blockLocs = new ArrayList<>();

        int minX = minPos.getFloorX();
        int maxX = maxPos.getFloorX();
        int minY = minPos.getFloorY();
        int maxY = maxPos.getFloorY();
        int minZ = minPos.getFloorZ();
        int maxZ = maxPos.getFloorZ();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    blockLocs.add(new Vector3i(x, y, z));
                }
            }
        }

        return new BlockLocSequence(worldTag, blockLocs);
    }

    @Override
    public WorldTagArea worldTag(WorldTag worldTag) {
        return new WorldTagAreaSquare(worldTag, minPos, maxPos);
    }

    @Override
    public WorldTagArea relativeBase(WorldTagLocation baseLocation) {
        return new WorldTagAreaSquare(worldTag,
                minPos.add(baseLocation.position), maxPos.add(baseLocation.position));
    }

    @Override
    public WorldTagAreaSquare relativeBasePoint(Vector3d basePoint) {
        return new WorldTagAreaSquare(worldTag, minPos.add(basePoint), maxPos.add(basePoint));
    }

    @Override
    public WorldTagArea relativeTo(WorldTagLocation baseLocation) {
        return new WorldTagAreaSquare(worldTag,
                minPos.sub(baseLocation.position), maxPos.sub(baseLocation.position));
    }

    @Override
    public WorldTagAreaSquare relativeToPoint(Vector3d basePoint) {
        return new WorldTagAreaSquare(worldTag, minPos.sub(basePoint), maxPos.sub(basePoint));
    }

    public static WorldTagAreaSquare fromWorldTagAndPositions(WorldTag worldTag, Vector3d pos1, Vector3d pos2) {
        return new WorldTagAreaSquare(worldTag,
                pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());
    }

    static class Serializer implements TypeSerializer<WorldTagAreaSquare> {

        @Override
        public WorldTagAreaSquare deserialize(TypeToken<?> type, ConfigurationNode value)
                throws ObjectMappingException {
            WorldTag worldTag = value.getNode("world_tag").getValue(TypeToken.of(WorldTag.class));
            Vector3d minPos = value.getNode("min_position").getValue(TypeToken.of(Vector3d.class));
            Vector3d maxPos = value.getNode("max_position").getValue(TypeToken.of(Vector3d.class));

            return new WorldTagAreaSquare(worldTag, minPos, maxPos);
        }

        @Override
        public void serialize(TypeToken<?> type, WorldTagAreaSquare obj, ConfigurationNode value)
                throws ObjectMappingException {
            value.getNode("world_tag").setValue(TypeToken.of(WorldTag.class), obj.worldTag);
            value.getNode("min_position").setValue(TypeToken.of(Vector3d.class), obj.minPos);
            value.getNode("max_position").setValue(TypeToken.of(Vector3d.class), obj.maxPos);
        }
    }
}

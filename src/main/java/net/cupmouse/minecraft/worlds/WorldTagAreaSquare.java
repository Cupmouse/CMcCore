package net.cupmouse.minecraft.worlds;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.spongepowered.api.data.value.mutable.SetValue;
import org.spongepowered.api.world.World;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class WorldTagAreaSquare extends WorldTagArea {

    public final Vector3i minPos;
    public final Vector3i maxPos;

    public WorldTagAreaSquare(WorldTag worldTag, int x1, int x2, int y1, int y2, int z1, int z2) {
        super(worldTag);

        this.minPos = new Vector3i(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2));
        this.maxPos = new Vector3i(Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }

    public WorldTagAreaSquare(WorldTag worldTag, Vector3i pos1, Vector3i pos2) {
        this(worldTag, pos1.getX(), pos2.getX(), pos1.getY(), pos2.getY(), pos1.getZ(), pos2.getZ());
    }

    @Override
    public boolean isInArea(Vector3d position) {
        return (position.getX() >= minPos.getX() && position.getX() <= maxPos.getX())
                && (position.getY() >= minPos.getY() && position.getX() <= maxPos.getY())
                && (position.getZ() >= minPos.getZ() && position.getZ() <= maxPos.getZ());
    }

    @Override
    public BlockLocSequence getOutlineBlocks() {
        ArrayList<Vector3i> blockLocs = new ArrayList<>();

        int x = minPos.getX(), y = minPos.getY(), z = minPos.getZ();

        // 四角を書く
        for (; y <= maxPos.getY(); y++) {

            // FIXME

            // 下段と上段なら四角を描く
            if (y == minPos.getY() || y == maxPos.getY()) {
                for (; x <= maxPos.getX(); x++) {
                    blockLocs.add(new Vector3i(x, y, z));
                }

                for (; z <= maxPos.getZ(); z++) {
                    blockLocs.add(new Vector3i(x, y, z));
                }

                for (; z >= minPos.getZ(); z--) {
                    blockLocs.add(new Vector3i(x, y, z));
                }

                for (; x >= maxPos.getX(); x--) {
                    blockLocs.add(new Vector3i(x, y, z));
                }
            } else {
                // でないなら、横のフレームを作る。
                blockLocs.add(new Vector3i(minPos.getX(), y, minPos.getZ()));
                blockLocs.add(new Vector3i(maxPos.getX(), y, minPos.getZ()));
                blockLocs.add(new Vector3i(minPos.getX(), y, maxPos.getZ()));
                blockLocs.add(new Vector3i(maxPos.getX(), y, maxPos.getZ()));
            }
        }

        return new BlockLocSequence(worldTag, blockLocs);
    }

    @Override
    public BlockLocSequence getCornerBlocks() {
        ArrayList<Vector3i> blockLocs = new ArrayList<>();
        blockLocs.add(new Vector3i(minPos.getX(), minPos.getY(), minPos.getZ()));
        blockLocs.add(new Vector3i(maxPos.getX(), minPos.getY(), minPos.getZ()));
        blockLocs.add(new Vector3i(minPos.getX(), minPos.getY(), maxPos.getZ()));
        blockLocs.add(new Vector3i(maxPos.getX(), minPos.getY(), maxPos.getZ()));
        blockLocs.add(new Vector3i(minPos.getX(), maxPos.getY(), minPos.getZ()));
        blockLocs.add(new Vector3i(maxPos.getX(), maxPos.getY(), minPos.getZ()));
        blockLocs.add(new Vector3i(minPos.getX(), maxPos.getY(), maxPos.getZ()));
        blockLocs.add(new Vector3i(maxPos.getX(), maxPos.getY(), maxPos.getZ()));

        return new BlockLocSequence(worldTag, blockLocs);
    }

    @Override
    public WorldTagAreaSquare relativeBasePoint(Vector3i basePoint) {
        return new WorldTagAreaSquare(worldTag, minPos.add(basePoint), maxPos.add(basePoint));
    }

    @Override
    public WorldTagAreaSquare relativeTo(Vector3i basePoint) {
        return new WorldTagAreaSquare(worldTag, minPos.sub(basePoint), maxPos.sub(basePoint));
    }

    static class Serializer implements TypeSerializer<WorldTagAreaSquare> {

        @Override
        public WorldTagAreaSquare deserialize(TypeToken<?> type, ConfigurationNode value)
                throws ObjectMappingException {
            WorldTag worldTag = value.getNode("world_tag").getValue(TypeToken.of(WorldTag.class));
            Vector3i minPos = value.getNode("min_position").getValue(TypeToken.of(Vector3i.class));
            Vector3i maxPos = value.getNode("max_position").getValue(TypeToken.of(Vector3i.class));

            WorldTagAreaSquare areaSquare = new WorldTagAreaSquare(worldTag, minPos, maxPos);

            return areaSquare;
        }

        @Override
        public void serialize(TypeToken<?> type, WorldTagAreaSquare obj, ConfigurationNode value)
                throws ObjectMappingException {
            value.getNode("world_tag").setValue(TypeToken.of(WorldTag.class), obj.worldTag);
            value.getNode("min_position").setValue(TypeToken.of(Vector3i.class), obj.minPos);
            value.getNode("max_position").getValue(TypeToken.of(Vector3i.class), obj.maxPos);
        }
    }
}

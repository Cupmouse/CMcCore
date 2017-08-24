package net.cupmouse.minecraft.worlds;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public abstract class WorldTagArea {

    public final WorldTag worldTag;

    protected WorldTagArea(WorldTag worldTag) {
        this.worldTag = worldTag;
    }

    public boolean isInArea(Location<World> location) {
        return WorldTagModule.isThis(worldTag, location.getExtent())
                && isInArea(location.getPosition());
    }

    /**
     * ワールド成分は無視してポジションだけでエリア内にいるか確認します。
     * @param position
     * @return
     */
    public abstract boolean isInArea(Vector3d position);

    public abstract BlockLocSequence getOutlineBlocks();

    public abstract BlockLocSequence getCornerBlocks();

    public abstract WorldTagArea worldTag(WorldTag worldTag);

    public abstract WorldTagArea relativeBase(WorldTagLocation baseLocation);

    public abstract WorldTagArea relativeBasePoint(Vector3d basePoint);

    public abstract WorldTagArea relativeTo(WorldTagLocation baseLocation);

    public abstract WorldTagArea relativeToPoint(Vector3d basePoint);



    static class Serializer implements TypeSerializer<WorldTagArea> {

        @Override
        public WorldTagArea deserialize(TypeToken<?> type, ConfigurationNode value)
                throws ObjectMappingException {
            String shape = value.getNode("shape").getString();

            TypeToken<WorldTagAreaSquare> token;

            switch (shape) {
                case "square":
                    token = TypeToken.of(WorldTagAreaSquare.class);
                    break;
                default:
                    throw new ObjectMappingException("SHAPE不明");
            }

            return value.getValue(token);
        }

        @Override
        public void serialize(TypeToken<?> type, WorldTagArea obj, ConfigurationNode value)
                throws ObjectMappingException {

            if (obj instanceof WorldTagAreaSquare) {
                TypeSerializers.getDefaultSerializers().get(TypeToken.of(WorldTagAreaSquare.class))
                        .serialize(type, (WorldTagAreaSquare) obj, value);
            } else {
                throw new ObjectMappingException("登録されていない未知の形");
            }
        }
    }
}
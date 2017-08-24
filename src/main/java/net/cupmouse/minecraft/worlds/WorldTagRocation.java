package net.cupmouse.minecraft.worlds;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.reflect.TypeToken;
import net.cupmouse.minecraft.util.UnknownWorldException;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

public final class WorldTagRocation extends WorldTagPosition {

    public final Vector3d rotation;

    private WorldTagRocation(WorldTag worldTag, Vector3d position, Vector3d rotation) {
        super(worldTag, position);
        this.rotation = rotation;
    }

    public WorldTagLocation convertToLocation() {
        return WorldTagLocation.fromTagAndPosition(worldTag, position);
    }

    @Override
    public WorldTagRocation worldTag(WorldTag worldTag) {
        return new WorldTagRocation(worldTag, position, rotation);
    }

    @Override
    public WorldTagRocation position(Vector3d position) {
        return new WorldTagRocation(worldTag, position, rotation);
    }

    @Override
    public WorldTagRocation relativeBase(WorldTagPosition basePosition) {
        return (WorldTagRocation) super.relativeBase(basePosition);
    }

    @Override
    public WorldTagRocation relativeBasePoint(Vector3d basePoint) {
        return (WorldTagRocation) super.relativeBasePoint(basePoint);
    }

    @Override
    public WorldTagRocation relativeTo(WorldTagPosition basePosition) {
        return (WorldTagRocation) super.relativeTo(basePosition);
    }

    @Override
    public WorldTagRocation relativeToPoint(Vector3d basePoint) {
        return (WorldTagRocation) super.relativeToPoint(basePoint);
    }

    public static WorldTagRocation fromTagAndPositionAndRotation(WorldTag worldTag,
                                                                 Vector3d position, Vector3d rotation) {
        return new WorldTagRocation(worldTag, position, rotation);
    }

    public static Optional<WorldTagRocation> fromEntity(Entity entity) {
        Location<World> location = entity.getLocation();

        Optional<WorldTag> worldTagOptional = WorldTagModule.whatIsThisWorld(location.getExtent());
        return worldTagOptional.flatMap(worldTag -> Optional.of(
                new WorldTagRocation(worldTag, location.getPosition(), entity.getRotation())));

    }

    public static Optional<WorldTagRocation> fromSponge(Location<World> spongeLocation, Vector3d rotation) {
        Optional<WorldTag> worldTagOptional = WorldTagModule.whatIsThisWorld(spongeLocation.getExtent());

        return worldTagOptional.map(worldTag ->
                new WorldTagRocation(worldTag, spongeLocation.getPosition(), rotation));

    }

    static class Serializer implements TypeSerializer<WorldTagRocation> {

        @Override
        public WorldTagRocation deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {

            WorldTag worldTag = value.getNode("world_tag").getValue(TypeToken.of(WorldTag.class));
            Vector3d position = value.getNode("position").getValue(TypeToken.of(Vector3d.class));
            Vector3d rotation = value.getNode("rotation").getValue(TypeToken.of(Vector3d.class));

            return new WorldTagRocation(worldTag, position, rotation);
        }

        @Override
        public void serialize(TypeToken<?> type, WorldTagRocation obj, ConfigurationNode value) throws ObjectMappingException {
            value.getNode("world_tag").setValue(TypeToken.of(WorldTag.class), obj.worldTag);
            value.getNode("position").setValue(TypeToken.of(Vector3d.class), obj.position);
            value.getNode("rotation").setValue(TypeToken.of(Vector3d.class), obj.rotation);
        }
    }
}

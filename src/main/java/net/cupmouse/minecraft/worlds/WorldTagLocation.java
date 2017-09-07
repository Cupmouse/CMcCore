package net.cupmouse.minecraft.worlds;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

public class WorldTagLocation extends WorldTagPosition {

    public WorldTagLocation(WorldTag worldTag, Vector3d position) {
        super(worldTag, position);
    }

    @Override
    public boolean teleportHere(Entity entity) {
        return convertToSpongeLocation().filter(entity::setLocation).isPresent();
    }

    @Override
    public Optional<Transform<World>> convertToTransform() {
        Optional<World> taggedWorld = WorldTagModule.getTaggedWorld(worldTag);

        return taggedWorld.map(world -> new Transform<>(world, position));
    }

    @Override
    public WorldTagLocation worldTag(WorldTag worldTag) {
        return new WorldTagLocation(worldTag, position);
    }

    @Override
    public WorldTagPosition position(Vector3d position) {
        return new WorldTagLocation(worldTag, position);
    }

    @Override
    public WorldTagLocation relativeBase(WorldTagPosition basePosition) {
        return (WorldTagLocation) super.relativeBase(basePosition);
    }

    @Override
    public WorldTagLocation relativeBasePoint(Vector3d basePoint) {
        return (WorldTagLocation) super.relativeBasePoint(basePoint);
    }

    @Override
    public WorldTagLocation relativeTo(WorldTagPosition basePosition) {
        return (WorldTagLocation) super.relativeTo(basePosition);
    }

    @Override
    public WorldTagLocation relativeToPoint(Vector3d basePoint) {
        return (WorldTagLocation) super.relativeToPoint(basePoint);
    }

    public WorldTagRocation convertToRocation() {
        return WorldTagRocation.fromTagAndPositionAndRotation(worldTag, position, Vector3d.ZERO);
    }

    public static WorldTagLocation fromTagAndPosition(WorldTag worldTag, Vector3d position) {
        return new WorldTagLocation(worldTag, position);
    }

    public static Optional<WorldTagLocation> fromEntity(Entity entity) {
        Location<World> location = entity.getLocation();

        Optional<WorldTag> worldTagOptional = WorldTagModule.whatIsThisWorld(location.getExtent());
        return worldTagOptional.flatMap(worldTag -> Optional.of(
                new WorldTagLocation(worldTag, location.getPosition())));
    }

    public static Optional<WorldTagLocation> fromSponge(Location<World> spongeLocation) {
        return WorldTagModule.whatIsThisWorld(spongeLocation.getExtent())
                .map(worldTag -> new WorldTagLocation(worldTag, spongeLocation.getPosition()));
    }

    static class Serializer implements TypeSerializer<WorldTagLocation> {

        @Override
        public WorldTagLocation deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
            WorldTag worldTag = value.getNode("world_tag").getValue(TypeToken.of(WorldTag.class));
            Vector3d position = value.getNode("position").getValue(TypeToken.of(Vector3d.class));

            return new WorldTagLocation(worldTag, position);
        }

        @Override
        public void serialize(TypeToken<?> type, WorldTagLocation obj, ConfigurationNode value) throws ObjectMappingException {
            value.getNode("world_tag").setValue(TypeToken.of(WorldTag.class), obj.worldTag);
            value.getNode("position").setValue(TypeToken.of(Vector3d.class), obj.position);
        }
    }
}

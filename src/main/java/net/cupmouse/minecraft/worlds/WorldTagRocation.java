package net.cupmouse.minecraft.worlds;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.reflect.TypeToken;
import net.cupmouse.minecraft.util.UnknownWorldException;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

public final class WorldTagRocation implements WorldTagPosition {

    public final WorldTag worldTag;
    public final Vector3d position;
    public final Vector3d rotation;

    public WorldTagRocation(WorldTag worldTag, Vector3d position, Vector3d rotation) {
        this.worldTag = worldTag;
        this.position = position;
        this.rotation = rotation;
    }

    @Override
    public boolean teleportHere(Entity entity) throws UnknownWorldException {
        return entity.setLocationAndRotation(convertSponge(), rotation);
    }

    public WorldTagLocation convertLocation() {
        return new WorldTagLocation(worldTag, rotation);
    }

    @Override
    public Location<World> convertSponge() throws UnknownWorldException {
        return convertLocation().convertSponge();
    }

    @Override
    public WorldTag getWorldTag() {
        return worldTag;
    }

    @Override
    public Vector3d getPosition() {
        return position;
    }

    public static WorldTagRocation fromEntity(Entity entity) throws UnknownWorldException {
        Location<World> location = entity.getLocation();

        Optional<WorldTag> worldTagOptional = WorldTagModule.whatIsThisWorld(location.getExtent());

        if (!worldTagOptional.isPresent()) {
            throw new UnknownWorldException();
        }

        return new WorldTagRocation(worldTagOptional.get(), location.getPosition(), entity.getRotation());
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

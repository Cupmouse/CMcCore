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

public class WorldTagLocation implements WorldTagPosition {

    public final WorldTag worldTag;
    public final Vector3d position;

    public WorldTagLocation(WorldTag worldTag, Vector3d position) {
        this.worldTag = worldTag;
        this.position = position;
    }

    @Override
    public boolean teleportHere(Entity entity) throws UnknownWorldException {
        return entity.setLocation(convertSponge());
    }

    @Override
    public Location<World> convertSponge() throws UnknownWorldException {
        Optional<World> taggedWorld = WorldTagModule.getTaggedWorld(worldTag);

        if (!taggedWorld.isPresent()) {
            throw new UnknownWorldException();
        }

        return new Location<World>(taggedWorld.get(), position);
    }

    @Override
    public WorldTag getWorldTag() {
        return worldTag;
    }

    @Override
    public Vector3d getPosition() {
        return position;
    }

    @Override
    public WorldTagLocation relativeBasePoint(Vector3i basePoint) {
        return new WorldTagLocation(worldTag, position.add(basePoint.toDouble()));
    }

    public WorldTagRocation convertRocation() {
        return new WorldTagRocation(worldTag, position, Vector3d.ZERO);
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

package net.cupmouse.minecraft.worlds;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.reflect.TypeToken;
import net.cupmouse.minecraft.Utilities;
import net.cupmouse.minecraft.util.WorldNotFoundException;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.Vector;

public class WorldTagLocation {
    public final WorldTag worldTag;
    public final Vector3d position;

    public WorldTagLocation(WorldTag worldTag, Vector3d position) {
        this.worldTag = worldTag;
        this.position = position;
    }

    public boolean teleportHere(Entity entity) throws WorldNotFoundException {
        return entity.setLocation(convertLocation());
    }

    public Location<World> convertLocation() throws WorldNotFoundException {
        Optional<World> taggedWorld = WorldTagModule.getTaggedWorld(worldTag);

        if (!taggedWorld.isPresent()) {
            throw new WorldNotFoundException();
        }

        return new Location<World>(taggedWorld.get(), position);
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

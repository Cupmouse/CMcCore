package net.cupmouse.minecraft.worlds;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.reflect.TypeToken;
import net.cupmouse.minecraft.Utilities;
import net.cupmouse.minecraft.util.WorldNotFoundException;
import net.cupmouse.minecraft.worlds.WorldTag;
import net.cupmouse.minecraft.worlds.WorldTagLocation;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.spongepowered.api.entity.Entity;

import javax.xml.stream.events.EndElement;
import java.lang.reflect.Type;

public final class WorldTagRocation extends WorldTagLocation {

    public final Vector3d rotation;

    public WorldTagRocation(WorldTag worldTag, Vector3d position, Vector3d rotation) {
        super(worldTag, position);
        this.rotation = rotation;
    }

    @Override
    public boolean teleportHere(Entity entity) throws WorldNotFoundException {
        return entity.setLocationAndRotation(convertLocation(), rotation);
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

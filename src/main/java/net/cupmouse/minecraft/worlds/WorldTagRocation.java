package net.cupmouse.minecraft.worlds;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.reflect.TypeToken;
import net.cupmouse.minecraft.Utilities;
import net.cupmouse.minecraft.worlds.WorldTag;
import net.cupmouse.minecraft.worlds.WorldTagLocation;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

import java.lang.reflect.Type;

public final class WorldTagRocation {

    public final WorldTagLocation tagLocation;
    public final Vector3d rotation;

    public WorldTagRocation(WorldTag worldTag, Vector3d position, Vector3d rotation) {
        this.tagLocation = new WorldTagLocation(worldTag, position);
        this.rotation = rotation;
    }

    public WorldTagRocation(WorldTagLocation tagLocation, Vector3d rotation) {
        this.tagLocation = tagLocation;
        this.rotation = rotation;
    }

    static class Serializer implements TypeSerializer<WorldTagRocation> {

        @Override
        public WorldTagRocation deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
            WorldTagLocation tagLocation = value.getValue(TypeToken.of(WorldTagLocation.class));
            Vector3d rotation = value.getNode("rotation").getValue(TypeToken.of(Vector3d.class));

            return new WorldTagRocation(tagLocation, rotation);
        }

        @Override
        public void serialize(TypeToken<?> type, WorldTagRocation obj, ConfigurationNode value) throws ObjectMappingException {
            value.setValue(TypeToken.of(WorldTagLocation.class), obj.tagLocation);
            value.getNode("rotation").setValue(TypeToken.of(Vector3d.class), obj.rotation);
        }
    }
}

package net.cupmouse.minecraft.worlds;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

import java.util.Objects;

public final class WorldTag {

    private String name;

    private WorldTag(String name) {
        this.name = name;
    }

    public String getTagName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldTag worldTag = (WorldTag) o;
        return Objects.equals(name, worldTag.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public static WorldTag byName(String name) {
        return new WorldTag(name);
    }

    static class Serializer implements TypeSerializer<WorldTag> {

        @Override
        public WorldTag deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
            String name = value.getNode("name").getString();

            return byName(name);
        }

        @Override
        public void serialize(TypeToken<?> type, WorldTag obj, ConfigurationNode value) throws ObjectMappingException {
            value.getNode("name").setValue(obj.getTagName());
        }
    }
}

package net.cupmouse.minecraft.worlds;

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
}

package net.cupmouse.minecraft.worlds;

import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

public abstract class WorldTagPosition {

    public final WorldTag worldTag;
    public final Vector3d position;

    protected WorldTagPosition(WorldTag worldTag, Vector3d position) {
        this.worldTag = worldTag;
        this.position = position;
    }

    public final boolean teleportHere(Entity entity) {
        Optional<Location<World>> spongeLocationOptional = convertSponge();
        return spongeLocationOptional.filter(entity::setLocation).isPresent();
    }

    public final Optional<Location<World>> convertSponge() {
        return WorldTagModule.getTaggedWorld(worldTag).map(world -> new Location<>(world, position));
    }

    public final WorldTag getWorldTag() {
        return worldTag;
    }

    public final Vector3d getPosition() {
        return position;
    }

    public abstract WorldTagPosition worldTag(WorldTag worldTag);

    public abstract WorldTagPosition position(Vector3d position);


    /**
     * WorldTagは違っていてもこの関数を呼んだインスタンスの方が採用されます
     * @param basePosition
     * @return
     */
    public WorldTagPosition relativeBase(WorldTagPosition basePosition) {
        return position(position.add(basePosition.position));
    }

    public WorldTagPosition relativeBasePoint(Vector3d basePoint) {
        return position(position.add(basePoint));
    }

    /**
     * WorldTagは違っていてもこの関数を呼んだインスタンスの方が採用されます
     *
     * @param basePosition
     * @return
     */
    public WorldTagPosition relativeTo(WorldTagPosition basePosition) {
        return position(position.sub(basePosition.position));
    }

    public WorldTagPosition relativeToPoint(Vector3d basePoint) {
        return position(position.sub(basePoint));
    }

}

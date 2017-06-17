package net.cupmouse.minecraft.worlds;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import net.cupmouse.minecraft.util.UnknownWorldException;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public interface WorldTagPosition {

    boolean teleportHere(Entity entity) throws UnknownWorldException;

    Location<World> convertSponge() throws UnknownWorldException;

    WorldTag getWorldTag();

    Vector3d getPosition();

    WorldTagPosition relativeBasePoint(Vector3i basePoint);
}

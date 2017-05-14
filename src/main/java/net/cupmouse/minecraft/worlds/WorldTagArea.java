package net.cupmouse.minecraft.worlds;

import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public abstract class WorldTagArea {

    public final WorldTag worldTag;

    protected WorldTagArea(WorldTag worldTag) {
        this.worldTag = worldTag;
    }

    public boolean isInArea(Location<World> location) {
        return WorldTagModule.isThis(worldTag, location.getExtent())
                && isInArea(location.getPosition());
    }

    /**
     * ワールド成分は無視してポジションだけでエリア内にいるか確認します。
     * @param position
     * @return
     */
    public abstract boolean isInArea(Vector3d position);

    public abstract BlockLocSequence getOutlineBlocks();

    public abstract BlockLocSequence getCornerBlocks();
}
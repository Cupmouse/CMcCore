package net.cupmouse.minecraft.worlds;

import com.flowpowered.math.vector.Vector3i;

import java.util.Collection;
import java.util.Collections;

public class BlockLocSequence {

    public final WorldTag worldTag;
    public final Collection<Vector3i> blockLocs;

    public BlockLocSequence(WorldTag worldTag, Collection<Vector3i> blockLocs) {
        this.worldTag = worldTag;
        this.blockLocs = Collections.unmodifiableCollection(blockLocs);
    }
}

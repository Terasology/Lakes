// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.Lakes;

import org.terasology.engine.math.Region3i;
import org.terasology.engine.world.generation.Border3D;
import org.terasology.engine.world.generation.facets.SurfaceHeightFacet;
import org.terasology.math.geom.BaseVector2i;

/**
 * Marks how deep a lake would be at given world coordinates (x, z).
 * <p>
 * The lake depth is given as positive floating point value which should be interpreted as depth from actual surface
 * height.
 */
public class LakeDepthFacet extends SurfaceHeightFacet {

    public LakeDepthFacet(Region3i targetRegion, Border3D border) {
        super(targetRegion, border);
    }

    public int getWorldIndex(BaseVector2i pos) {
        return getWorldIndex(pos.x(), pos.y());
    }
}

// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.Lakes;

import org.terasology.engine.math.Region3i;
import org.terasology.engine.world.generation.Border3D;
import org.terasology.engine.world.generation.facets.base.BaseFieldFacet2D;
import org.terasology.math.geom.BaseVector2i;

/**
 * Marks the amount of empty space, e.g. air blocks, there will be over lake surface should there be some in the
 * particular xz coordinates.
 */
public class LakeHeightFacet extends BaseFieldFacet2D {

    public LakeHeightFacet(Region3i targetRegion, Border3D border) {
        super(targetRegion, border);
    }


    public int getWorldIndex(BaseVector2i pos) {
        return getWorldIndex(pos.x(), pos.y());
    }
}

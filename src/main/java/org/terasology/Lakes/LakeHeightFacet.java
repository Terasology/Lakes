/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.Lakes;

import org.terasology.math.Region3i;
import org.terasology.math.geom.BaseVector2i;
import org.terasology.world.generation.Border3D;
import org.terasology.world.generation.facets.SurfaceHeightFacet;
import org.terasology.world.generation.facets.base.BaseFieldFacet2D;

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

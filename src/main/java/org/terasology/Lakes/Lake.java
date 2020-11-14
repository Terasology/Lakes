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

import org.terasology.math.geom.Rect2i;
import org.terasology.math.geom.Vector3i;
import org.terasology.utilities.procedural.Noise;
import org.terasology.utilities.procedural.WhiteNoise;
import org.terasology.world.block.Block;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.Set;

public class Lake implements Iterable<Vector3i> {
    public static final int MAX_RADIUS = 15;
    public static final int MAX_DEPTH = 10;

    public final int surfaceHeight;
    public final Block liquid;
    private Set<Vector3i> content;

    public Lake(int surfaceHeight, Set<Vector3i> content, Block liquid) {
        this.surfaceHeight = surfaceHeight;
        this.content = content;
        this.liquid = liquid;
    }

    @Override
    public Iterator<Vector3i> iterator() {
        return content.iterator();
    }
}

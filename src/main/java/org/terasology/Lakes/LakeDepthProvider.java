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

import org.terasology.math.geom.BaseVector2i;
import org.terasology.math.geom.Rect2i;
import org.terasology.math.geom.Vector2f;
import org.terasology.utilities.procedural.BrownianNoise;
import org.terasology.utilities.procedural.SimplexNoise;
import org.terasology.utilities.procedural.SubSampledNoise;
import org.terasology.world.generation.Border3D;
import org.terasology.world.generation.FacetProviderPlugin;
import org.terasology.world.generation.GeneratingRegion;
import org.terasology.world.generation.Produces;
import org.terasology.world.generator.plugin.RegisterPlugin;

@RegisterPlugin
@Produces(LakeDepthFacet.class)
public class LakeDepthProvider implements FacetProviderPlugin {

    private SubSampledNoise surfaceNoise1;
    private SubSampledNoise surfaceNoise2;
    private SubSampledNoise surfaceNoise3;

    @Override
    public void setSeed(long seed) {
        surfaceNoise1 = new SubSampledNoise(new SimplexNoise(seed / 2), new Vector2f(0.02f, 0.02f), 1);
        surfaceNoise2 = new SubSampledNoise(new BrownianNoise(new SimplexNoise(seed / 8), 8), new Vector2f(0.005f,
                0.005f), 1);
        surfaceNoise3 = new SubSampledNoise(new SimplexNoise(seed / 4), new Vector2f(0.002f, 0.002f), 1);
    }

    @Override
    public void process(GeneratingRegion region) {

        Border3D border = region.getBorderForFacet(LakeDepthFacet.class);
        LakeDepthFacet facet = new LakeDepthFacet(region.getRegion(), border);
        Rect2i processRegion = facet.getWorldRegion();

        float[] sNoise1Values = surfaceNoise1.noise(processRegion);
        float[] sNoise2Values = surfaceNoise2.noise(processRegion);
        float[] sNoise3Values = surfaceNoise3.noise(processRegion);

        for (BaseVector2i position : processRegion.contents()) {

            float noiseValue1 = sNoise1Values[facet.getWorldIndex(position)];
            float noiseValue2 = sNoise2Values[facet.getWorldIndex(position)];
            float noiseValue3 = sNoise3Values[facet.getWorldIndex(position)];

            facet.setWorld(position, 4 * Math.abs(noiseValue1 + noiseValue2 + noiseValue3));

        }

        region.setRegionFacet(LakeDepthFacet.class, facet);
    }
}

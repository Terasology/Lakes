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

import org.terasology.entitySystem.Component;
import org.terasology.math.Region3i;
import org.terasology.math.geom.Vector3i;
import org.terasology.nui.properties.Checkbox;
import org.terasology.utilities.procedural.Noise;
import org.terasology.utilities.procedural.WhiteNoise;
import org.terasology.world.generation.Border3D;
import org.terasology.world.generation.ConfigurableFacetProvider;
import org.terasology.world.generation.Facet;
import org.terasology.world.generation.FacetBorder;
import org.terasology.world.generation.FacetProviderPlugin;
import org.terasology.world.generation.GeneratingRegion;
import org.terasology.world.generation.Produces;
import org.terasology.world.generation.Requires;
import org.terasology.world.generation.facets.SurfaceHeightFacet;
import org.terasology.world.generator.plugin.RegisterPlugin;

@RegisterPlugin
@Produces(LavaFacet.class)
@Requires(@Facet(value = SurfaceHeightFacet.class, border = @FacetBorder(sides =
        Lake.MAX_RADIUS + Lake.MAX_LENGTH_OUTER + 1)))
public class LavaProvider extends LakeProvider implements FacetProviderPlugin, ConfigurableFacetProvider {
    private LavaFacetProviderConfiguration configuration = new LavaFacetProviderConfiguration();

    private Noise noise;

    @Override
    public void setSeed(long seed) {
        noise = new WhiteNoise(seed * 73946);
    }

    @Override
    public void process(GeneratingRegion region) {
        boolean enabled = configuration.enabled;

        SurfaceHeightFacet surfaceHeightFacet = region.getRegionFacet(SurfaceHeightFacet.class);

        Border3D border = region.getBorderForFacet(LakeFacet.class);
        //Extend border by max radius + max length + max outer length and max lakedepth
        border = border.extendBy(Lake.MAX_DEPTH, Lake.MAX_DEPTH, Lake.MAX_RADIUS + Lake.MAX_LENGTH_OUTER);
        LavaFacet lakes = new LavaFacet(region.getRegion(), border);
        Region3i processRegion = lakes.getWorldRegion();

        lakes.setEnabled(enabled);

        if (enabled) {

            Vector3i min = processRegion.min();
            int step = 3;
            Vector3i start = new Vector3i(
                    (min.x() + (step - Math.floorMod(min.x(), step))),
                    min.y() + (step - Math.floorMod(min.y(), step)),
                    min.z() + (step - Math.floorMod(min.z(), step))
            );

            for (int wy = start.y(); wy < processRegion.maxY(); wy += step) {
                for (int wx = start.x(); wx < processRegion.maxX(); wx += step) {
                    for (int wz = start.z(); wz < processRegion.maxZ(); wz += step) {
                        Vector3i pos = new Vector3i(wx, wy, wz);
                        float noiseValue = noise.noise(pos.x() * 0.3f, pos.y() * 0.3f, pos.z() * 0.3f);
                        float sHeight = surfaceHeightFacet.getWorld(pos.x(), pos.z());

                        if (pos.y() < sHeight - 40
                                && noiseValue > 0.9999 - Math.log(1 + (sHeight - pos.y()) / 4) * 0.00001) {

                            lakes.add(new Lake(pos, 10 + 20 * Math.abs(Math.round(noise.noise(pos.x(), pos.z())))));

                        } else if (pos.y() == Math.round(sHeight) && noiseValue > 0.999995
                                && checkGradient(pos, surfaceHeightFacet)) {

                            Lake temp = new Lake(pos, 10 + 20 * Math.abs(Math.round(noise.noise(pos.x(), pos.z()))));

                            if (checkCorners(temp.getBoundingBox(), surfaceHeightFacet)) {

                                int minHeight = getMinimumHeight(temp.getBoundingBox(), surfaceHeightFacet);

                                if (minHeight < pos.y()) {
                                    temp.setWaterHeight(minHeight);
                                }

                                lakes.add(temp);

                            }

                        }

                    }
                }
            }
        }


        region.setRegionFacet(LavaFacet.class, lakes);
    }

    @Override
    public String getConfigurationName() {
        return "Lava Lakes";
    }

    @Override
    public Component getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(Component configuration) {
        this.configuration = (LavaFacetProviderConfiguration) configuration;
    }

    private static class LavaFacetProviderConfiguration implements Component {
        @Checkbox(description = "Enable Lava Lakes")
        public boolean enabled = true;
    }
}

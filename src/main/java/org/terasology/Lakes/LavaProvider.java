// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.Lakes;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.math.Region3i;
import org.terasology.engine.utilities.procedural.Noise;
import org.terasology.engine.utilities.procedural.WhiteNoise;
import org.terasology.engine.world.generation.Border3D;
import org.terasology.engine.world.generation.ConfigurableFacetProvider;
import org.terasology.engine.world.generation.Facet;
import org.terasology.engine.world.generation.FacetBorder;
import org.terasology.engine.world.generation.FacetProviderPlugin;
import org.terasology.engine.world.generation.GeneratingRegion;
import org.terasology.engine.world.generation.Produces;
import org.terasology.engine.world.generation.Requires;
import org.terasology.engine.world.generation.facets.SurfaceHeightFacet;
import org.terasology.engine.world.generator.plugin.RegisterPlugin;
import org.terasology.math.geom.Vector3i;
import org.terasology.nui.properties.Checkbox;

@RegisterPlugin
@Produces(LavaFacet.class)
@Requires(@Facet(value = SurfaceHeightFacet.class, border = @FacetBorder(sides =
        Lake.MAX_RADIUS + Lake.MAX_LENGTH_OUTER + 1)))
public class LavaProvider extends LakeProvider implements FacetProviderPlugin, ConfigurableFacetProvider {

    private static final int SKIP_BLOCKS = 3;
    private static final float UNDERGROUND_LAVA_LAKES_SAMPLING_CONSTANT = 0.3f;

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
            Vector3i start = new Vector3i(
                    (min.x() + (SKIP_BLOCKS - Math.floorMod(min.x(), SKIP_BLOCKS))),
                    min.y() + (SKIP_BLOCKS - Math.floorMod(min.y(), SKIP_BLOCKS)),
                    min.z() + (SKIP_BLOCKS - Math.floorMod(min.z(), SKIP_BLOCKS))
            );

            for (int wy = start.y(); wy < processRegion.maxY(); wy += SKIP_BLOCKS) {
                for (int wx = start.x(); wx < processRegion.maxX(); wx += SKIP_BLOCKS) {
                    for (int wz = start.z(); wz < processRegion.maxZ(); wz += SKIP_BLOCKS) {
                        Vector3i pos = new Vector3i(wx, wy, wz);
                        float noiseValue = noise.noise(
                                pos.x() * UNDERGROUND_LAVA_LAKES_SAMPLING_CONSTANT,
                                pos.y() * UNDERGROUND_LAVA_LAKES_SAMPLING_CONSTANT,
                                pos.z() * UNDERGROUND_LAVA_LAKES_SAMPLING_CONSTANT
                        );
                        float sHeight = surfaceHeightFacet.getWorld(pos.x(), pos.z());

                        if (pos.y() < sHeight - 40
                                && noiseValue > 0.9999 - Math.log(1 + (sHeight - pos.y()) / 4) * 0.00001) {

                            lakes.add(new Lake(pos, Lake.MIN_VERTICES +
                                    Math.round((Lake.MAX_VERTICES - Lake.MIN_VERTICES) * Math.abs(noise.noise(pos.x()
                                            , pos.z())))
                            ));

                        } else if (pos.y() == Math.round(sHeight) && noiseValue > 0.999995
                                && checkGradient(pos, surfaceHeightFacet)) {

                            Lake temp = new Lake(pos, Lake.MIN_VERTICES +
                                    Math.round((Lake.MAX_VERTICES - Lake.MIN_VERTICES) * Math.abs(noise.noise(pos.x()
                                            , pos.z())))
                            );

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

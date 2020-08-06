// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.Lakes;

import org.terasology.core.world.CoreBiome;
import org.terasology.core.world.generator.facets.BiomeFacet;
import org.terasology.core.world.generator.facets.TreeFacet;
import org.terasology.core.world.generator.trees.TreeGenerator;
import org.terasology.core.world.generator.trees.Trees;
import org.terasology.math.TeraMath;
import org.terasology.math.geom.Rect2i;
import org.terasology.math.geom.Vector3i;
import org.terasology.utilities.procedural.Noise;
import org.terasology.utilities.procedural.WhiteNoise;
import org.terasology.world.generation.Border3D;
import org.terasology.world.generation.Facet;
import org.terasology.world.generation.FacetBorder;
import org.terasology.world.generation.FacetProviderPlugin;
import org.terasology.world.generation.GeneratingRegion;
import org.terasology.world.generation.Produces;
import org.terasology.world.generation.Requires;
import org.terasology.world.generation.Updates;
import org.terasology.world.generation.facets.SurfaceHeightFacet;
import org.terasology.world.generator.plugin.RegisterPlugin;

@RegisterPlugin
@Requires({
        @Facet(value = SurfaceHeightFacet.class, border = @FacetBorder(sides = Trees.MAXRADIUS)),
        @Facet(value = LakeFacet.class, border = @FacetBorder(sides = Trees.MAXRADIUS)),
        @Facet(value = BiomeFacet.class, border = @FacetBorder(sides = Trees.MAXRADIUS))
//        @Facet(value = TreeFacet.class, border = @FacetBorder(sides = Trees.MAXRADIUS))
})
@Updates(@Facet(TreeFacet.class))
public class LakeTreeProvider implements FacetProviderPlugin {
    private Noise noise;

    @Override
    public void process(GeneratingRegion region) {
//        Border3D borderForTreeFacet = region.getBorderForFacet(TreeFacet.class);
        TreeFacet treeFacet = region.getRegionFacet(TreeFacet.class);

        SurfaceHeightFacet surfaceHeightFacet = region.getRegionFacet(SurfaceHeightFacet.class);
        Rect2i worldRegion = surfaceHeightFacet.getWorldRegion();
        LakeFacet lakeFacet = region.getRegionFacet(LakeFacet.class);
        BiomeFacet biomeFacet = region.getRegionFacet(BiomeFacet.class);

        for (int wz = worldRegion.minY(); wz <= worldRegion.maxY(); wz++) {
            for (int wx = worldRegion.minX(); wx <= worldRegion.maxX(); wx++) {
                int surfaceHeight = TeraMath.floorToInt(surfaceHeightFacet.getWorld(wx, wz));
                Vector3i pos = new Vector3i(wx, surfaceHeight, wz);
                if (treeFacet.getWorldRegion().encompasses(pos)) {
                    if (biomeFacet.getWorld(wx, wz).getId().equals(CoreBiome.DESERT.getId())) {

                        Lake lake = lakeFacet.getNearestLake(pos);
                        if (lake.isNotNull() && lake.getOrigin().y > surfaceHeight - 10 && noise.noise(wx, wz) > 0.99) {
                            if (lake.OuterContains(pos)) {
                                TreeGenerator tree = Trees.oakTree();
                                treeFacet.setWorld(wx, surfaceHeight, wz, tree);
                            }
                        }
                    }
                }
            }
        }

//        region.setRegionFacet(TreeFacet.class, treeFacet);
    }

    @Override
    public void setSeed(long seed) {
        noise = new WhiteNoise(seed + 43);
    }
}

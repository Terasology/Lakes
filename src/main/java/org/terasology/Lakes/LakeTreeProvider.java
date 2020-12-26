// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.Lakes;

import org.joml.Vector3i;
import org.terasology.core.world.CoreBiome;
import org.terasology.core.world.generator.facets.BiomeFacet;
import org.terasology.core.world.generator.facets.TreeFacet;
import org.terasology.core.world.generator.trees.Trees;
import org.terasology.paradice.trees.GenericTrees;
import org.terasology.utilities.procedural.WhiteNoise;
import org.terasology.world.generation.Facet;
import org.terasology.world.generation.FacetBorder;
import org.terasology.world.generation.FacetProviderPlugin;
import org.terasology.world.generation.GeneratingRegion;
import org.terasology.world.generation.Requires;
import org.terasology.world.generation.Updates;
import org.terasology.world.generation.facets.SurfacesFacet;
import org.terasology.world.generator.plugin.RegisterPlugin;

@RegisterPlugin
@Requires({
        @Facet(value = SurfacesFacet.class, border = @FacetBorder(sides = Trees.MAXRADIUS)),
        @Facet(value = LakeFacet.class, border = @FacetBorder(sides = Trees.MAXRADIUS + LakeTreeProvider.SCATTER, bottom = 5)),
        @Facet(value = BiomeFacet.class, border = @FacetBorder(sides = Trees.MAXRADIUS + LakeTreeProvider.SCATTER, bottom = 5))
})
@Updates(@Facet(TreeFacet.class))
public class LakeTreeProvider implements FacetProviderPlugin {
    public static final int SCATTER = 4;
    private WhiteNoise noise;

    @Override
    public void process(GeneratingRegion region) {
        TreeFacet treeFacet = region.getRegionFacet(TreeFacet.class);

        SurfacesFacet surfacesFacet = region.getRegionFacet(SurfacesFacet.class);
        LakeFacet lakeFacet = region.getRegionFacet(LakeFacet.class);
        BiomeFacet biomeFacet = region.getRegionFacet(BiomeFacet.class);

        for (Lake lake : lakeFacet.getLakes()) {
            for (Vector3i lakePos : lake) {
                if (
                    !biomeFacet.getWorldRegion().contains(lakePos.x, lakePos.z)
                    || !biomeFacet.getWorld(lakePos.x, lakePos.z).getId().equals(CoreBiome.DESERT.getId())
                    || noise.noise(lakePos.x, lakePos.y, lakePos.z) < 0.5
                ) {
                    continue;
                }
                Vector3i pos = new Vector3i(lakePos);
                pos.add(Math.floorMod(noise.intNoise(lakePos.x, lakePos.y + 1, lakePos.z), SCATTER * 2 + 1) - SCATTER,0,0);
                pos.add(0,Math.floorMod(noise.intNoise(lakePos.x, lakePos.y + 2, lakePos.z), SCATTER * 2 + 1) - SCATTER,0);
                pos.add(0,0,Math.floorMod(noise.intNoise(lakePos.x, lakePos.y + 3, lakePos.z), SCATTER * 2 + 1) - SCATTER);
                if (
                    surfacesFacet.getWorldRegion().contains(pos.x, pos.y - 1, pos.z)
                    && surfacesFacet.getWorld(pos.x, pos.y - 1, pos.z)
                    && treeFacet.getWorldRegion().contains(pos)) {
                    treeFacet.setWorld(pos, GenericTrees.palmTree());
                }
            }
        }
    }

    @Override
    public void setSeed(long seed) {
        noise = new WhiteNoise(seed + 43);
    }
}

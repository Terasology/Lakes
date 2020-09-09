// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.Lakes;

import org.terasology.coreworlds.CoreBiome;
import org.terasology.coreworlds.generator.facets.BiomeFacet;
import org.terasology.engine.math.Region3i;
import org.terasology.engine.utilities.procedural.Noise;
import org.terasology.engine.utilities.procedural.WhiteNoise;
import org.terasology.engine.world.generation.Border3D;
import org.terasology.engine.world.generation.Facet;
import org.terasology.engine.world.generation.FacetBorder;
import org.terasology.engine.world.generation.FacetProviderPlugin;
import org.terasology.engine.world.generation.GeneratingRegion;
import org.terasology.engine.world.generation.Produces;
import org.terasology.engine.world.generation.Requires;
import org.terasology.engine.world.generation.facets.SurfaceHeightFacet;
import org.terasology.engine.world.generation.facets.base.BaseFieldFacet2D;
import org.terasology.engine.world.generator.plugin.RegisterPlugin;
import org.terasology.gestalt.naming.Name;
import org.terasology.math.geom.Rect2i;
import org.terasology.math.geom.Vector2i;
import org.terasology.math.geom.Vector3i;

@RegisterPlugin
@Produces(LakeFacet.class)
@Requires({
        @Facet(value = SurfaceHeightFacet.class, border = @FacetBorder(sides = (Lake.MAX_SIZE) * 2)),
        @Facet(value = BiomeFacet.class, border = @FacetBorder(sides = (Lake.MAX_SIZE) * 2))
})
public class LakeProvider implements FacetProviderPlugin {
    private static final int SKIP_BLOCKS = 3;
    // Don't make any of these constants integer values, the noise functions returns only 0 or 1 for them
    private static final float SURFACE_LAKES_SAMPLING_CONSTANT = 0.9f;
    private static final float SURFACE_DESERT_LAKES_SAMPLING_CONSTANT = 0.3f;
    private static final float UNDERGROUND_LAKES_SAMPLING_CONSTANT = 0.3f;

    private Noise noise;

    @Override
    public void setSeed(long seed) {
        // to change the seed value
        noise = new WhiteNoise(seed * 3882);
    }

    @Override
    public void process(GeneratingRegion region) {
        SurfaceHeightFacet surfaceHeightFacet = region.getRegionFacet(SurfaceHeightFacet.class);

        Border3D border = region.getBorderForFacet(LakeFacet.class);
        //Extend border by max radius + max length + max outer length and max lakedepth
        border = border.extendBy(Lake.MAX_DEPTH, Lake.MAX_DEPTH, (Lake.MAX_SIZE));
        LakeFacet lakes = new LakeFacet(region.getRegion(), border);
        Region3i worldRegion = lakes.getWorldRegion();
        BiomeFacet biomeFacet = region.getRegionFacet(BiomeFacet.class);

        Vector3i min = worldRegion.min();
        Vector3i start = new Vector3i(
                (min.x() + (SKIP_BLOCKS - Math.floorMod(min.x(), SKIP_BLOCKS))),
                min.y() + (SKIP_BLOCKS - Math.floorMod(min.y(), SKIP_BLOCKS)),
                min.z() + (SKIP_BLOCKS - Math.floorMod(min.z(), SKIP_BLOCKS))
        );

        for (int wy = start.y(); wy < worldRegion.maxY(); wy += SKIP_BLOCKS) {
            for (int wx = start.x(); wx < worldRegion.maxX(); wx += SKIP_BLOCKS) {
                for (int wz = start.z(); wz < worldRegion.maxZ(); wz += SKIP_BLOCKS) {
                    // To any future onlooker, pos needs to be created a new Vector3i here or else the lakes
                    // don't expand beyond one chunk for reasons unknown. Tried doing pos.set(wx, wy, wz)
                    // and it didn't work - sin3point14
                    Vector3i pos = new Vector3i(wx, wy, wz);
                    float sHeight = surfaceHeightFacet.getWorld(pos.x(), pos.z());
                    if (pos.y() < sHeight - 20) {
                        // Underground Lakes
                        // If the sampling constant(0.3) is changed also adjust the number of '9's in the comparison
                        // same goes for other places noise has been sampled
                        if (noise.noise(
                                pos.x() * UNDERGROUND_LAKES_SAMPLING_CONSTANT,
                                pos.y() * UNDERGROUND_LAKES_SAMPLING_CONSTANT,
                                pos.z() * UNDERGROUND_LAKES_SAMPLING_CONSTANT
                        ) > 0.9999
                        ) {
                            lakes.add(new Lake(pos, Lake.MIN_VERTICES +
                                    Math.round((Lake.MAX_VERTICES - Lake.MIN_VERTICES) * Math.abs(noise.noise(pos.x()
                                            , pos.z())))
                            ));
                        }
                    } else if (pos.y() == Math.round(sHeight) && checkGradient(pos,
                            surfaceHeightFacet)) {
                        // Surface Lakes
                        if (computeProbability(pos, biomeFacet) > 0.99) {
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
        region.setRegionFacet(LakeFacet.class, lakes);
    }

    private float computeProbability(Vector3i pos, BiomeFacet biomeFacet) {
        float probability;
        Name biomeID = biomeFacet.getWorld(pos.x(), pos.z()).getId();
        if (biomeID.equals(CoreBiome.DESERT.getId())) {
            probability = noise.noise(
                    pos.x() * SURFACE_DESERT_LAKES_SAMPLING_CONSTANT,
                    pos.y() * SURFACE_DESERT_LAKES_SAMPLING_CONSTANT,
                    pos.z() * SURFACE_DESERT_LAKES_SAMPLING_CONSTANT
            );
        } else {
            probability = noise.noise(
                    pos.x() * SURFACE_LAKES_SAMPLING_CONSTANT,
                    pos.y() * SURFACE_LAKES_SAMPLING_CONSTANT,
                    pos.z() * SURFACE_LAKES_SAMPLING_CONSTANT
            );

        }
        return probability;
    }

    protected boolean checkGradient(Vector3i pos, BaseFieldFacet2D facet) {

        Rect2i boundingBox = Rect2i.createFromMinAndMax(pos.x() - 3, pos.z() - 3, pos.x() + 3, pos.z() + 3);

        if (facet.getWorldRegion().contains(boundingBox)) {
            float xDiff = Math.abs(facet.getWorld(pos.x() + 3, pos.z()) - facet.getWorld(pos.x() - 3, pos.z()));
            float yDiff = Math.abs(facet.getWorld(pos.x(), pos.z() + 3) - facet.getWorld(pos.x(), pos.z() - 3));
            float xyDiff = Math.abs(facet.getWorld(pos.x() + 3, pos.z() + 3) - facet.getWorld(pos.x() - 3,
                    pos.z() - 3));
            return !(xDiff > 2) && !(yDiff > 2) && !(xyDiff > 2);
        }

        return false;
    }

    protected boolean checkCorners(Rect2i boundingBox, BaseFieldFacet2D facet) {
        Vector2i max = boundingBox.max();
        Vector2i min = boundingBox.min();
        if (facet.getWorldRegion().contains(boundingBox)) {
            float[] corners = new float[4];
            corners[0] = facet.getWorld(max);
            corners[1] = facet.getWorld(min);
            corners[2] = facet.getWorld(min.x() + boundingBox.sizeX(), min.y());
            corners[3] = facet.getWorld(min.x(), min.y() + boundingBox.sizeY());
            for (int i = 0; i < corners.length; i++) {
                for (int j = 0; j < corners.length; j++) {
                    if (Math.abs(corners[i] - corners[j]) > 4) {
                        return false;
                    }
                }
            }

        }
        return true;
    }

    protected int getMinimumHeight(Rect2i boundingBox, BaseFieldFacet2D facet) {
        boundingBox = facet.getWorldRegion().intersect(boundingBox);
        Vector2i max = boundingBox.max();
        Vector2i min = boundingBox.min();
        int minHeight = Integer.MAX_VALUE;
        float[] corners = new float[4];
        corners[0] = facet.getWorld(max);
        corners[1] = facet.getWorld(min);
        corners[2] = facet.getWorld(max.x(), min.y());
        corners[3] = facet.getWorld(min.x(), max.y());
        for (float corner : corners) {
            if (corner < minHeight) {
                minHeight = Math.round(corner);
            }
        }
        return minHeight;
    }
}

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

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.terasology.engine.math.Direction;
import org.terasology.engine.registry.CoreRegistry;
import org.terasology.engine.utilities.procedural.BrownianNoise;
import org.terasology.engine.utilities.procedural.Noise;
import org.terasology.engine.utilities.procedural.SimplexNoise;
import org.terasology.engine.utilities.procedural.SubSampledNoise;
import org.terasology.engine.utilities.procedural.WhiteNoise;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.engine.world.generation.Border3D;
import org.terasology.engine.world.generation.Facet;
import org.terasology.engine.world.generation.FacetBorder;
import org.terasology.engine.world.generation.FacetProviderPlugin;
import org.terasology.engine.world.generation.GeneratingRegion;
import org.terasology.engine.world.generation.Produces;
import org.terasology.engine.world.generation.Requires;
import org.terasology.engine.world.generation.Updates;
import org.terasology.engine.world.generation.facets.DensityFacet;
import org.terasology.engine.world.generation.facets.ElevationFacet;
import org.terasology.engine.world.generation.facets.SeaLevelFacet;
import org.terasology.engine.world.generation.facets.SurfacesFacet;
import org.terasology.engine.world.generator.plugin.RegisterPlugin;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;


/**
 * Try to place lakes in random locations on the surface and underground.
 * The depth of each lake is determined by a random noise field added to a random-width paraboloid.
 * The points where the depth is <= 0 are where the lake stops.
 * If the surface in that region is missing, or too steep, or under various other conditions,
 * the provider gives up on generating that lake, and doesn't place it.
 *
 * Underground lakes are generated similarly, but in 3D.
 * If they breach the surface, they're cancelled.
 * The upper part of their cave is filled with air.
 * If they're sufficiently far underground, they're lava instead of water.
 */
@RegisterPlugin
@Produces(LakeFacet.class)
@Updates({
        @Facet(value = SurfacesFacet.class, border = @FacetBorder(sides = Lake.MAX_RADIUS * 3, bottom = Lake.MAX_DEPTH + 3, top = Lake.MAX_DEPTH + 3)),
        @Facet(value = DensityFacet.class, border = @FacetBorder(sides = Lake.MAX_RADIUS * 3, bottom = Lake.MAX_DEPTH + 3, top = Lake.MAX_DEPTH + 3))
})
@Requires({
    @Facet(value = ElevationFacet.class, border = @FacetBorder(sides = Lake.MAX_RADIUS * 2)),
    @Facet(value = SeaLevelFacet.class)
})
public class LakeProvider implements FacetProviderPlugin {
    private static final int SKIP_BLOCKS = 3;
    private static final float SURFACE_FREQUENCY = 0.0003f;
    private static final float SURFACE_EFFECTIVE_FREQUENCY = SURFACE_FREQUENCY * SKIP_BLOCKS * SKIP_BLOCKS * SKIP_BLOCKS;
    private static final float UNDERGROUND_FREQUENCY = 0.000001f;
    private static final float UNDERGROUND_EFFECTIVE_FREQUENCY = UNDERGROUND_FREQUENCY * SKIP_BLOCKS * SKIP_BLOCKS * SKIP_BLOCKS;
    private static final float SURFACE_LAKE_IRREGULARITY = 1.3f;
    private static final float UNDERGROUND_LAKE_IRREGULARITY = 1.3f;

    Block water;
    Block lava;

    private WhiteNoise noise;
    private Noise depthModifyingNoise;

    @Override
    public void setSeed(long seed) {
        // to change the seed value
        noise = new WhiteNoise(seed * 3882);
        depthModifyingNoise = new SubSampledNoise(new BrownianNoise(new SimplexNoise(seed * 3883), 2), new Vector3f(0.05f, 0.05f, 0.05f), 1);
        BlockManager blockManager = CoreRegistry.get(BlockManager.class);
        water = blockManager.getBlock("CoreAssets:Water");
        lava = blockManager.getBlock("CoreAssets:Lava");
    }

    @Override
    public void process(GeneratingRegion region) {
        SurfacesFacet surfacesFacet = region.getRegionFacet(SurfacesFacet.class);
        DensityFacet densityFacet = region.getRegionFacet(DensityFacet.class);
        ElevationFacet elevationFacet = region.getRegionFacet(ElevationFacet.class);
        SeaLevelFacet seaLevelFacet = region.getRegionFacet(SeaLevelFacet.class);

        Border3D border = region.getBorderForFacet(LakeFacet.class);
        LakeFacet facet = new LakeFacet(region.getRegion(), border);

        Vector3i min = facet.getWorldRegion().getMin(new Vector3i());
        Vector3i start = new Vector3i(
                Math.floorDiv(min.x, SKIP_BLOCKS) * SKIP_BLOCKS,
                Math.floorDiv(min.y, SKIP_BLOCKS) * SKIP_BLOCKS,
                Math.floorDiv(min.z, SKIP_BLOCKS) * SKIP_BLOCKS
        );

        for (int wx0 = start.x; wx0 <= facet.getWorldRegion().maxX(); wx0 += SKIP_BLOCKS) {
            for (int wz0 = start.z; wz0 <= facet.getWorldRegion().maxZ(); wz0 += SKIP_BLOCKS) {
                // underground lakes
                for (int wy0 = start.y; wy0 <= facet.getWorldRegion().maxY(); wy0 += SKIP_BLOCKS) {
                    if (Math.abs(noise.noise(wx0, wz0, wy0)) < UNDERGROUND_EFFECTIVE_FREQUENCY) {
                        int wx = wx0 + Math.floorMod(noise.intNoise(wx0, wy0, wz0 + 1), SKIP_BLOCKS);
                        int wy = wy0 + Math.floorMod(noise.intNoise(wx0, wy0, wz0 + 2), SKIP_BLOCKS);
                        int wz = wz0 + Math.floorMod(noise.intNoise(wx0, wy0, wz0 + 3), SKIP_BLOCKS);
                        if (!elevationFacet.getWorldArea().contains(wx, wz) || !densityFacet.getWorldRegion().contains(wx, wy, wz)) {
                            continue;
                        }
                        float depth = elevationFacet.getWorld(wx, wz) - wy;
                        if (depth > 20 && densityFacet.getWorld(wx, wy, wz) > 0) {
                            generateUndergroundLake(new Vector3i(wx, wy, wz), facet, densityFacet, depth);
                        }
                    }
                }

                // surface lakes
                int wx = wx0 + Math.floorMod(noise.intNoise(wx0, wz0, 0), SKIP_BLOCKS);
                int wz = wz0 + Math.floorMod(noise.intNoise(wx0, wz0, 1), SKIP_BLOCKS);
                if (!surfacesFacet.getWorldRegion().contains(wx, surfacesFacet.getWorldRegion().minY(), wz)) {
                    continue;
                }
                for (int wy : surfacesFacet.getWorldColumn(wx, wz)) {
                    if (Math.abs(noise.noise(wx, wy, wz)) < SURFACE_EFFECTIVE_FREQUENCY) {
                        generateSurfaceLake(new Vector3i(wx, wy, wz), facet, surfacesFacet, densityFacet, seaLevelFacet.getSeaLevel());
                    }
                }
            }
        }
        region.setRegionFacet(LakeFacet.class, facet);
    }

    private void generateUndergroundLake(Vector3i origin, LakeFacet facet, DensityFacet densityFacet, float distanceBelowGround) {
        float width = square(noise.noise(origin.x, origin.y, origin.z + 4)) * (Lake.MAX_RADIUS - 3) + 3;
        float depth = Math.abs(noise.noise(origin.x, origin.y, origin.z + 5)) * (width / 2 - 2) + 2;
        Set<Vector3i> content = new HashSet<>();
        Queue<Vector3i> frontier = new ArrayDeque<>();
        frontier.add(origin);
        while (!frontier.isEmpty()) {
            Vector3i pos = frontier.remove();
            if (content.contains(pos)) {
                continue;
            }
            float lakeness = 1 + UNDERGROUND_LAKE_IRREGULARITY * depthModifyingNoise.noise(pos.x, pos.y, pos.z)
                - square((pos.x - origin.x) / width)
                - square((pos.y - origin.y) / depth)
                - square((pos.z - origin.z) / width);
            if (lakeness > 0) {
                if (!densityFacet.getWorldRegion().contains(pos) || densityFacet.getWorld(pos) <= 0) {
                    // The lake breaches the surface. Abort.
                    return;
                }
                content.add(pos);
                frontier.add(Direction.FORWARD.asVector3i().add(pos, new Vector3i()));
                frontier.add(Direction.BACKWARD.asVector3i().add(pos, new Vector3i()));
                frontier.add(Direction.LEFT.asVector3i().add(pos, new Vector3i()));
                frontier.add(Direction.RIGHT.asVector3i().add(pos, new Vector3i()));
                frontier.add(Direction.UP.asVector3i().add(pos, new Vector3i()));
                frontier.add(Direction.DOWN.asVector3i().add(pos, new Vector3i()));
            }
        }
        facet.add(new Lake(origin.y, content, distanceBelowGround > 100 ? lava : water));
    }

    private void generateSurfaceLake(Vector3i origin, LakeFacet facet, SurfacesFacet surfaces, DensityFacet density, int seaLevel) {
        if (origin.y < seaLevel) {
            return;
        }
        float width = Math.abs(noise.noise(origin.x, origin.y, origin.z + 4)) * (Lake.MAX_RADIUS - Lake.MIN_RADIUS) + Lake.MIN_RADIUS;
        float depth = Math.abs(noise.noise(origin.x, origin.y, origin.z + 5)) * (width / 2 - Lake.MIN_DEPTH) + Lake.MIN_DEPTH;

        // Generate the lake's 2D shape
        Set<Vector3i> surface = new HashSet<>();
        Set<Vector3i> shore = new HashSet<>();
        Queue<Vector3i> frontier = new ArrayDeque<>();
        frontier.add(origin);
        while (!frontier.isEmpty()) {
            Vector3i pos = frontier.remove();
            if (surface.contains(pos)) {
                continue;
            }
            if (!surfaces.getWorldRegion().contains(pos)) {
                // Information important to the lake's construction is missing. Abort.
                return;
            }
            if (localDepth(origin, pos, depth, width) <= 0) {
                shore.add(pos);
            } else {
                surface.add(pos);
                frontier.add(new Vector3i(0,0,1).add(pos));
                frontier.add(new Vector3i(0,0,-1).add(pos));
                frontier.add(new Vector3i(1,0,0).add(pos));
                frontier.add(new Vector3i(-1,0,0).add(pos));
            }
        }

        if (surface.isEmpty()) {
            // This lake contains no blocks.
            return;
        }

        // Check that there are surfaces nearby (prevents lakes from overlapping with each other, or with caves).
        for (Vector3i pos : surface) {
            boolean hasSurface = false;
            for (int surfaceHeight : surfaces.getWorldColumn(pos.x, pos.z)) {
                if (surfaceHeight >= origin.y - 2 && surfaceHeight <= origin.y + 4) {
                    hasSurface = true;
                    break;
                }
            }
            if (!hasSurface) {
                // There is no surface within range. Abort.
                return;
            }
        }

        // Calculate the height of the shore. The minimum height is used as the height of the lake surface.
        int minHeight = Integer.MAX_VALUE;
        int maxHeight = Integer.MIN_VALUE;
        for (Vector3i pos : shore) {
            boolean hasSurface = false;
            for (int surfaceHeight : surfaces.getWorldColumn(pos.x, pos.z)) {
                if (surfaceHeight >= origin.y - 2 && surfaceHeight <= origin.y + 4) {
                    hasSurface = true;
                    minHeight = Math.min(minHeight, surfaceHeight);
                    maxHeight = Math.max(maxHeight, surfaceHeight);
                    break;
                }
            }
            if (!hasSurface) {
                // There is no surface within range. Abort.
                return;
            }
        }

        if (maxHeight - minHeight > 2 || minHeight <= seaLevel) {
            // This area is too sloped or already underwater. Abort.
            return;
        }

        // Carve out space for the lake, and calculate the set of all the blocks it contains.
        Set<Vector3i> content = new HashSet<>();
        for (Vector3i pos : surface) {
            pos.y = minHeight - localDepth(origin, pos, depth, width); // The lake floor
            if (!density.getWorldRegion().contains(pos) || !density.getWorldRegion().contains(pos.x, minHeight, pos.z)) {
                return;
            }
            if (density.getWorld(pos) > 0) {
                int surfaceHeight = surfaces.getNextAbove(pos);
                while (pos.y < surfaceHeight) {
                    pos.add(0,1,0);
                    density.setWorld(pos, 0);
                    if (pos.y <= minHeight) {
                        content.add(new Vector3i(pos));
                    }
                }
                surfaces.setWorld(pos, false);
            } else {
                int surfaceHeight = surfaces.getNextBelow(pos);
                pos.set(0,surfaceHeight + 1,0);
                while (pos.y < minHeight) {
                    content.add(new Vector3i(pos));
                    pos.add(0,1,0);
                }
                surfaces.setWorld(pos.x, surfaceHeight, pos.z, false);
            }
        }

        for (Vector3i pos : shore) {
            int surfaceHeight = surfaces.getNextAbove(new org.joml.Vector3i(pos.x, minHeight, pos.z));
            if (surfaceHeight > minHeight) {
                density.setWorld(pos.x, surfaceHeight, pos.z, 0);
                surfaces.setWorld(pos.x, surfaceHeight, pos.z, false);
                surfaces.setWorld(pos.x, surfaceHeight - 1, pos.z, true);
            }
        }

        facet.add(new Lake(minHeight, content, water));
    }

    private float square(float x) {
        return x * x;
    }

    private int localDepth(Vector3i origin, Vector3i pos, float depth, float width) {
        return (int) (depth * (SURFACE_LAKE_IRREGULARITY * depthModifyingNoise.noise(pos.x, pos.y, pos.z) + 1 - square((pos.x - origin.x) / width) - square((pos.z - origin.z) / width)));
    }
}

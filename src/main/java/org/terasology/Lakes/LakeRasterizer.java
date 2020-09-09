// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.Lakes;

import org.terasology.engine.math.ChunkMath;
import org.terasology.engine.registry.CoreRegistry;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.engine.world.chunks.CoreChunk;
import org.terasology.engine.world.generation.Region;
import org.terasology.engine.world.generation.WorldRasterizerPlugin;
import org.terasology.engine.world.generation.facets.SurfaceHeightFacet;
import org.terasology.engine.world.generator.plugin.RegisterPlugin;
import org.terasology.math.geom.Vector3i;

@RegisterPlugin
public class LakeRasterizer implements WorldRasterizerPlugin {

    private Block sand;
    private Block stone;
    private Block water;
    private Block air;
    private Block lava;

    @Override
    public void initialize() {
        water = CoreRegistry.get(BlockManager.class).getBlock("CoreAssets:Water");
        lava = CoreRegistry.get(BlockManager.class).getBlock("CoreAssets:Lava");
        sand = CoreRegistry.get(BlockManager.class).getBlock("CoreAssets:Sand");
        stone = CoreRegistry.get(BlockManager.class).getBlock("CoreAssets:Stone");
        air = CoreRegistry.get(BlockManager.class).getBlock("Engine:Air");
    }

    @Override
    public void generateChunk(CoreChunk chunk, Region chunkRegion) {

        LakeDepthFacet lakeDepthFacet = chunkRegion.getFacet(LakeDepthFacet.class);
        LakeHeightFacet lakeHeightFacet = chunkRegion.getFacet(LakeHeightFacet.class);
        LakeFacet lakeFacet = chunkRegion.getFacet(LakeFacet.class);
        LavaFacet lavaFacet = chunkRegion.getFacet(LavaFacet.class);
        SurfaceHeightFacet surfaceHeightFacet = chunkRegion.getFacet(SurfaceHeightFacet.class);

        for (Vector3i position : chunkRegion.getRegion()) {

            Lake lake = lakeFacet.getNearestLake(position);
            Lake lavaLake = lavaFacet.getNearestLake(position);

            if (lake.isNotNull() && lake.isInRange(position)) {

                float surfaceHeight = surfaceHeightFacet.getWorld(position.x(), position.z());
                float lakeDepth = lakeDepthFacet.getWorld(position.x(), position.z());
                float lakeHeight = lakeHeightFacet.getWorld(position.x(), position.z());

                if (lake.LakeContains(position) && position.y() <= lake.getWaterHeight() && (position.y() >= lake.getWaterHeight() - lakeDepth ||
                        position.y() > surfaceHeight)) {
                    chunk.setBlock(ChunkMath.calcRelativeBlockPos(position), water);
                } else if (lake.OuterContains(position) && position.y() <= lake.getWaterHeight() && position.y() >= surfaceHeight) {
                    chunk.setBlock(ChunkMath.calcRelativeBlockPos(position), sand);
                } else if (lake.LakeContains(position) && position.y() > lake.getWaterHeight() && position.y() <= lake.getWaterHeight() + lakeHeight) {
                    chunk.setBlock(ChunkMath.calcRelativeBlockPos(position), air);
                }
            }

            if (lavaFacet.isEnabled() && lavaLake.isNotNull() && lake.isInRange(position)) {

                float surfaceHeight = surfaceHeightFacet.getWorld(position.x(), position.z());
                float lakeDepth = lakeDepthFacet.getWorld(position.x(), position.z());
                float lakeHeight = lakeHeightFacet.getWorld(position.x(), position.z());

                if (lavaLake.LakeContains(position) && position.y() <= lavaLake.getWaterHeight() && (position.y() >= lavaLake.getWaterHeight() - lakeDepth ||
                        position.y() > surfaceHeight)) {
                    chunk.setBlock(ChunkMath.calcRelativeBlockPos(position), lava);
                } else if (lavaLake.OuterContains(position) && position.y() <= lavaLake.getWaterHeight() && position.y() >= surfaceHeight) {
                    chunk.setBlock(ChunkMath.calcRelativeBlockPos(position), stone);
                } else if (lavaLake.LakeContains(position) && position.y() > lavaLake.getWaterHeight() && position.y() <= lavaLake.getWaterHeight() + lakeHeight) {

                    chunk.setBlock(ChunkMath.calcRelativeBlockPos(position), air);
                }
            }

        }
    }
}

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

import org.terasology.math.ChunkMath;
import org.terasology.math.geom.Vector3i;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.chunks.CoreChunk;
import org.terasology.world.generation.Region;
import org.terasology.world.generation.WorldRasterizerPlugin;
import org.terasology.world.generation.facets.SurfaceHeightFacet;
import org.terasology.world.generator.plugin.RegisterPlugin;

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

            if(lake.isNotNull() && lake.isInRange(position) ) {

                float surfaceHeight = surfaceHeightFacet.getWorld(position.x(), position.z());
                float lakeDepth = lakeDepthFacet.getWorld(position.x(), position.z());
                float lakeHeight = lakeHeightFacet.getWorld(position.x(), position.z());

                if (lake.LakeContains(position) && position.y() <= lake.getWaterHeight() && (position.y() >= lake.getWaterHeight() - lakeDepth ||
                    position.y() > surfaceHeight)) {
                    chunk.setBlock(ChunkMath.calcRelativeBlockPos(position), water);
                }

               else if (lake.OuterContains(position) && position.y() <= lake.getWaterHeight() && position.y() >= surfaceHeight) {
                    chunk.setBlock(ChunkMath.calcRelativeBlockPos(position), sand);
                }

                else if (lake.LakeContains(position) && position.y() > lake.getWaterHeight() && position.y() <= lake.getWaterHeight() + lakeHeight) {
                    chunk.setBlock(ChunkMath.calcRelativeBlockPos(position), air);
                }
            }

            if(lavaFacet.isEnabled() && lavaLake.isNotNull() && lake.isInRange(position)) {

                float surfaceHeight = surfaceHeightFacet.getWorld(position.x(), position.z());
                float lakeDepth = lakeDepthFacet.getWorld(position.x(), position.z());
                float lakeHeight = lakeHeightFacet.getWorld(position.x(), position.z());

                if (lavaLake.LakeContains(position) && position.y() <= lavaLake.getWaterHeight() && (position.y() >= lavaLake.getWaterHeight() - lakeDepth ||
                        position.y() > surfaceHeight)) {
                    chunk.setBlock(ChunkMath.calcRelativeBlockPos(position), lava);
                }

                else if (lavaLake.OuterContains(position) && position.y() <= lavaLake.getWaterHeight() && position.y() >= surfaceHeight) {
                    chunk.setBlock(ChunkMath.calcRelativeBlockPos(position), stone);
                }

                else if (lavaLake.LakeContains(position) && position.y() > lavaLake.getWaterHeight() && position.y() <= lavaLake.getWaterHeight() + lakeHeight) {
                    chunk.setBlock(ChunkMath.calcRelativeBlockPos(position), air);
                }
            }

        }
    }
}

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

import org.joml.Vector3i;
import org.terasology.engine.registry.CoreRegistry;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.engine.world.chunks.Chunk;
import org.terasology.engine.world.chunks.Chunks;
import org.terasology.engine.world.generation.Region;
import org.terasology.engine.world.generation.WorldRasterizerPlugin;
import org.terasology.engine.world.generator.plugin.RegisterPlugin;

@RegisterPlugin
public class LakeRasterizer implements WorldRasterizerPlugin {

    private Block air;

    @Override
    public void initialize() {
        air = CoreRegistry.get(BlockManager.class).getBlock("Engine:Air");
    }

    @Override
    public void generateChunk(Chunk chunk, Region chunkRegion) {
        LakeFacet lakeFacet = chunkRegion.getFacet(LakeFacet.class);
        Vector3i tempPos = new Vector3i();
        for (Lake lake : lakeFacet.getLakes()) {
            for (Vector3i pos : lake) {
                if (chunkRegion.getRegion().contains(pos)) {
                    Block block = pos.y > lake.surfaceHeight ? air : lake.liquid;
                    chunk.setBlock(Chunks.toRelative(pos, tempPos), block);
                }
            }
        }
    }
}

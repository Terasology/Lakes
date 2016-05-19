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
package org.terasology.PolyLakes;

import org.terasology.math.geom.BaseVector2i;
import org.terasology.math.Region3i;
import org.terasology.math.geom.BaseVector3i;
import org.terasology.math.geom.Rect2i;
import org.terasology.math.geom.Vector3i;
import org.terasology.utilities.procedural.Noise;
import org.terasology.utilities.procedural.WhiteNoise;
import org.terasology.world.generation.*;
import org.terasology.world.generation.facets.SurfaceHeightFacet;
import org.terasology.world.generation.facets.base.BaseFieldFacet2D;
import org.terasology.world.generator.plugin.RegisterPlugin;

@RegisterPlugin
@Produces(LakeFacet.class)
@Requires(@Facet(value = SurfaceHeightFacet.class, border = @FacetBorder(sides = 28)))
public class LakeProvider implements FacetProviderPlugin {

    private Noise noise;

    @Override
    public void setSeed(long seed) {
        noise = new WhiteNoise(seed);
    }

    @Override
    public void process(GeneratingRegion region) {
        SurfaceHeightFacet surfaceHeightFacet = region.getRegionFacet(SurfaceHeightFacet.class);

        Border3D border = region.getBorderForFacet(LakeFacet.class);
        //Extend border by max radius + max length + max outer length and max lakedepth
        border = border.extendBy(11,12,27);
        LakeFacet lakes = new LakeFacet(region.getRegion(), border);
        Region3i worldRegion = lakes.getWorldRegion();
        for (Vector3i pos : worldRegion) {
            float sHeight = surfaceHeightFacet.getWorld(pos.x(),pos.z());

            if( pos.y()<sHeight-20 && noise.noise(pos.x(), pos.y(), pos.z()) > 0.99995){
                lakes.add(new Lake(pos, 10+20*Math.abs(Math.round(noise.noise(pos.x(),pos.z())))));
            }
            else if (pos.y()==sHeight && noise.noise(pos.x(), pos.y(), pos.z()) > 0.99/* && checkGradient(pos,surfaceHeightFacet)*/) {
                lakes.add(new Lake(pos, 10+20*Math.abs(Math.round(noise.noise(pos.x(),pos.z())))));
            }

        }


        region.setRegionFacet(LakeFacet.class, lakes);
    }

    private boolean checkGradient(Vector3i pos, BaseFieldFacet2D facet){

        Rect2i BB = Rect2i.createFromMinAndMax(pos.x()-1,pos.z()-1,pos.x()+1,pos.z()+1);

        if(facet.getWorldRegion().contains(BB)){
            float xDiff = Math.abs(facet.getWorld(pos.x()+1,pos.z())-facet.getWorld(pos.x()-1,pos.z()));
            float yDiff = Math.abs(facet.getWorld(pos.x(),pos.z()+1)-facet.getWorld(pos.x(),pos.z()-1));
            float xyDiff = Math.abs(facet.getWorld(pos.x()+1,pos.z()+1)-facet.getWorld(pos.x()-1,pos.z()-1));
            if(xDiff > 5 || yDiff > 5 || xyDiff > 5){
                return true;
            }
            else return true;
        }

        return true;
    }
}

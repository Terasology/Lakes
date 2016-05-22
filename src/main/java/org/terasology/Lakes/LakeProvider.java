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

import org.terasology.math.Region3i;
import org.terasology.math.geom.Rect2i;
import org.terasology.math.geom.Vector2i;
import org.terasology.math.geom.Vector3i;
import org.terasology.utilities.procedural.Noise;
import org.terasology.utilities.procedural.WhiteNoise;
import org.terasology.world.generation.*;
import org.terasology.world.generation.facets.SurfaceHeightFacet;
import org.terasology.world.generation.facets.base.BaseFieldFacet2D;
import org.terasology.world.generator.plugin.RegisterPlugin;

@RegisterPlugin
@Produces(LakeFacet.class)
@Requires(@Facet(value = SurfaceHeightFacet.class, border = @FacetBorder(sides = 30)))
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
        border = border.extendBy(11,12,15);
        LakeFacet lakes = new LakeFacet(region.getRegion(), border);
        Region3i worldRegion = lakes.getWorldRegion();
        for (Vector3i pos : worldRegion) {
            float sHeight = surfaceHeightFacet.getWorld(pos.x(),pos.z());
            float noiseValue = noise.noise(pos.x(),pos.y(),pos.z());
            if( pos.y()<sHeight-20 && noiseValue > 0.99998){
                lakes.add(new Lake(pos, 10+20*Math.abs(Math.round(noise.noise(pos.x(),pos.z())))));
            }

            else if (pos.y()==Math.round(sHeight) && noiseValue > 0.9993 && checkGradient(pos,surfaceHeightFacet)) {
                Lake temp = new Lake(pos, 10+20*Math.abs(Math.round(noise.noise(pos.x(),pos.z()))));
                if( checkCorners(temp.getBB(), surfaceHeightFacet)){
                    int minHeight= getMinimumHeight(temp.getBB(), surfaceHeightFacet);
                    if(minHeight < pos.y()){
                        temp.setWaterHeight(minHeight);
                    }
                    lakes.add(temp);
                }
            }


        }


        region.setRegionFacet(LakeFacet.class, lakes);
    }

    protected boolean checkGradient(Vector3i pos, BaseFieldFacet2D facet){

        Rect2i BB = Rect2i.createFromMinAndMax(pos.x()-3,pos.z()-3,pos.x()+3,pos.z()+3);

        if(facet.getWorldRegion().contains(BB)){
            float xDiff = Math.abs(facet.getWorld(pos.x()+3,pos.z())-facet.getWorld(pos.x()-3,pos.z()));
            float yDiff = Math.abs(facet.getWorld(pos.x(),pos.z()+3)-facet.getWorld(pos.x(),pos.z()-3));
            float xyDiff = Math.abs(facet.getWorld(pos.x()+3,pos.z()+3)-facet.getWorld(pos.x()-3,pos.z()-3));
            if(xDiff > 2 || yDiff > 2 || xyDiff > 2){
                return false;
            }
            else return true;
        }

        return false;
    }

    protected boolean checkCorners(Rect2i BB, BaseFieldFacet2D facet){
        Vector2i max = BB.max();
        Vector2i min = BB.min();
        if(facet.getWorldRegion().contains(BB)){
            float[] corners = new float[4];
            corners[0] = facet.getWorld(max);
            corners[1] = facet.getWorld(min);
            corners[2] = facet.getWorld(min.x()+BB.sizeX(),min.y());
            corners[3] = facet.getWorld(min.x(),min.y()+BB.sizeY());
            for(int i = 0 ;i < corners.length;i++){
                for(int j = 0; j < corners.length; j++){
                    if(Math.abs(corners[i]-corners[j])>4){
                        return false;
                    }
                }
            }

        }
        return true;
    }

    protected int getMinimumHeight(Rect2i BB, BaseFieldFacet2D facet){

        Vector2i max = BB.max();
        Vector2i min = BB.min();
        int minHeight = 9999;
        if(facet.getWorldRegion().contains(BB)){
            float[] corners = new float[4];
            corners[0] = facet.getWorld(max);
            corners[1] = facet.getWorld(min);
            corners[2] = facet.getWorld(min.x()+BB.sizeX(),min.y());
            corners[3] = facet.getWorld(min.x(),min.y()+BB.sizeY());
            for(int i = 0 ;i< corners.length;i++){
                if(corners[i]<minHeight){
                    minHeight = Math.round(corners[i]);
                }
            }
        }
        return minHeight;
    }
}

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
import org.terasology.math.geom.BaseVector3i;
import org.terasology.math.geom.Vector3i;
import org.terasology.world.generation.Border3D;
import org.terasology.world.generation.facets.base.BaseFacet3D;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


public class LakeFacet extends BaseFacet3D {

    protected Set<Lake> lakes = new LinkedHashSet<>();
    protected boolean enabled = true;
    protected Lake NullLake = new Lake();

    public LakeFacet(Region3i targetRegion, Border3D border) {
        super(targetRegion, border);
    }

    /**
     * @param lake
     */
    public void add(Lake lake) {
        lakes.add(lake);
    }

    /**
     * @return the lakes
     */
    public Set<Lake> getLakes() {
        return Collections.unmodifiableSet(lakes);
    }

    public Lake getNearestLake(Vector3i pos){

        if(lakes.isEmpty()){
            return NullLake;
        }

        Lake nearest = (Lake) lakes.toArray()[0];

        double dis=9999;

        for(Lake lake : lakes){
            double newDis =  pos.distance(lake.getOrigin());
            if(dis > newDis){
                nearest = lake;
                dis=newDis;
            }
        }

        return nearest;
    }

    public int getWorldIndex(BaseVector3i pos) {
        return getWorldIndex(pos.x(), pos.y(),pos.z());
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled( boolean enabled ){ this.enabled = enabled; }
}
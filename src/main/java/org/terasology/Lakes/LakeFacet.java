// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.Lakes;

import org.terasology.engine.math.Region3i;
import org.terasology.engine.world.generation.Border3D;
import org.terasology.engine.world.generation.facets.base.BaseFacet3D;
import org.terasology.math.geom.Vector3i;

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

    public Lake getNearestLake(Vector3i pos) {

        if (lakes.isEmpty()) {
            return NullLake;
        }

        Lake nearest = (Lake) lakes.toArray()[0];

        double dis = Double.MAX_VALUE;

        for (Lake lake : lakes) {
            double newDis = pos.distanceSquared(lake.getOrigin());
            if (dis > newDis) {
                nearest = lake;
                dis = newDis;
            }
        }

        return nearest;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

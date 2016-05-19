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

import java.lang.Math;
import org.terasology.math.geom.Vector2f;
import org.terasology.utilities.procedural.WhiteNoise;

public class Polygon {

    private Vector2f[] points;
    private Vector2f origin;
    private float maxLength = 10;
    private float maxRadius = 40;

    public Polygon(int pnum, Vector2f origin){

        points = createEllipticPolygon(pnum);
        this.origin=origin;

    }

    private Vector2f[] createRandPolygon(int pnum){
        Vector2f[] poly = new Vector2f[pnum];
        poly[0]=origin;
        WhiteNoise rand = new WhiteNoise((long) origin.length());

        float alpha, length;

        for(int i=1; i<pnum; i++){
            alpha = rand.intNoise(i)*360;
            length = rand.intNoise(i+pnum)*maxLength;
            float newX=poly[i-1].x+length*(float)Math.cos((double) alpha);
            float newY=poly[i-1].y+length*(float)Math.sin((double) alpha);

            poly[i] = new Vector2f(newX,newY);
        }

        return poly;

    }

    private Vector2f[] createEllipticPolygon(int pnum){
        Vector2f[] poly = new Vector2f[pnum];
        WhiteNoise rand = new WhiteNoise((long) origin.length());

        float alpha, yRadius, xRadius, length;

        for(int i=0; i<pnum; i++){
            alpha = 360/(pnum*(i+1));
            xRadius = rand.intNoise(i+pnum)*maxRadius;
            yRadius = rand.intNoise(i+2*pnum)*maxRadius;
            length = rand.intNoise(i)*maxLength;
            float newX=poly[i-1].x+xRadius*(float)Math.cos((double) alpha);
            float newY=poly[i-1].y+yRadius*(float)Math.sin((double) alpha);

            newX=newX+(newX-origin.x)*length;
            newY=newY+(newY-origin.x)*length;

            poly[i] = new Vector2f(newX,newY);
        }

        return poly;

    }
}

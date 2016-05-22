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

import org.terasology.math.geom.Rect2i;
import org.terasology.math.geom.Vector3i;
import org.terasology.utilities.procedural.Noise;
import org.terasology.utilities.procedural.WhiteNoise;

import java.awt.*;

public class Lake {

    private Vector3i origin;
    private float maxLength = 2;
    private float maxLengthOuter = 2;
    private float maxRadius = 11;
    private float maxDepth = 12;
    private int WaterHeight;
    private Polygon LakePoly;
    private Polygon OuterPoly;
    private Noise noise;
    private int points;

    public Lake(Vector3i origin, int pnum){
        long seed = Math.round(origin.length()*217645199);
        //noise = new WhiteNoise(Math.round(origin.length()*217645199));

        noise = new WhiteNoise(seed);
        this.origin=origin;
        WaterHeight = origin.y();
        createEllipticPolygon(pnum);
        points = pnum;
    }

    //Null Lake
    public Lake (){
        points = 0;
    }

    private void createEllipticPolygon(int pnum){


        int[] x = new int[pnum];
        int[] y = new int[pnum];
        int[] xOuter = new int[pnum];
        int[] yOuter = new int[pnum];

        double alpha;
        float yRadius, xRadius, length, outerlength;

        xRadius =  Math.abs(noise.noise(origin.x(),origin.y())*maxRadius);
        yRadius = Math.abs(noise.noise(origin.z(),origin.y())*maxRadius);

        for(int i=0; i<pnum; i++){
            alpha = i * 2 * Math.PI / pnum;

            length = Math.abs(noise.noise(origin.y()*i,origin.x()*i)*maxLength);
            outerlength = Math.abs(noise.noise(origin.y()*i*2,origin.x()*i*2)*maxLengthOuter);

            //Lake Polygon points:
            x[i] = origin.x()+Math.round(xRadius*(float)Math.cos((double) alpha));
            y[i] = origin.z()+Math.round(yRadius*(float)Math.sin((double) alpha));

            x[i]=Math.round(x[i]+Math.signum((x[i]-origin.x()))*length);
            y[i]=Math.round(y[i]+Math.signum((y[i]-origin.z()))*length);


            //Outer Polygon points:
            xOuter[i]=Math.round(x[i]+Math.signum((x[i]-origin.x()))*(2+Math.abs(outerlength)));
            yOuter[i]=Math.round(y[i]+Math.signum((y[i]-origin.z()))*(2+Math.abs(outerlength)));

        }


        LakePoly = new Polygon(x,y,pnum);
        OuterPoly = new Polygon(xOuter, yOuter, pnum);

    }

    public boolean LakeContains(Vector3i pos){
        return LakePoly.contains(pos.getX(),pos.getZ());
    }

    public boolean OuterContains(Vector3i pos){
        return !LakePoly.contains(pos.getX(),pos.getZ()) && OuterPoly.contains(pos.getX(),pos.getZ());
    }

    public boolean BBContains(Vector3i pos){
        return OuterPoly.getBounds().contains(pos.getX(),pos.getZ());
    }

    public Rect2i getBB() {
        Rectangle AwtRect = OuterPoly.getBounds();
        Rect2i TeraRect = Rect2i.createFromMinAndMax(Math.round((float) AwtRect.getMinX()),
                Math.round((float) AwtRect.getMinY()), Math.round((float) AwtRect.getMaxX()),
                Math.round((float) AwtRect.getMaxY()));
        return TeraRect;
    }

    public Vector3i getOrigin(){
        return origin;
    }

    public int getWaterHeight() { return WaterHeight; }

    public void setWaterHeight( int WaterHeight ) { this.WaterHeight = WaterHeight; }

    public boolean isNull() {

        if (points == 0) {
            return true;
        }
        else return false;
    }

    public boolean isInRange(Vector3i pos ) {
        if(origin == null){ return false; }
        return Math.abs(pos.x()-origin.x())<=maxRadius && Math.abs(pos.z()-origin.z())<=maxRadius
                && Math.abs(pos.x()-origin.x())<=maxDepth;
    }

}


/* This methods creates a completely random Polygon, however intersection is possible!
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
*/
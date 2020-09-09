// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.Lakes;

import org.terasology.engine.utilities.procedural.Noise;
import org.terasology.engine.utilities.procedural.WhiteNoise;
import org.terasology.math.geom.Rect2i;
import org.terasology.math.geom.Vector3i;

import java.awt.Polygon;
import java.awt.Rectangle;

public class Lake {
    public static final int MAX_LENGTH = 10;
    public static final int MAX_LENGTH_OUTER = MAX_LENGTH + 8;
    public static final int MAX_RADIUS = 20;
    public static final int MAX_SIZE = MAX_RADIUS + MAX_LENGTH + MAX_LENGTH_OUTER;
    public static final int MAX_DEPTH = 12;
    public static final int MIN_VERTICES = 10;
    public static final int MAX_VERTICES = 30;
    private final int points;
    private Vector3i origin;
    private int waterHeight;
    private Polygon lakePoly;
    private Polygon outerPoly;
    private Noise noise;

    public Lake(Vector3i origin, int pnum) {
        long seed = Math.round(origin.length() * 217645199);
        //noise = new WhiteNoise(Math.round(origin.length()*217645199));

        noise = new WhiteNoise(seed);
        this.origin = origin;
        waterHeight = origin.y();
        createEllipticPolygon(pnum);
        points = pnum;
    }

    //Null Lake
    public Lake() {
        points = 0;
    }

    private void createEllipticPolygon(int pnum) {


        int[] x = new int[pnum];
        int[] y = new int[pnum];
        int[] xOuter = new int[pnum];
        int[] yOuter = new int[pnum];

        double alpha;
        float yRadius, xRadius, length, outerlength;

        xRadius = Math.abs(noise.noise(origin.x(), origin.y()) * MAX_RADIUS);
        yRadius = Math.abs(noise.noise(origin.z(), origin.y()) * MAX_RADIUS);

        for (int i = 0; i < pnum; i++) {
            alpha = i * 2 * Math.PI / pnum;

            length = Math.abs(noise.noise(origin.y() * i, origin.x() * i) * MAX_LENGTH);
            outerlength = Math.abs(noise.noise(origin.y() * i * 2, origin.x() * i * 2) * MAX_LENGTH_OUTER);

            //Lake Polygon points:
            x[i] = origin.x() + Math.round(xRadius * (float) Math.cos(alpha));
            y[i] = origin.z() + Math.round(yRadius * (float) Math.sin(alpha));

            x[i] = Math.round(x[i] + Math.signum((x[i] - origin.x())) * length);
            y[i] = Math.round(y[i] + Math.signum((y[i] - origin.z())) * length);


            //Outer Polygon points:
            xOuter[i] = Math.round(x[i] + Math.signum((x[i] - origin.x())) * (Math.abs(outerlength)));
            yOuter[i] = Math.round(y[i] + Math.signum((y[i] - origin.z())) * (Math.abs(outerlength)));

        }


        lakePoly = new Polygon(x, y, pnum);
        outerPoly = new Polygon(xOuter, yOuter, pnum);

    }

    public boolean LakeContains(Vector3i pos) {
        return lakePoly.contains(pos.getX(), pos.getZ());
    }

    public boolean OuterContains(Vector3i pos) {
        return !lakePoly.contains(pos.getX(), pos.getZ()) && outerPoly.contains(pos.getX(), pos.getZ());
    }

    public boolean boundingBoxContains(Vector3i pos) {
        return outerPoly.getBounds().contains(pos.getX(), pos.getZ());
    }

    public Rect2i getBoundingBox() {
        Rectangle awtRect = outerPoly.getBounds();
        Rect2i teraRect = Rect2i.createFromMinAndMax(Math.round((float) awtRect.getMinX()),
                Math.round((float) awtRect.getMinY()), Math.round((float) awtRect.getMaxX()),
                Math.round((float) awtRect.getMaxY()));
        return teraRect;
    }

    public Vector3i getOrigin() {
        return origin;
    }

    /**
     * Returns the y position of the lake surface.
     *
     * @return Y of lake surface in world coordinates
     */
    public int getWaterHeight() {
        return waterHeight;
    }

    public void setWaterHeight(int waterHeight) {
        this.waterHeight = waterHeight;
    }

    public boolean isNotNull() {
        return points != 0;
    }

    public boolean isInRange(Vector3i pos) {
        if (origin == null) {
            return false;
        }
        return Math.abs(pos.x() - origin.x()) <= MAX_SIZE && Math.abs(pos.z() - origin.z()) <= MAX_SIZE
                && Math.abs(pos.y() - origin.y()) <= MAX_DEPTH;
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

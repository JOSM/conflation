package com.vividsolutions.jcs.conflate.polygonmatch;

import com.vividsolutions.jcs.algorithm.VertexHausdorffDistance;
import org.locationtech.jts.geom.Geometry;

/**
 * Uses an approximation of the Hausdorff distance.
 * @see VertexHausdorffDistance
 */
public class HausdorffDistanceMatcher extends AbstractDistanceMatcher {

    public HausdorffDistanceMatcher() {}

    /**
     * Constructor not part of the original JCS code.
     * @param maxDistance the maximum distance, which will give a score of 0.
     */
    public HausdorffDistanceMatcher(double maxDistance) {
        super();
        setMaxDistance(maxDistance);
    }

    @Override
    protected double distance(Geometry target, Geometry candidate) {
        return new VertexHausdorffDistance(target, candidate).distance();
    }

}

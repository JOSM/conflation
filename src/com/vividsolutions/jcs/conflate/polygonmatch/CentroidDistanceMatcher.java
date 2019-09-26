package com.vividsolutions.jcs.conflate.polygonmatch;

import org.locationtech.jts.geom.Geometry;

public class CentroidDistanceMatcher extends AbstractDistanceMatcher {

    public CentroidDistanceMatcher() {}

    /**
     * Constructor not part of the original JCS code.
     * @param maxDistance the maximum distance, which will give a score of 0.
     */
    public CentroidDistanceMatcher(double maxDistance) {
        super();
        setMaxDistance(maxDistance);
    }

    @Override
    protected double distance(Geometry target, Geometry candidate) {
        return target.getCentroid().distance(
            candidate.getCentroid());
    }
}

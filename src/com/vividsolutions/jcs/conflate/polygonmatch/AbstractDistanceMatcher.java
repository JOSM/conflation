package com.vividsolutions.jcs.conflate.polygonmatch;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import java.awt.geom.Point2D;

public abstract class AbstractDistanceMatcher extends IndependentCandidateMatcher {

    /**
     * The maximum distance, which will give a score of 0.
     * Field not part of the original JCS code.
     */
    protected double maxDistance = 0;
    
    @Override
    public double match(Geometry target, Geometry candidate) {
        double distance = distance(target, candidate);
        if (maxDistance > 0) {
            return Math.max(0, 1 - (distance / maxDistance));
        } else {
            return 1
                    - (distance
                    / combinedEnvelopeDiagonalDistance(target, candidate));
        }
    }

    protected abstract double distance(Geometry target, Geometry candidate);

    private double combinedEnvelopeDiagonalDistance(
        Geometry target,
        Geometry candidate) {
        Envelope envelope = new Envelope(target.getEnvelopeInternal());
        envelope.expandToInclude(candidate.getEnvelopeInternal());
        return Point2D.distance(
            envelope.getMinX(),
            envelope.getMinY(),
            envelope.getMaxX(),
            envelope.getMaxY());
    }

    /**
     * Set he maximum distance, which will give a score of 0.
     * Method not part of the original JCS code.
     * @param maxDistance the maximum distance.
     */
    public void setMaxDistance(double maxDistance) {
        if (maxDistance < 0)
            this.maxDistance = 0;
        else
            this.maxDistance = maxDistance;
    }
}

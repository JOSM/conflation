package com.vividsolutions.jcs.conflate.polygonmatch;

import org.locationtech.jts.geom.Geometry;

public class CentroidAligner extends IndependentCandidateMatcher {

    private IndependentCandidateMatcher matcher;

    public CentroidAligner(IndependentCandidateMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public double match(Geometry target, Geometry candidate) {
        return matcher.match(align(target), align(candidate));
    }

    private Geometry align(Geometry original) {
        Geometry aligned = original.copy();
        MatcherUtil.align(aligned, aligned.getCentroid().getCoordinate());
        return aligned;
    }
}

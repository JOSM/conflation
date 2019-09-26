package com.vividsolutions.jcs.conflate.polygonmatch;

import org.locationtech.jts.geom.Geometry;

public class OverlapMatcher extends IndependentCandidateMatcher {

    @Override
    public double match(Geometry target, Geometry candidate) {
        //Impose the min to curb roundoff error in exact matches (a situation which
        //arose during testing (identical datasets)) [Jon Aquino]
        return Math.min(1, (2 * target.intersection(candidate).getArea())
            / (target.getArea() + candidate.getArea()));
    }
}

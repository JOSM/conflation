// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.matcher;

import com.vividsolutions.jcs.conflate.polygonmatch.AbstractDistanceMatcher;
import org.locationtech.jts.geom.Geometry;

/**
 * Compute the standard distance (i.e. the minimum) between two geometries.
 */
public class StandardDistanceMatcher extends AbstractDistanceMatcher {
    
        public StandardDistanceMatcher() {}

        public StandardDistanceMatcher(double maxDistance) {
            super();
            setMaxDistance(maxDistance);
        }

        @Override
        protected double distance(Geometry target, Geometry candidate) {
            return target.distance(candidate);
        }

}

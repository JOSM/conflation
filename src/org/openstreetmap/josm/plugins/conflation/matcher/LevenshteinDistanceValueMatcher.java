// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.matcher;

import org.openstreetmap.josm.tools.Utils;

public class LevenshteinDistanceValueMatcher implements ValueMatcher {

    public static final LevenshteinDistanceValueMatcher INSTANCE = new LevenshteinDistanceValueMatcher();

    public final int distanceThreshold;

    public LevenshteinDistanceValueMatcher() {
        this(0);
    }

    public LevenshteinDistanceValueMatcher(int distanceThreshold) {
        this.distanceThreshold = distanceThreshold;
    }

    @Override
    public double match(String target, String candidate) {
        int maxLength = Integer.max(target.length(), candidate.length());
        if (maxLength == 0)
            return 1.0;
        double divider = (distanceThreshold > 0) ? distanceThreshold : maxLength;
        return Math.max(0.0, 1.0 - ((double) Utils.getLevenshteinDistance(target, candidate) / divider));
    }
}

// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.matcher;

@FunctionalInterface
public interface ValueMatcher {

    /**
     * Match two string values and return the score.
     * @return matching score (between 0.0 and 1.0)
     */
    double match(String target, String candidate);
}

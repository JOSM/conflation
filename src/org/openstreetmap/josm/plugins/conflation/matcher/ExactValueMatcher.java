// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.matcher;

public class ExactValueMatcher implements ValueMatcher {

    public static final ExactValueMatcher INSTANCE = new ExactValueMatcher();

    @Override
    public double match(String target, String candidate) {
        return target.equals(candidate) ? 1.0 : 0.0;
    }
}

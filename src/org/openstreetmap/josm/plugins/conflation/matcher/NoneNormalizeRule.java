// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.matcher;

import org.openstreetmap.josm.data.validation.tests.SimilarNamedWays.NormalizeRule;

public class NoneNormalizeRule implements NormalizeRule {

    @Override
    public String normalize(String value) {
        return value;
    }
}

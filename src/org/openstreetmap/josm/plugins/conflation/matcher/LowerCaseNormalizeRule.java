// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.matcher;

import java.util.Locale;

import org.openstreetmap.josm.data.validation.tests.SimilarNamedWays.NormalizeRule;

public class LowerCaseNormalizeRule implements NormalizeRule {

    public static final LowerCaseNormalizeRule INSTANCE = new LowerCaseNormalizeRule();

    @Override
    public String normalize(String t) {
        return t.toLowerCase(Locale.ENGLISH);
    }
}

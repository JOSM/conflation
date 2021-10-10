// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.matcher;

import java.text.Normalizer;

import org.openstreetmap.josm.data.validation.tests.SimilarNamedWays.NormalizeRule;

public class AccentlessNormalizeRule implements NormalizeRule {

    public static final AccentlessNormalizeRule INSTANCE = new AccentlessNormalizeRule();

    @Override
    public String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}

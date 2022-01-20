package com.vividsolutions.jcs.conflate.polygonmatch;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.plugins.conflation.OsmFeature;
import org.openstreetmap.josm.plugins.jts.JTSConverter;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.feature.IndexedFeatureCollection;

/**
 * Test class for {@link WeightedMatcher}
 * @author Taylor Smock
 */
@BasicPreferences
class WeightedMatcherTest {
    @RegisterExtension
    JOSMTestRules josmTestRules = new JOSMTestRules().projection();
    /**
     * Non-regression test for JOSM #21788
     * This occurred when two {@link org.openstreetmap.josm.plugins.conflation.OsmFeature} objects had {@link Comparable}
     * equality.
     */
    @Test
    void testNonRegression21788() {
        final Node target = new Node();
        target.setCoor(LatLon.ZERO);
        final Node node1 = new Node(1, 1);
        node1.setCoor(LatLon.ZERO);
        final Node node2 = new Node(2, 1);
        node2.setCoor(LatLon.ZERO);
        final JTSConverter converter = new JTSConverter(true);
        final WeightedMatcher weightedMatcher = new WeightedMatcher(1, new UnityMatcher());
        assertDoesNotThrow(() -> weightedMatcher.match(new OsmFeature(target, converter),
                new FeatureDataset(Arrays.asList(new OsmFeature(node1, converter), new OsmFeature(node2, converter)), new FeatureSchema())));
    }

    /**
     * This matcher always gives a score of 1. This is to make it easier for getting out of bounds.
     */
    private static final class UnityMatcher implements FeatureMatcher {
        @Override
        public Matches match(Feature target, FeatureCollection candidates) {
            final Matches matches = new Matches(target.getSchema());
            candidates.forEach(feature -> matches.add(feature, 1));
            return matches;
        }
    }
}

// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Unit tests of {@link SimpleMatch}
 */
public class SimpleMatchTest {

    @Rule
    public JOSMTestRules rules = new JOSMTestRules().preferences();

    @Test
    public void testGetMergingTagCollectionOverwrite() {
        SimpleMatchSettings settings = new SimpleMatchSettings();
        settings.isReplacingGeometry = true;
        settings.mergeTags = new SimpleMatchSettings.All<>();
        settings.overwriteTags = Arrays.asList("addr:housenumber");
        OsmPrimitive n1 = new Node();
        OsmPrimitive n2 = new Node();
        SimpleMatch match = new SimpleMatch(n1, n2, 0.5, 10.0);
        n1.put("addr:housenumber", "1");
        n2.put("addr:housenumber", "2");
        n1.put("addr:street", "Street One");
        n2.put("addr:street", "Street One Two");
        TagCollection tagCollection = match.getMergingTagCollection(settings);
        assertTrue(tagCollection.getNumTagsFor("addr:street") == 2);
        assertTrue(tagCollection.getNumTagsFor("addr:housenumber") == 1);
    }
}

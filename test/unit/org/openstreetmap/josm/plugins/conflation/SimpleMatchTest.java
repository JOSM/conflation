// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link SimpleMatch}
 */
@BasicPreferences
class SimpleMatchTest {
    @Test
    void testGetMergingTagCollectionOverwrite() {
        SimpleMatchSettings settings = new SimpleMatchSettings();
        settings.isReplacingGeometry = true;
        settings.mergeTags = new SimpleMatchSettings.All<>();
        settings.overwriteTags = Collections.singletonList("addr:housenumber");
        OsmPrimitive n1 = new Node();
        OsmPrimitive n2 = new Node();
        SimpleMatch match = new SimpleMatch(n1, n2, 0.5, 10.0);
        n1.put("addr:housenumber", "1");
        n2.put("addr:housenumber", "2");
        n1.put("addr:street", "Street One");
        n2.put("addr:street", "Street One Two");
        TagCollection tagCollection = match.getMergingTagCollection(settings);
        assertEquals(2, tagCollection.getNumTagsFor("addr:street"));
        assertEquals(1, tagCollection.getNumTagsFor("addr:housenumber"));
    }
}

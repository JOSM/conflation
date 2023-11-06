package org.openstreetmap.josm.plugins.conflation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.plugins.jts.JTSConverter;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Projection;

import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.FeatureSchema;
import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Test class for {@link OsmFeature}
 * @author Taylor Smock
 */
@BasicPreferences
@Projection
class OsmFeatureTest {
    /**
     * This checks that two osm features (points) do not match if they have the same geometry but different ids.
     * This is a partial non-regression test for JOSM #21788. The root of the issue is that TreeMap uses the Comparable
     * interface in put operations, and the {@link Comparable} interface specifies that {@code 0} implies equality.
     */
    @Test
    void testCompareTo() {
        final JTSConverter converter = new JTSConverter(true);

        final Node node1 = new Node(1, 1);
        final Node node2 = new Node(2, 1);
        // This is deliberate -- we want to make certain that different equal objects work (i.e., no one accidentally
        // uses == ).
        final Node node3 = new Node(1, 1);
        Arrays.asList(node1, node2, node3).forEach(node -> node.setCoor(LatLon.ZERO));

        final OsmFeature osmFeature1 = new OsmFeature(node1, converter);
        final OsmFeature osmFeature2 = new OsmFeature(node2, converter);
        final OsmFeature osmFeature3 = new OsmFeature(node3, converter);

        assertEquals(osmFeature1, osmFeature3, "The two nodes are equal");
        assertNotEquals(osmFeature1, osmFeature2, "The two nodes are not equal");
        assertEquals(0, osmFeature1.compareTo(osmFeature3));
        assertEquals(0, osmFeature3.compareTo(osmFeature1));
        assertNotEquals(0, osmFeature1.compareTo(osmFeature2));
        assertNotEquals(0, osmFeature2.compareTo(osmFeature3));
    }

    @Test
    void testEqualsContract() {
        final Node redNode = new Node(1, 1);
        final Node blueNode = new Node(2, 2);
        redNode.setCoor(LatLon.NORTH_POLE);
        blueNode.setCoor(LatLon.SOUTH_POLE);

        final FeatureSchema redSchema = new FeatureSchema();
        final FeatureSchema blueSchema = new FeatureSchema();
        redSchema.addAttribute("red", AttributeType.STRING);
        blueSchema.addAttribute("blue", AttributeType.STRING);
        EqualsVerifier.forClass(OsmFeature.class)
                .usingGetClass()
                .withNonnullFields("primitive")
                .withIgnoredFields("attributes" /* mutable */,
                        "schema" /* mutable */,
                        "id" /* not used in class */)
                .withPrefabValues(OsmPrimitive.class, redNode, blueNode)
                .withPrefabValues(FeatureSchema.class, redSchema, blueSchema)
                .verify();
    }
}

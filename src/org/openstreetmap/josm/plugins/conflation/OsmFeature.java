// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation;

import java.util.Map;
import java.util.Objects;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.plugins.jts.JTSConverter;

import com.vividsolutions.jump.feature.AbstractBasicFeature;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureSchema;

public class OsmFeature extends AbstractBasicFeature {
    private Object[] attributes;
    private final OsmPrimitive primitive;

    /**
     * Create a copy of the OSM geometry
     * TODO: update from underlying primitive
     */
    public OsmFeature(OsmPrimitive prim, JTSConverter jtsConverter) {
        super(new FeatureSchema());
        this.primitive = Objects.requireNonNull(prim);
        Map<String, String> keys = prim.getKeys();
        this.attributes = new Object[keys.size() + 1];
        this.getSchema().addAttribute("__GEOMETRY__", AttributeType.GEOMETRY);
        keys.forEach((key, value) -> {
            this.getSchema().addAttribute(key, AttributeType.STRING);
            setAttribute(key, value);
        });
        final JTSConverter converter;
        if (jtsConverter != null)
            converter = jtsConverter;
        else
            converter = new JTSConverter(true);
        setGeometry(converter.convert(prim));
    }

    @Override
    public void setAttributes(Object[] attributes) {
        this.attributes = attributes;
    }

    @Override
    public void setAttribute(int attributeIndex, Object newAttribute) {
        attributes[attributeIndex] = newAttribute;
    }

    @Override
    public Object getAttribute(int i) {
        return attributes[i];
    }

    @Override
    public Object[] getAttributes() {
        return attributes;
    }
    
    public OsmPrimitive getPrimitive() {
        return primitive;
    }
    
    @Override
    public int getID() {
        // FIXME: should work most of the time, GeoAPI more robust, need to
        // consider the dataset (e.g. two non-uploaded layers can have different
        // objects with the same id
        return (int) primitive.getUniqueId();
    }

    @Override
    public int compareTo(Feature abstractBasicFeature) {
        // Rather unfortunately, we cannot implement the interface with OsmFeature
        // So we are going to special case osm features, and compare ids.
        // The super.compareTo only looks at geometry. If the geometry is the same, then it returns 0.
        final int superCompare = super.compareTo(abstractBasicFeature);
        if (superCompare == 0 && abstractBasicFeature instanceof OsmFeature) {
            final OsmFeature other = (OsmFeature) abstractBasicFeature;
            return this.primitive.compareTo(other.primitive);
        }
        return superCompare;
    }

    @Override
    public boolean equals(Object other) {
        return other != null && this.getClass().equals(other.getClass())
                && Objects.equals(((OsmFeature) other).primitive, this.primitive);
    }

    @Override
    public int hashCode() {
        // No superclasses implement hashCode
        return this.primitive.hashCode();
    }
}

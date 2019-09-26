/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */
package com.vividsolutions.jump.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.strtree.STRtree;

/**
 *  An IndexedFeatureCollection creates a new collection which is backed by a
 *  FeatureCollection, but which is indexed for query purposes.
 */
public class IndexedFeatureCollection extends FeatureCollectionWrapper {
    private SpatialIndex spatialIndex;

    public IndexedFeatureCollection(FeatureCollection fc) {
        //Based on tests on Victoria ICI data, 10 is an optimum node-capacity for
        //fast queries. [Jon Aquino]
        this(fc, new STRtree(10));
    }

    public IndexedFeatureCollection(FeatureCollection fc,
        SpatialIndex spatialIndex) {
        super(fc);
        this.spatialIndex = spatialIndex;
        createIndex();
    }

    @Override
	public void add(Feature feature) {
        throw new UnsupportedOperationException("Index cannot be modified");
    }

    @Override
	public void remove(Feature feature) {
        throw new UnsupportedOperationException("Index cannot be modified");
    }

    @Override
	public List<Feature> query(Envelope env) {
        // index query returns list of *potential* overlaps (e.g. it is a primary filter)
        @SuppressWarnings("unchecked")
		List<Feature> candidate = spatialIndex.query(env);

        // filter out only Features where envelope actually intersects
        List<Feature> result = new ArrayList<>();

        for (Feature f : candidate) {
            if (env.intersects(f.getGeometry().getEnvelopeInternal())) {
                result.add(f);
            }
        }

        return result;
    }

    private void createIndex() {
        for (Feature f : this) {
            spatialIndex.insert(f.getGeometry().getEnvelopeInternal(), f);
        }
    }

    @Override
	public void addAll(Collection<? extends Feature> features) {
        throw new UnsupportedOperationException("Index cannot be modified");
    }

    @Override
	public Collection<Feature> remove(Envelope env) {
        throw new UnsupportedOperationException("Index cannot be modified");
    }

    @Override
	public void removeAll(Collection<Feature> features) {
        throw new UnsupportedOperationException("Index cannot be modified");
    }
}

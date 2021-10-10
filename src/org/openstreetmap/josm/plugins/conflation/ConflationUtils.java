// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.visitor.MergeSourceBuildingVisitor;
import org.openstreetmap.josm.gui.MainApplication;

/**
 * Utilities class.
 */
public final class ConflationUtils {

    private ConflationUtils() {}

    public static EastNorth getCenter(OsmPrimitive prim) {
        LatLon center = prim.getBBox().getTopLeft().getCenter(prim.getBBox().getBottomRight());
        return MainApplication.getMap().mapView.getProjection().latlon2eastNorth(center);
    }

    public static List<PrimitiveData> copyObjects(DataSet sourceDataSet, OsmPrimitive primitive) {
        return copyObjects(sourceDataSet, Collections.singleton(primitive));
    }

    public static List<PrimitiveData> copyObjects(DataSet sourceDataSet, Collection<OsmPrimitive> primitives) {

        Collection<OsmPrimitive> origSelection = sourceDataSet.getSelected();
        sourceDataSet.setSelected(primitives);
        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(sourceDataSet);

        DataSet newDataSet = builder.build();
        //restore selection
        sourceDataSet.setSelected(origSelection);

        return newDataSet.allPrimitives().stream()
                .map(p -> p.save())
                .collect(Collectors.toList());
    }
}

// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.plugins.jts.JTSConverter;
import org.openstreetmap.josm.tools.Logging;

import com.vividsolutions.jcs.conflate.polygonmatch.FCMatchFinder;
import com.vividsolutions.jcs.conflate.polygonmatch.Matches;
import com.vividsolutions.jump.feature.AttributeType;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.feature.IndexedFeatureCollection;
import com.vividsolutions.jump.task.TaskMonitor;

public final class MatchesComputation {

    private MatchesComputation() {}

    /**
     * Generates a list of matches from the given user settings.
     * @param settings the setting to use: list of objects to match, the match finder to use...
     * @param monitor progress monitor for report
     * @return the list of match found
     */
    public static Collection<SimpleMatch> generateMatches(SimpleMatchSettings settings, ProgressMonitor monitor) {
        monitor.subTask("Generating matches");

        // create Features and collections from primitive selections
        HashSet<OsmPrimitive> allPrimitives = new HashSet<>();
        HashSet<OsmPrimitive> refPrimitives = new HashSet<>(settings.referenceSelection);
        HashSet<OsmPrimitive> subPrimitives = new HashSet<>(settings.subjectSelection);
        allPrimitives.addAll(refPrimitives);
        allPrimitives.addAll(subPrimitives);

        AbstractMap.SimpleEntry<FeatureCollection,HashMap<OsmPrimitive, Throwable>>
            allFeaturesAndErrors = createFeatureCollection(allPrimitives, monitor);

        FeatureCollection allFeatures = allFeaturesAndErrors.getKey();
        HashMap<OsmPrimitive, Throwable> allErrors = allFeaturesAndErrors.getValue();

        if (! allErrors.isEmpty()) {
            GuiHelper.runInEDT(() -> new Notification(
                    tr("Some items where ignored as their geometry is not compatible with the JTS library")
                    ).setIcon(JOptionPane.WARNING_MESSAGE).show());
            for (Map.Entry<OsmPrimitive, Throwable> entry : allErrors.entrySet()) {
                if (refPrimitives.contains(entry.getKey())) {
                    refPrimitives.remove(entry.getKey());
                    settings.referenceSelection.remove(entry.getKey());
                }
                if (subPrimitives.contains(entry.getKey())) {
                    subPrimitives.remove(entry.getKey());
                    settings.subjectSelection.remove(entry.getKey());
                }
            }
        }


        FeatureCollection refColl = new FeatureDataset(allFeatures.getFeatureSchema());
        FeatureCollection subColl = new FeatureDataset(allFeatures.getFeatureSchema());
        for (Feature f : allFeatures.getFeatures()) {
            OsmFeature osmFeature = (OsmFeature) f;
            if (refPrimitives.contains(osmFeature.getPrimitive()))
                refColl.add(osmFeature);
            if (subPrimitives.contains(osmFeature.getPrimitive()))
                subColl.add(osmFeature);
        }


        // Index the collection for efficient search with WindowMatcher
        refColl = new IndexedFeatureCollection(refColl);
        subColl = new IndexedFeatureCollection(subColl);

        //TODO: pass to MatchFinderPanel to use as hint/default for DistanceMatchers
        // get maximum possible distance so scores can be scaled (FIXME: not quite accurate)
        // Envelope envelope = refColl.getEnvelope();
        // envelope.expandToInclude(subColl.getEnvelope());
        // double maxDistance = Point2D.distance(
        //     envelope.getMinX(),
        //     envelope.getMinY(),
        //     envelope.getMaxX(),
        //     envelope.getMaxY());

        // build matcher
        FCMatchFinder finder = settings.matchFinder;

        // FIXME: ignore/filter duplicate objects (i.e. same object in both sets)
        // FIXME: fix match functions to work on point/linestring features as well
        // find matches
        Map<Feature, Matches> map = finder.match(refColl, subColl, new TaskMonitorJosmAdapter(monitor));

        monitor.subTask("Finishing");

        // convert to simple one-to-one match
        ArrayList<SimpleMatch> list = new ArrayList<>();
        for (Map.Entry<Feature, Matches> entry: map.entrySet()) {
            OsmFeature target = (OsmFeature) entry.getKey();
            OsmFeature subject = (OsmFeature) entry.getValue().getTopMatch();
            if (target != null && subject != null)
                list.add(new SimpleMatch(target.getPrimitive(), subject.getPrimitive(),
                        entry.getValue().getTopScore()));
        }
        return list;
    }

    /**
     * Create FeatureSchema using union of all keys from all selected primitives
     */
    private static FeatureSchema createSchema(Collection<OsmPrimitive> prims) {
        Set<String> keys = new HashSet<>();
        for (OsmPrimitive prim : prims) {
            keys.addAll(prim.getKeys().keySet());
        }
        FeatureSchema schema = new FeatureSchema();
        schema.addAttribute("__GEOMETRY__", AttributeType.GEOMETRY);
        for (String key : keys) {
            schema.addAttribute(key, AttributeType.STRING);
        }
        return schema;
    }

    private static AbstractMap.SimpleEntry<FeatureCollection,HashMap<OsmPrimitive, Throwable>>
        createFeatureCollection(Collection<OsmPrimitive> prims, ProgressMonitor monitor)
    {
        FeatureDataset dataset = new FeatureDataset(createSchema(prims));
        HashMap<OsmPrimitive, Throwable> errorset = new HashMap<OsmPrimitive, Throwable>();
        //TODO: use factory instead of passing converter
        JTSConverter converter = new JTSConverter(true);
        for (OsmPrimitive prim : prims) {
            // Relations not supported yet
            try {
                if (!(prim instanceof Relation))
                    dataset.add(new OsmFeature(prim, converter));
            } catch (Throwable e) {
                Logger.getLogger(MatchesComputation.class.getName()).log(Level.WARNING, "Element skipped: cannot convert OSM primitive geometry to JTS feature", e);
                errorset.put(prim, e);
            }
        }
        return new AbstractMap.SimpleEntry<FeatureCollection,HashMap<OsmPrimitive, Throwable>>(dataset, errorset);
    }

    /**
     * Progress monitor for use with JCS linked to a JOSM ProgressMonitor.
     */
    private static class TaskMonitorJosmAdapter implements TaskMonitor {

        private final ProgressMonitor josmMonitor;
        private final HashMap<String, String> translations = new HashMap<>();
        {
            translations.put("Finding matches", tr("Finding matches"));
            translations.put("Sorting scores", tr("Sorting scores"));
            translations.put("Discarding inferior matches", tr("Discarding inferior matches"));
        }

        TaskMonitorJosmAdapter(ProgressMonitor josmMonitor) {
            this.josmMonitor = josmMonitor;
        }

        @Override
        public void report(String description) {
            josmMonitor.subTask(translations.getOrDefault(description, description));
        }

        @Override
        public void report(int itemsDone, int totalItems, String itemDescription) {
            josmMonitor.setTicksCount(totalItems);
            josmMonitor.setTicks(itemsDone);
        }

        @Override
        public void report(Exception exception) {
            throw new UnsupportedOperationException("Not supported yet.", exception);
        }

        @Override
        public void allowCancellationRequests() {
        }

        @Override
        public boolean isCancelRequested() {
            return josmMonitor.isCanceled();
        }
    }
}

/*
 * The JCS Conflation Suite (JCS) is a library of Java classes that
 * can be used to build automated or semi-automated conflation solutions.
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
package com.vividsolutions.jcs.conflate.polygonmatch;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.util.Assert;
import com.vividsolutions.jump.feature.BasicFeature;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;
import com.vividsolutions.jump.feature.IndexedFeatureCollection;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.CollectionUtil;
import com.vividsolutions.jump.util.CoordinateArrays;

/**
 *  An FCMatchFinder wrapper that also treats unions of adjacent target features
 *  as themselves target features. Such unions are formed into composite target
 *  features. These composites are temporary -- before the results are returned,
 *  each composite is split into its constituent features. <P>
 *
 *  The result returned is a one-to-one mapping of target feature to matched
 *  candidate feature; the one-to-one mapping is achieved by discarding all
 *  matches except for those with the highest scores, for each feature (target
 *  and matched candidate). <P>
 *
 *  Note on composites: if a composite's top score is higher than the top score
 *  of each of its constituents, the composite match is retained and constituent
 *  matches are discarded; otherwise, the composite match is discarded and
 *  constituent matches are retained.
 */
public class TargetUnioningFCMatchFinder implements FCMatchFinder {
    private FCMatchFinder matchFinder;
    private int maxCompositeSize;
    /**
     *@param  maxCompositeSize  the maximum number of adjacent target features to
     *      try combining
     *@param  matchFinder       the FCMatchFinder to wrap
     */
    public TargetUnioningFCMatchFinder(int maxCompositeSize, FCMatchFinder matchFinder) {
        this.maxCompositeSize = maxCompositeSize;
        this.matchFinder = matchFinder;
    }
    @Override
    public Map<Feature, Matches> match(
        FeatureCollection targetFC,
        FeatureCollection candidateFC,
        TaskMonitor monitor) {
        monitor.allowCancellationRequests();
        FeatureCollection compositeTargetFC = createCompositeFC(targetFC, monitor);
        Map<Feature, Matches> compositeTargetFeatureToMatchesMap =
            matchFinder.match(compositeTargetFC, candidateFC, monitor);
        compositeTargetFeatureToMatchesMap =
            disambiguateCompositeTargetConstituents(
                compositeTargetFeatureToMatchesMap,
                candidateFC.getFeatureSchema(),
                monitor);
        createUnionIDs(compositeTargetFeatureToMatchesMap, monitor);
        Map<Feature, Matches> filteredTargetToMatchesMap =
            splitCompositeTargets(compositeTargetFeatureToMatchesMap, monitor);
        //Zero-score targets will have been filtered out. Put them back. [Jon Aquino]
        Map<Feature, Matches> targetToMatchesMap =
            AreaFilterFCMatchFinder.blankTargetToMatchesMap(
                targetFC.getFeatures(),
                candidateFC.getFeatureSchema());
        targetToMatchesMap.putAll(filteredTargetToMatchesMap);
        return targetToMatchesMap;
    }
    private List<Feature> lastTargetConstituents;
    private List<Integer> lastUnionIDs;
    private void createUnionIDs(final Map<Feature, Matches> compositeTargetFeatureToMatchesMap, TaskMonitor monitor) {
        monitor.report("Creating union IDs");
        List<Feature> compositeTargets = new ArrayList<>(compositeTargetFeatureToMatchesMap.keySet());
        Collections.sort(compositeTargets, new Comparator<Feature>() {
            @Override
            public int compare(Feature o1, Feature o2) {
                double s1 = compositeTargetFeatureToMatchesMap.get(o1).getTopScore();
                double s2 = compositeTargetFeatureToMatchesMap.get(o2).getTopScore();
                return s1 < s2 ? -1 : s1 > s2 ? 1 : 0;
            }
        });
        lastTargetConstituents = new ArrayList<>();
        lastUnionIDs = new ArrayList<>();
        int unionID = 0;
        for (int i = 0; i < compositeTargets.size(); i++) {
            monitor.report(i+1, compositeTargets.size(), "unions");
            CompositeFeature compositeTarget = (CompositeFeature) compositeTargets.get(i);
            if (compositeTarget.getFeatures().size() == 1) {
                continue;
            }
            unionID++;
            for (Feature targetConstituent : compositeTarget.getFeatures()) {
                lastTargetConstituents.add(targetConstituent);
                lastUnionIDs.add(Integer.valueOf(unionID));
            }
        }
    }
    protected FeatureCollection createCompositeFC(
        FeatureCollection fc,
        TaskMonitor monitor) {
        FeatureCollection compositeFC = new FeatureDataset(fc.getFeatureSchema());
        Set<CompositeFeature> composites = createCompositeSet(fc, monitor);
        add(composites, compositeFC, monitor);
        return new IndexedFeatureCollection(compositeFC);
    }
    /**
     * Returns a composite-target-to-Matches map in which each target constituent will be
     * found in at most one composite target. Does not disambiguate composite targets
     * or matches (use DisambiguatingFCMatchFinder to do that), just composite target
     * constituents.
     */
    protected Map<Feature, Matches> disambiguateCompositeTargetConstituents(
        Map<Feature, Matches> compositeTargetToMatchesMap,
        FeatureSchema candidateSchema,
        TaskMonitor monitor) {
        List<Feature> targetConstituentsEncountered = new ArrayList<>();
        List<Feature> compositeTargets = new ArrayList<>();
        List<Feature> candidates = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        SortedSet<DisambiguationMatch> matchSet =
            DisambiguationMatch.createDisambiguationMatches(compositeTargetToMatchesMap, monitor);
        monitor.report("Discarding inferior composite matches");
        int j = 0;
        outer : for (DisambiguationMatch match : matchSet) {
            monitor.report(++j, matchSet.size(), "matches");
            for (Feature targetConstituent : ((CompositeFeature) match.getTarget()).getFeatures()) {
                if (targetConstituentsEncountered.contains(targetConstituent)) {
                    continue outer;
                }
            }
            compositeTargets.add(match.getTarget());
            candidates.add(match.getCandidate());
            scores.add(Double.valueOf(match.getScore()));
            targetConstituentsEncountered.addAll(((CompositeFeature) match.getTarget()).getFeatures());
        }
        Map<Feature, Matches> newMap = new HashMap<>();
        for (int i = 0; i < compositeTargets.size(); i++) {
            Matches matches = new Matches(candidateSchema);
            matches.add(
                candidates.get(i),
                scores.get(i).doubleValue());
            newMap.put(compositeTargets.get(i), matches);
        }
        return newMap;
    }
    private List<Feature> featuresWithCommonEdge(Feature feature, FeatureCollection fc) {
        List<Feature> featuresWithCommonEdge = new ArrayList<>();
        List<Feature> candidates = fc.query(feature.getGeometry().getEnvelopeInternal());
        for (Feature candidate : candidates) {
            if (feature == candidate
                || shareEdge(feature.getGeometry(), candidate.getGeometry())) {
                featuresWithCommonEdge.add(candidate);
            }
        }
        return featuresWithCommonEdge;
    }
    protected boolean shareEdge(Geometry a, Geometry b) {
        Set<Edge> aEdges = edges(a);
        Set<Edge> bEdges = edges(b);
        for (Edge bEdge : bEdges) {
            if (aEdges.contains(bEdge)) {
                return true;
            }
        }
        return false;
    }
    private static class Edge implements Comparable<Edge> {
        private Coordinate p0, p1;
        public Edge(Coordinate a, Coordinate b) {
            if (a.compareTo(b) < 1) {
                p0 = a;
                p1 = b;
            } else {
                p0 = b;
                p1 = a;
            }
        }
        @Override
        public int compareTo(Edge other) {
            int result = p0.compareTo(other.p0);
            if (result != 0)
                return result;
            return p1.compareTo(other.p1);
        }
    }
    private Set<Edge> edges(Geometry g) {
        Set<Edge> edges = new TreeSet<>();
        for (Coordinate[] coordinates : CoordinateArrays.toCoordinateArrays(g, false)) {
            for (int j = 1; j < coordinates.length; j++) { //1
                edges.add(new Edge(coordinates[j], coordinates[j - 1]));
            }
        }
        return edges;
    }
    /**
     *  Splits each composite target into its constituent features.
     */
    protected Map<Feature, Matches> splitCompositeTargets(Map<Feature, Matches> compositeToMatchesMap, TaskMonitor monitor) {
        monitor.report("Splitting composites");
        int compositesProcessed = 0;
        int totalComposites = compositeToMatchesMap.size();
        Map<Feature, Matches> newMap = new HashMap<>();
        for (Iterator<Feature> i = compositeToMatchesMap.keySet().iterator();
            i.hasNext() && !monitor.isCancelRequested();
            ) {
            CompositeFeature composite = (CompositeFeature) i.next();
            compositesProcessed++;
            monitor.report(compositesProcessed, totalComposites, "composites");
            Matches matches = compositeToMatchesMap.get(composite);
            for (Feature targetConstituent : composite.getFeatures()) {
                Assert.isTrue(!newMap.containsKey(targetConstituent));
                newMap.put(targetConstituent, matches.clone());
            }
        }
        return newMap;
    }
    private Set<CompositeFeature> createCompositeSet(FeatureCollection fc, TaskMonitor monitor) {
        monitor.report("Creating composites of adjacent features");
        int featuresProcessed = 0;
        int totalFeatures = fc.getFeatures().size();
        //Use a Set to prevent duplicate composites [Jon Aquino]
        Set<CompositeFeature> composites = new HashSet<>();
        for (Iterator<Feature> i = fc.getFeatures().iterator();
            i.hasNext() && !monitor.isCancelRequested();
            ) {
            Feature feature = i.next();
            featuresProcessed++;
            monitor.report(featuresProcessed, totalFeatures, "features");
            List<Feature> featuresWithCommonEdge = featuresWithCommonEdge(feature, fc);
            for (Iterator<List<Feature>> j =
                CollectionUtil
                    .combinations(featuresWithCommonEdge, maxCompositeSize, feature)
                    .iterator();
                j.hasNext() && !monitor.isCancelRequested();
                ) {
                List<Feature> combination = j.next();
                composites.add(new CompositeFeature(fc.getFeatureSchema(), combination));
            }
        }
        return composites;
    }

    public static class CompositeFeature extends BasicFeature {
        private List<Feature> features;
        private int hashCode;
        public CompositeFeature(FeatureSchema schema, List<Feature> features) {
            super(schema);
            this.features = features;
            Geometry union = features.get(0).getGeometry();
            hashCode = features.get(0).hashCode();
            for (int i = 1; i < features.size(); i++) {
                Feature feature = features.get(i);
                union = union.union(feature.getGeometry());
                hashCode = Math.min(hashCode, feature.hashCode());
            }
            setGeometry(union);
        }
        public List<Feature> getFeatures() {
            return features;
        }
        @Override
        public boolean equals(Object obj) {
            Assert.isTrue(obj instanceof CompositeFeature, obj.getClass().toString());
            CompositeFeature other = (CompositeFeature) obj;
            if (features.size() != other.features.size()) {
                return false;
            }
            for (Feature myFeature : features) {
                if (!other.features.contains(myFeature)) {
                    return false;
                }
            }
            return true;
        }
        @Override
        public int hashCode() {
            return hashCode;
        }
    }
    private void add(Collection<? extends Feature> features, FeatureCollection fc, TaskMonitor monitor) {
        monitor.report("Building feature-collection");
        fc.addAll(features);
    }
    public Integer getUnionID(Feature target) {
        int i = lastTargetConstituents.indexOf(target);
        if (i == -1) { return null; }
        return lastUnionIDs.get(i);
    }
}

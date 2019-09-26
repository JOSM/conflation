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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.locationtech.jts.util.Assert;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.task.TaskMonitor;

/**
 * Enforces a one-to-one relationship between target features and
 * matched candidate features, in the returned result set.
 * "Conservative" because only top matches are allowed.
 * <P>
 * <b>Note:</b> DisambiguatingFCMatchFinder seems to give better results.
 * <P>
 * Example: OneToOneFCMatchFinder wraps a FCMatchFinder that returns the
 * following matches (and scores): T1-C1 (0.8), T2-C1 (0.9), T2-C2 (0.8),
 * T2-C3 (1.0), T3-C4 (0.5). T1 and T2 are from the target dataset, whereas
 * C1, C2, and C3 are from the candidate dataset. OneToOneFCMatchFinder filters
 * out all matches except the top ones, for each feature, leaving:
 * T2-C3 (1.0), T3-C4 (0.5).
 * * @see DisambiguatingFCMatchFinder
 */
public class TopMatchDisambiguatingFCMatchFinder implements FCMatchFinder {

    private FCMatchFinder matchFinder;

    public TopMatchDisambiguatingFCMatchFinder(FCMatchFinder matchFinder) {
        this.matchFinder = matchFinder;
    }

    @Override
    public Map<Feature, Matches> match(
        FeatureCollection targetFC,
        FeatureCollection candidateFC,
        TaskMonitor monitor) {
        Map<Feature, Matches> originalTargetToMatchesMap =
            matchFinder.match(targetFC, candidateFC, monitor);
        monitor.allowCancellationRequests();
        monitor.report("Finding best forward matches");
        Map<Feature, Matches> bestForwardMatches = filterMatches(originalTargetToMatchesMap, monitor);
        monitor.report("Finding best reverse matches");
        Map<Feature, Matches> bestReverseMatches =
            filterMatches(invert(originalTargetToMatchesMap, monitor), monitor);
        monitor.report("Finding common best matches");
        //Want matches that are "best" regardless of whether forward or reverse.
        //This is the only scheme I can think of right now that will satisfy
        //the case described in the class comment. [Jon Aquino]
        Map<Feature, Matches> filteredTargetToMatchesMap =
            commonMatches(
                bestForwardMatches,
                invert(bestReverseMatches, monitor),
                monitor);
        //Put back the targets that were filtered out (albeit with no matches). [Jon Aquino]
        Map<Feature, Matches> targetToMatchesMap =
            AreaFilterFCMatchFinder.blankTargetToMatchesMap(
                targetFC.getFeatures(),
                candidateFC.getFeatureSchema());
        targetToMatchesMap.putAll(filteredTargetToMatchesMap);
        return targetToMatchesMap;
    }

    private Map<Feature, Matches> commonMatches(
        Map<Feature, Matches> featureToMatchesMap1,
        Map<Feature, Matches> featureToMatchesMap2,
        TaskMonitor monitor) {
        int featuresProcessed = 0;
        int totalFeatures = featureToMatchesMap1.size();
        Map<Feature, Matches> commonMatches = new HashMap<>();
        for (Iterator<Feature> i = featureToMatchesMap1.keySet().iterator();
            i.hasNext() && !monitor.isCancelRequested();
            ) {
            Feature key1 = i.next();
            featuresProcessed++;
            monitor.report(featuresProcessed, totalFeatures, "features");
            if (!featureToMatchesMap2.containsKey(key1)) {
                continue;
            }
            Matches matches1 = featureToMatchesMap1.get(key1);
            Matches matches2 = featureToMatchesMap2.get(key1);
            if (matches1.getTopMatch() == matches2.getTopMatch()) {
                Assert.isTrue(matches1.getTopScore() == matches2.getTopScore());
                commonMatches.put(key1, matches1);
            }
        }
        return commonMatches;
    }

    private Map<Feature, Matches> filterMatches(Map<Feature, Matches> featureToMatchesMap, TaskMonitor monitor) {
        int featuresProcessed = 0;
        int totalFeatures = featureToMatchesMap.size();
        Map<Feature, Matches> newMap = new HashMap<>();
        if (featureToMatchesMap.isEmpty()) {
            return newMap;
        }
        for (Iterator<Feature> i = featureToMatchesMap.keySet().iterator();
            i.hasNext() && !monitor.isCancelRequested();
            ) {
            Feature feature = i.next();
            featuresProcessed++;
            monitor.report(featuresProcessed, totalFeatures, "features filtered");
            Matches oldMatches = featureToMatchesMap.get(feature);
            if (oldMatches.isEmpty()) {
                continue;
            }
            Matches newMatches = new Matches(oldMatches.getFeatureSchema());
            newMatches.add(oldMatches.getTopMatch(), oldMatches.getTopScore());
            newMap.put(feature, newMatches);
        }
        return newMap;
    }

    protected Map<Feature, Matches> invert(Map<Feature, Matches> featureToMatchesMap, TaskMonitor monitor) {
        int featuresProcessed = 0;
        int totalFeatures = featureToMatchesMap.size();
        Map<Feature, Matches> newMap = new HashMap<>();
        if (featureToMatchesMap.isEmpty()) {
            return newMap;
        }
        for (Iterator<Feature> i = featureToMatchesMap.keySet().iterator();
            i.hasNext() && !monitor.isCancelRequested();
            ) {
            Feature oldKey = i.next();
            featuresProcessed++;
            monitor.report(featuresProcessed, totalFeatures, "features inverted");
            Matches oldMatches = featureToMatchesMap.get(oldKey);
            for (int j = 0; j < oldMatches.size(); j++) {
                Feature newKey = oldMatches.getFeature(j);
                Matches newMatches = newMap.get(newKey);
                if (newMatches == null) {
                    newMatches = new Matches(oldKey.getSchema());
                }
                newMatches.add(oldKey, oldMatches.getScore(j));
                newMap.put(newKey, newMatches);
            }
        }
        return newMap;
    }
}

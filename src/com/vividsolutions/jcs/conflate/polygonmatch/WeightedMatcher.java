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
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.locationtech.jts.util.Assert;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureSchema;

/**
 * Runs multiple FeatureMatchers, and combines their scores using a weighted
 * average.
 */
public class WeightedMatcher implements FeatureMatcher {

    /**
   * Creates a WeightedMatcher with the given matchers and their weights.
   * @param matchersAndWeights alternates between Doubles and FeatureMatchers
   */
  public WeightedMatcher(Object... matchersAndWeights) {
    Objects.requireNonNull(matchersAndWeights);
    Assert.isTrue(matchersAndWeights.length % 2 == 0);
    for (int i = 0; i < matchersAndWeights.length; i += 2) {
      add((FeatureMatcher) matchersAndWeights[i+1],
          ((Number) matchersAndWeights[i]).doubleValue());
      //Number rather than Double so parties (e.g. Jython) can pass in Integers. [Jon Aquino]
    }
  }

  /**
   * Adds a matcher to the WeightedMatcher's matchers. If weight is 0, the
   * matcher will be ignored.
   * @param matcher a matcher to add
   * @param weight the weight given to scores returned by the matcher
   */
  private void add(FeatureMatcher matcher, double weight) {
    Assert.isTrue(weight >= 0);
    if (weight == 0) {
        return;
    }
    this.matcherToWeightMap.put(matcher, weight);
  }

  private final Map<FeatureMatcher, Double> matcherToWeightMap = new HashMap<>();

  /**
   * Searches a collection of candidate features for those that match the given
   * target feature, using each FeatureMatcher.
   * @param target the feature to match
   * @param candidates the features to search for matches
   * @return the candidates that pass at least one FeatureMatcher. Each score is
   * a weighted average of the scores from the FeatureMatchers.
   */
    @Override
  public Matches match(Feature target, FeatureCollection candidates) {
    if (weightTotal() == 0) { return new Matches(candidates.getFeatureSchema()); }
    Map<FeatureMatcher, Matches> matcherToMatchesMap = matcherToMatchesMap(target, candidates);
    Map<Feature, Double> featureToScoreMap = featureToScoreMap(matcherToMatchesMap);
    return toMatches(featureToScoreMap, candidates.getFeatureSchema());
  }

  private Matches toMatches(Map<Feature, Double> featureToScoreMap, FeatureSchema schema) {
    Matches matches = new Matches(schema);
    featureToScoreMap.forEach(matches::add);
    return matches;
  }

  private Map<FeatureMatcher, Matches> matcherToMatchesMap(Feature feature, FeatureCollection candidates) {
    HashMap<FeatureMatcher, Matches> matcherToMatchesMap = new HashMap<>();
    for (FeatureMatcher matcher : matcherToWeightMap.keySet()) {
      if (normalizedWeight(matcher) == 0) { continue; }
      matcherToMatchesMap.put(matcher, matcher.match(feature, candidates));
    }
    return matcherToMatchesMap;
  }

  private Map<Feature, Double> featureToScoreMap(Map<FeatureMatcher, Matches> matcherToMatchesMap) {
    Map<Feature, Double> featureToScoreMap = new TreeMap<>();
    matcherToMatchesMap.forEach((matcher, matches) -> this.addToFeatureToScoreMap(matches, matcher, featureToScoreMap));
    return featureToScoreMap;
  }

  private void addToFeatureToScoreMap(Matches matches, FeatureMatcher matcher,
                                      Map<Feature, Double> featureToScoreMap) {

    for (Map.Entry<Feature, Double> entry : matches.entrySet()) {
      double score = entry.getValue() * this.normalizedWeight(matcher);
      featureToScoreMap.compute(entry.getKey(), (key, value) -> value == null ? score : value + score);
    }
  }

  private double normalizedWeight(FeatureMatcher matcher) {
    return this.matcherToWeightMap.get(matcher) / this.weightTotal();
  }

  private double weightTotal() {
    return this.matcherToWeightMap.values().stream().mapToDouble(Double::doubleValue).sum();
  }
}

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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.util.AssertionFailedException;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.feature.FeatureDataset;
import com.vividsolutions.jump.feature.FeatureSchema;

/**
 * A FeatureCollection that stores the "score" of each Feature.  The score is
 * a number between 0.0 and 1.0 that indicates the confidence of a match.
 */
public class Matches extends AbstractMap<Feature, Double> implements FeatureCollection, Cloneable {
    private final Set<Map.Entry<Feature, Double>> entrySet = new HashSet<>();
    /**
     * Creates a Matches object.
     * @param schema metadata applicable to the features that will be stored in
     * this Matches object
     */
    public Matches(FeatureSchema schema) {
        this(schema, Collections.emptyList());
    }

    @Override
    protected Matches clone() {
        Matches clone = new Matches(dataset.getFeatureSchema());
        for (int i = 0; i < size(); i++) {
            clone.add(getFeature(i), getScore(i));
        }
        return clone;
    }

    /**
     * Creates a Matches object, initialized with the given Feature's.
     * @param schema metadata applicable to the features that will be stored in
     * this Matches object
     * @param features added to the Matches, each with the max score (1.0)
     */
    public Matches(FeatureSchema schema, List<Feature> features) {
        // We want to ensure that the dataset won't have a ton of ArrayList#grow calls
        // So we initialize the dataset with all the data
        this.dataset = new FeatureDataset(features.size(), schema);
        this.scores = new double[features.size()];

        for (Feature match : features) {
            add(match, 1);
        }
    }

    private final FeatureDataset dataset;
    private double[] scores = new double[0];

    /**
     * This method is not supported, because added features need to be associated
     * with a score. Use #add(Feature, double) instead.
     * @param feature a feature to add as a match
     * @see #add(Feature, double)
     */
    @Override
    public void add(Feature feature) {
        throw new UnsupportedOperationException("Use #add(feature, score) instead");
    }

    /**
     * This method is not supported, because added features need to be associated
     * with a score. Use #add(Feature, double) instead.
     */
    @Override
    public void addAll(Collection<? extends Feature> features) {
        throw new UnsupportedOperationException("Use #add(feature, score) instead");
    }

    /**
     * This method is not supported, because added features need to be associated
     * with a score. Use #add(Feature, double) instead.
     * @param feature a feature to add as a match
     * @see #add(Feature, double)
     */
    public void add(int index, Feature feature) {
        throw new UnsupportedOperationException("Use #add(feature, score) instead");
    }

    /**
     * This method is not supported, because Matches should not normally need to
     * have matches removed.
     */
    @Override
    public Collection<Feature> remove(Envelope envelope) {
        //If we decide to implement this, remember to remove the corresponding
        //score. [Jon Aquino]
        throw new UnsupportedOperationException();
    }

    /**
     * This method is not supported, because Matches should not normally need to
     * have matches removed.
     */
    @Override
    public void clear() {
        //If we decide to implement this, remember to remove the corresponding
        //score. [Jon Aquino]
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<Feature, Double>> entrySet() {
        if (this.size() == this.entrySet.size()) {
            return this.entrySet;
        }
        return updateEntrySet();
    }

    private synchronized Set<Entry<Feature, Double>> updateEntrySet() {
        if (this.size() != this.entrySet.size()) {
            this.entrySet.clear();
            for (int i = 0; i < this.size(); i++) {
                this.entrySet.add(new SimpleEntry<>(this.getFeature(i), this.getScore(i)));
            }
        }
        return this.entrySet;
    }

    /**
     * This method is not supported, because Matches should not normally need to
     * have matches removed.
     */
    @Override
    public void removeAll(Collection<Feature> features) {
        //If we decide to implement this, remember to remove the corresponding
        //score. [Jon Aquino]
        throw new UnsupportedOperationException();
    }

    /**
     * This method is not supported, because Matches should not normally need to
     * have matches removed.
     * @param feature a feature to remove
     */
    @Override
    public void remove(Feature feature) {
        //If we decide to implement this, remember to remove the corresponding
        //score. [Jon Aquino]
        throw new UnsupportedOperationException();
    }
    /**
     * Adds a match. Features with zero-scores are ignored.
     * @param feature a feature to add as a match
     * @param score the confidence of the match, ranging from 0 to 1
     */
    public void add(Feature feature, double score) {
        // We want to avoid the string concatenation here, if we don't need it.
        // It is *very* expensive when run with large datasets.
        // This used to be an Assert.isTrue statement
        if (0 > score || score > 1) {
            throw new AssertionFailedException("Score = " + score);
        }
        if (score == 0) {
            return;
        }
        scoreAdd(dataset.size(), score);
        dataset.add(feature);
        if (score > topScore) {
            topScore = score;
            topMatch = feature;
        }
    }

    private void scoreAdd(int index, double score) {
        if (this.scores.length < index + 1) {
            this.scores = Arrays.copyOf(this.scores, index + 1);
        }
        this.scores[index] = score;
    }

    private Feature topMatch;
    private double topScore = 0;

    public double getTopScore() {
        return topScore;
    }

    /**
     * @return the feature with the highest score
     */
    public Feature getTopMatch() {
        return topMatch;
    }

    /**
     * Returns the score of the ith feature
     * @param i 0, 1, 2, ...
     * @return the confidence of the ith match
     */
    public double getScore(int i) {
        return scores[i];
    }

    @Override
    public FeatureSchema getFeatureSchema() {
        return dataset.getFeatureSchema();
    }

    @Override
    public Envelope getEnvelope() {
        return dataset.getEnvelope();
    }

    @Override
    public int size() {
        return dataset.size();
    }

    @Override
    public boolean isEmpty() {
        return dataset.isEmpty();
    }

    public Feature getFeature(int index) {
        return dataset.getFeature(index);
    }

    @Override
    public List<Feature> getFeatures() {
        return dataset.getFeatures();
    }

    @Override
    public Iterator<Feature> iterator() {
        return dataset.iterator();
    }

    @Override
    public List<Feature> query(Envelope envelope) {
        return dataset.query(envelope);
    }
}

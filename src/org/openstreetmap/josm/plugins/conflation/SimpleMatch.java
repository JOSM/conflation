// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation;

import java.util.Arrays;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * This class represents a potential match, i.e. a pair of primitives, a score
 * and related information.
 */
public class SimpleMatch implements Comparable<SimpleMatch> {

    final OsmPrimitive referenceObject;
    final OsmPrimitive subjectObject;
    final double score;
    final double distance;

    public SimpleMatch(OsmPrimitive referenceObject,
            OsmPrimitive subjectObject, double score) {
        this(referenceObject, subjectObject, score,
                // TODO: use distance calculated in score function, and make sure it's in meters?
                ConflationUtils.getCenter(referenceObject).distance(ConflationUtils.getCenter(subjectObject)));
    }

    public SimpleMatch(OsmPrimitive referenceObject,
            OsmPrimitive subjectObject, double score, double distance) {
        CheckParameterUtil.ensureParameterNotNull(referenceObject, "referenceObject");
        CheckParameterUtil.ensureParameterNotNull(subjectObject, "subjectObject");
        this.referenceObject = referenceObject;
        this.subjectObject = subjectObject;
        this.score = score;
        this.distance = distance;
    }

    public OsmPrimitive getReferenceObject() {
        return referenceObject;
    }

    public OsmPrimitive getSubjectObject() {
        return subjectObject;
    }

    public Object getScore() {
        return score;
    }

    public Object getDistance() {
        return distance;
    }

    /**
     * Return the TagCollection for the resulting object of the conflation of this match.
     * The resulting tag collection depends on the provided settings, in particular the configured
     * restricted list of tags to merge or to overwrite without confirmation.
     *
     * @param settings the configaration setting to use for the conflation.
     * @return the TagCollection for the resulting object of the conflation of this match.
     */
    public TagCollection getMergingTagCollection(SimpleMatchSettings settings) {
        if ((subjectObject.getId() > 0) && (referenceObject.getId() > 0)
                && settings.isReplacingGeometry && referenceObject.getDataSet() == subjectObject.getDataSet()) {
            // In this situation we are conflating already existing OSM objects (getId() >0)
            // which are on the same DataSet so one will be deleted by the ReplaceGeometryCommand.
            // We don't wan't to silently delete important tags, so in this particular situation we force
            // tag merging dialogue for user confirmation by returning a full tag collection:
            return TagCollection.unionOfAllPrimitives(Arrays.asList(subjectObject, referenceObject));
        } else {
            // Else we respect user conflation settings:
            TagCollection tagCollection = TagCollection.from(subjectObject);
            for (Tag refTag : referenceObject.getKeys().getTags()) {
                if (!refTag.getValue().equals(subjectObject.get(refTag.getKey()))) {
                    if (settings.overwriteTags.contains(refTag.getKey())) {
                        tagCollection.removeByKey(refTag.getKey());
                        tagCollection.add(refTag);
                    } else if (settings.forceKeepTags.contains(refTag.getKey())) {
                        // do nothing if we want to actually preserve the subject value
                    } else if (settings.mergeTags.contains(refTag.getKey()) || (referenceObject.getId() > 0)) {
                        tagCollection.add(refTag);
                    }
                }
            }
            return tagCollection;
        }
    }

    @Override
    public int compareTo(SimpleMatch o) {
        int comp = Double.compare(this.score, o.score);
        if (comp == 0) {
            comp = -Double.compare(this.distance, o.distance); // (-) greater distance is no good
            if (comp == 0) {
                comp = referenceObject.compareTo(o.referenceObject);
                if (comp == 0) {
                    comp = subjectObject.compareTo(o.subjectObject);
                }
            }
        }
        return comp;
    }

    @Override
    public int hashCode() {
        return referenceObject.hashCode() ^ subjectObject.hashCode();
    }
}

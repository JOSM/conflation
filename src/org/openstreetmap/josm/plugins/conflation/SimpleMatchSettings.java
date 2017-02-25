// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation;

import com.vividsolutions.jcs.conflate.polygonmatch.FCMatchFinder;
import java.util.List;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 *
 * @author joshdoe
 */
public class SimpleMatchSettings {
    private List<OsmPrimitive> subjectSelection;
    private List<OsmPrimitive> referenceSelection;
    private OsmDataLayer referenceLayer;
    private DataSet subjectDataSet;
    private OsmDataLayer subjectLayer;
    private DataSet referenceDataSet;
    private FCMatchFinder matchFinder;

    /**
     * @return the subjectSelection
     */
    public List<OsmPrimitive> getSubjectSelection() {
        return subjectSelection;
    }

    /**
     * @param subjectSelection the subjectSelection to set
     */
    public void setSubjectSelection(List<OsmPrimitive> subjectSelection) {
        this.subjectSelection = subjectSelection;
    }

    /**
     * @return the referenceSelection
     */
    public List<OsmPrimitive> getReferenceSelection() {
        return referenceSelection;
    }

    /**
     * @param referenceSelection the referenceSelection to set
     */
    public void setReferenceSelection(List<OsmPrimitive> referenceSelection) {
        this.referenceSelection = referenceSelection;
    }

    /**
     * @return the referenceLayer
     */
    public OsmDataLayer getReferenceLayer() {
        return referenceLayer;
    }

    /**
     * @param referenceLayer the referenceLayer to set
     */
    public void setReferenceLayer(OsmDataLayer referenceLayer) {
        this.referenceLayer = referenceLayer;
    }

    /**
     * @return the subjectDataSet
     */
    public DataSet getSubjectDataSet() {
        return subjectDataSet;
    }

    /**
     * @param subjectDataSet the subjectDataSet to set
     */
    public void setSubjectDataSet(DataSet subjectDataSet) {
        this.subjectDataSet = subjectDataSet;
    }

    /**
     * @return the subjectLayer
     */
    public OsmDataLayer getSubjectLayer() {
        return subjectLayer;
    }

    /**
     * @param subjectLayer the subjectLayer to set
     */
    public void setSubjectLayer(OsmDataLayer subjectLayer) {
        this.subjectLayer = subjectLayer;
    }

    /**
     * @return the referenceDataSet
     */
    public DataSet getReferenceDataSet() {
        return referenceDataSet;
    }

    /**
     * @param referenceDataSet the referenceDataSet to set
     */
    public void setReferenceDataSet(DataSet referenceDataSet) {
        this.referenceDataSet = referenceDataSet;
    }

    /**
     * @return the matchFinder
     */
    public FCMatchFinder getMatchFinder() {
        return matchFinder;
    }

    /**
     * @param matchFinder the matchFinder to set
     */
    public void setMatchFinder(FCMatchFinder matchFinder) {
        this.matchFinder = matchFinder;
    }
}

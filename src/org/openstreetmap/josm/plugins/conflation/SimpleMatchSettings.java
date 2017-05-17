// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation;

import com.vividsolutions.jcs.conflate.polygonmatch.FCMatchFinder;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Result of the configuration {@Link SettingsDialog}.
 * @author joshdoe
 */
public class SimpleMatchSettings {

    public List<OsmPrimitive> subjectSelection;
    public List<OsmPrimitive> referenceSelection;
    public OsmDataLayer referenceLayer;
    public DataSet subjectDataSet;
    public OsmDataLayer subjectLayer;
    public DataSet referenceDataSet;
    public FCMatchFinder matchFinder;

    /*=
     * If conflation should replace the geometry.
     */
    public boolean isReplacingGeometry;

    /**
     * List of tags to merge during conflation.
     * Should be set to the {@link All} constant to mean all tags.
     */
    public Collection<String> mergeTags;

    /**
     * List of tags to overwrite without confirmation during conflation.
     */
    public Collection<String> overwriteTags;

    /**
     * List of tags to keep without confirmation during conflation.
     */
    public Collection<String> forceKeepTags;


    /**
     * A Collection that always answer true when asked if it contains any object (except for the removed items!).
     */
    public static class All<E> implements Collection<E> {

        private HashSet<Object> removedItems = new HashSet<>();

        @Override public int size() {
            return Integer.MAX_VALUE; }

        @Override public boolean isEmpty() {
            return false; }

        @Override public boolean contains(Object o) {
            return !removedItems.contains(o); }

        @Override public boolean containsAll(Collection<?> c) {
            return !c.stream().anyMatch(removedItems::contains); }

        @Override public Iterator<E> iterator() {
            throw new UnsupportedOperationException(); }

        @Override public Object[] toArray() {
            throw new UnsupportedOperationException(); }

        @Override public <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException(); }

        @Override public boolean add(E e) {
            return removedItems.remove(e); }

        @Override public boolean remove(Object o) {
            return removedItems.add(o);
        }

        @Override public boolean addAll(Collection<? extends E> c) {
            return removedItems.removeAll(c); }

        @Override public boolean removeAll(Collection<?> c) {
            return removedItems.addAll(c);
        }

        @Override public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException(); }

        @Override public void clear() {
            throw new UnsupportedOperationException(); }
    }
}


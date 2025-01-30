// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;

import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.data.osm.TagCollection;

/**
 * Model for the conflation results table.
 */
class SimpleMatchesTableModel extends AbstractTableModel implements SimpleMatchListListener {

    private SimpleMatchList matches = null;
    private SimpleMatchSettings settings = null;

    private static final String[] columnNames = {tr("Reference"), tr("Subject"), "Distance (m)", "Score", "Tags"};

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        if (matches == null)
            return 0;
        return matches.size();
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (matches == null || row < 0 || row >= matches.size())
            return null;

        SimpleMatch c = matches.get(row);
        if (col == 0) {
            return c.getReferenceObject();
        } else if (col == 1) {
            return c.getSubjectObject();
        } else if (col == 2) {
            return c.getDistance();
        } else if (col == 3) {
            return c.getScore();
        } else if (col == 4) {
            if (c.getMergingTagCollection(settings).getKeysWithMultipleValues().isEmpty()) {
                TagCollection tags = TagCollection.unionOfAllPrimitives(Arrays.asList(c.getReferenceObject(), c.getSubjectObject()));
                if (tags.getKeysWithMultipleValues().isEmpty()) {
                    return "No conflicts!";
                } else {
                    return "Difference";
                }
            } else {
                return "Conflicts!";
            }
        }

        return null;
    }

    @Override
    public Class<?> getColumnClass(int c) {
        Object value = getValueAt(0, c);
        if (value != null) {
            return value.getClass();
        } else {
            return Object.class;
        }
    }

    /**
     * Return list of matches
     * @return the matches
     */
    public SimpleMatchList getMatches() {
        return matches;
    }

    /**
     * Set matches
     * @param matches the matches to set
     */
    public void setMatches(SimpleMatchList matches, SimpleMatchSettings settings) {
        this.settings = settings;
        if (matches != this.matches) {
            if (this.matches != null) {
                this.matches.removeConflationListChangedListener(this);
            }
            this.matches = matches;
            if (matches != null) {
                matches.addConflationListChangedListener(this);
            }
            fireTableDataChanged();
        }
    }

    /* ---------------------------------------------------------------------------------- */
    /* SimpleMatchListListener                                                            */
    /* ---------------------------------------------------------------------------------- */

    @Override
    public void simpleMatchListChanged(SimpleMatchList list) {
        fireTableDataChanged();
    }

    @Override
    public void simpleMatchListIntervalAdded(SimpleMatchList list, int index0, int index1) {
        fireTableRowsInserted(index0, index1);
    }

    @Override
    public void simpleMatchListIntervalRemoved(SimpleMatchList list, int index0, int index1) {
        fireTableRowsDeleted(index0, index1);
    }

}

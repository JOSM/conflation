// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation.command;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.Icon;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.conflation.UnmatchedObjectListModel;
import org.openstreetmap.josm.tools.ImageProvider;

public class RemoveUnmatchedObjectCommand extends Command {
    private final UnmatchedObjectListModel model;
    private final Collection<OsmPrimitive> objects;

    public RemoveUnmatchedObjectCommand(UnmatchedObjectListModel model,
            Collection<OsmPrimitive> objects) {
        super(MainApplication.getLayerManager().getEditDataSet());
        this.model = model;
        this.objects = objects;
    }

    @Override
    public boolean executeCommand() {
        boolean res = false;
        model.beginUpdate();
        try {
            res = model.removeAll(objects);
        } finally {
            model.endUpdate();
        }
        return res;
    }

    @Override
    public void undoCommand() {
        model.beginUpdate();
        try {
            model.addAll(objects);
        } finally {
            model.endUpdate();
        }
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
    }

    @Override
    public String getDescriptionText() {
        return tr(marktr("Remove {0} unmatched objects"), objects.size());
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("dialogs", "delete");
    }

    @Override
    public Collection<OsmPrimitive> getParticipatingPrimitives() {
        return objects;
    }
}

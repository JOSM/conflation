// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.Icon;

import org.openstreetmap.josm.command.AddPrimitivesCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.PseudoCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.conflation.ConflationUtils;
import org.openstreetmap.josm.plugins.conflation.UnmatchedObjectListModel;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 *  Command which copies objects from the reference to the subject layer.
 * @author joshdoe
 */
public class ConflateUnmatchedObjectCommand extends Command {

    private final AddPrimitivesCommand addPrimitivesCommand;
    private final Collection<OsmPrimitive> unmatchedObjects;
    private final UnmatchedObjectListModel listModel;

    public ConflateUnmatchedObjectCommand(OsmDataLayer sourceDataLayer, DataSet targetDataSet,
            Collection<OsmPrimitive> unmatchedObjects, UnmatchedObjectListModel listModel) {
        super(targetDataSet);
        this.unmatchedObjects = unmatchedObjects;
        this.listModel = listModel;

        List<PrimitiveData> newObjects = ConflationUtils.copyObjects(sourceDataLayer.getDataSet(), unmatchedObjects);

        addPrimitivesCommand = new AddPrimitivesCommand(newObjects, newObjects, targetDataSet);
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        addPrimitivesCommand.fillModifiedData(modified, deleted, added);
    }

    @Override
    public String getDescriptionText() {
        int size = unmatchedObjects.size();
        return trn("Added {0} object to subject layer", "Added {0} objects to subject layer", size, size);
    }

    @Override
    public boolean executeCommand() {
        listModel.beginUpdate();
        try {
            if (!addPrimitivesCommand.executeCommand())
                return false;
            listModel.removeAll(unmatchedObjects);
        } finally {
            listModel.endUpdate();
        }
        return true;
    }

    @Override
    public void undoCommand() {
        listModel.beginUpdate();
        try {
            addPrimitivesCommand.undoCommand();
            listModel.addAll(unmatchedObjects);
        } finally {
            listModel.endUpdate();
        }
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("dialogs", "conflation");
    }

    @Override
    public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        HashSet<OsmPrimitive> prims = new HashSet<>();
        prims.addAll(addPrimitivesCommand.getParticipatingPrimitives());
        prims.addAll(unmatchedObjects);
        return prims;
    }

    @Override
    public Collection<PseudoCommand> getChildren() {
        return Arrays.asList(addPrimitivesCommand);
    }
}

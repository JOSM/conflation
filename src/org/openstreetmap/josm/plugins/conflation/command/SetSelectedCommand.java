// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;

public class SetSelectedCommand extends Command {
    
    /** the id of primitives to select when executing the command */
    private Collection<? extends PrimitiveId> objects;
    
    /** the primitives to select when executing the command */
    private Collection<OsmPrimitive> newSelection;

    /** the selection before applying the new selection */
    private Collection<OsmPrimitive> oldSelection;
    
    public SetSelectedCommand(DataSet dataset, Collection<? extends PrimitiveId> objects) {
        super(dataset);
        this.objects = objects;
    }

    @Override
    public boolean executeCommand() {
        DataSet ds = getAffectedDataSet();
        oldSelection = ds.getSelected();
        if (newSelection == null) {
            newSelection = objects.stream().map(ds::getPrimitiveById).collect(Collectors.toList());
            objects = null;
        }
        ds.setSelected(newSelection);
        return true;
    }

    @Override
    public void undoCommand() {
        getAffectedDataSet().setSelected(oldSelection);
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        // Do nothing
    }

    @Override
    public String getDescriptionText() {
        int size = newSelection != null ? newSelection.size() : 0;
        return trn("Selected {0} object", "Selected {0} objects", size, size);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), objects, newSelection, oldSelection);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        SetSelectedCommand that = (SetSelectedCommand) obj;
        return Objects.equals(objects, that.objects) &&
                Objects.equals(newSelection, that.newSelection) &&
                Objects.equals(oldSelection, that.oldSelection);
    }
}

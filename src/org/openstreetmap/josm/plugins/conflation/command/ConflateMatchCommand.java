// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddPrimitivesCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.PseudoCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.gui.conflict.tags.CombinePrimitiveResolverDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.conflation.ConflationUtils;
import org.openstreetmap.josm.plugins.conflation.SimpleMatch;
import org.openstreetmap.josm.plugins.conflation.SimpleMatchList;
import org.openstreetmap.josm.plugins.conflation.SimpleMatchSettings;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryException;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryUtils;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.UserCancelException;


/**
 * Command to conflate one object with another.
 */
public class ConflateMatchCommand extends Command {
    private final SimpleMatch match;
    private final SimpleMatchList matchesList;
    private final SimpleMatchSettings settings;
    private SequenceCommand command; // may be null
    private boolean isBuilt;

    /**
     * Conflate a single match, using the given settings.
     * @param match the match to conflate
     * @param matchesList the list of match to update if success (remove the conflated match)
     * @param settings the settings that tell how to conflate.
     */
    public ConflateMatchCommand(SimpleMatch match,
            SimpleMatchList matchesList, SimpleMatchSettings settings) {
        super(settings.subjectLayer);
        this.match = match;
        this.matchesList = matchesList;
        this.settings = settings;
        // REM: the real commands are only built at execution time to ensure
        // that many ConflateMatchCommand could be combined in a SequenecCommand even
        // if a ConflateMatchCommand would impact a following ConflateMatchCommand.
        // This is the case for instance when ReplaceeGeometry modify the relations
        // members, this modifications need to be taken into account to compute
        // the next ReplaceeGeometryCommand, if not, relation membership would
        // be overwritten, see ticket https://josm.openstreetmap.de/ticket/7665
    }

    @Override
    public boolean executeCommand() {
        if (!isBuilt) {
            isBuilt = true;
            command = buildCommand();
        }
        return (command != null) ? command.executeCommand() : false;
    }

    @Override
    public void undoCommand() {
        if (command != null)
            command.undoCommand();
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        if (command != null)
            command.fillModifiedData(modified, deleted, added);
    }

    @Override
    public String getDescriptionText() {
        //TODO: make more descriptive
        return tr("Conflate object pair");
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("dialogs", "conflation");
    }

    @Override
    public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        if (command != null)
            return command.getParticipatingPrimitives();
        else
            return Arrays.asList();
    }

    @Override
    public Collection<PseudoCommand> getChildren() {
        if (command != null)
            return command.getChildren();
        else
            return Arrays.asList();
    }

    /**
     * Build the full command sequence.
     * @return the built command or null in case of error or user cancellation.
     */
    private SequenceCommand buildCommand() {
        List<Command> list = (settings.isReplacingGeometry) ?
                buildCopyAndReplaceGeometryCommand(match, settings)
                : buildTagMergingCommand(match, settings);
        if (list == null) {
            return null;
        } else {
            list.add(new RemoveMatchCommand(matchesList, Arrays.asList(match)));
            return new SequenceCommand("", list);
        }
    }

    /**
     * Built Replace Geometry Command (that also merge tags).
     *
     * Support a reference object in a different DataSet than the subject's one, in which case it will be copied.
     *
     * Respect the settings for tag merging conflict.
     * Will remove tags that were not supposed to be kept.
     *
     * @return the commands list or null if user canceled or error occurred.
     */
    public static List<Command> buildCopyAndReplaceGeometryCommand(SimpleMatch match, SimpleMatchSettings settings) {
        OsmPrimitive referenceObject = match.getReferenceObject();
        OsmPrimitive subjectObject = match.getSubjectObject();
        TagCollection tagCollection = match.getMergingTagCollection(settings);
        List<Command> commands = new ArrayList<>(3);
        if (settings.subjectLayer != settings.referenceLayer) {
            List<Command> copyPrimitiveCommand = buildCopyPrimitiveCommand(referenceObject, settings.subjectLayer);
            commands.addAll(copyPrimitiveCommand);
            if (!copyPrimitiveCommand.get(0).executeCommand()) {
                return null;
            }
            try {
                commands.add(buildReplaceGeometryCommand(subjectObject,
                        // get the copied reference object:
                        subjectObject.getDataSet().getPrimitiveById(referenceObject.getPrimitiveId()),
                        tagCollection));
            } finally {
                copyPrimitiveCommand.get(0).undoCommand();
            }
        } else {
            commands.add(buildReplaceGeometryCommand(subjectObject, referenceObject, tagCollection));
        }
        if (commands.get(commands.size()-1) == null) {
            // could be null because of UserCancel or error.
            return null;
        } else {
            // Remove keys untouched by ReplaceGeometryCommand but we don't want in our tagCollection
            Stream.concat(referenceObject.getKeys().keySet().stream(), subjectObject.getKeys().keySet().stream())
                .filter(key -> !tagCollection.hasTagsFor(key))
                .forEach(key -> {
                    // I don't know who will survive ReplaceGeometryCommand: referenceObject or subjectObject.
                    // In the doubt I modify both:
                    commands.add(new ChangePropertyCommand(referenceObject, key, null));
                    commands.add(new ChangePropertyCommand(subjectObject, key, null));
                });
            return commands;
        }
    }

    /**
     * Built Replace Geometry Command (that also merge tags).
     * @return the commands list or null if user canceled or error occurred.
     */
    public static Command buildReplaceGeometryCommand(OsmPrimitive subjectObject, OsmPrimitive referenceObject, TagCollection tagCollection) {
        // save and remove tags to avoid unwanted tags conflicts dialog:
        TagMap savedReferenceTags = saveAndRemoveTagsNotInCollection(referenceObject, tagCollection);
        TagMap savedSubjectTags = saveAndRemoveTagsNotInCollection(subjectObject, tagCollection);
        Command command = null;
        try {
            command = ReplaceGeometryUtils.buildReplaceCommand(subjectObject, referenceObject);
        } catch (ReplaceGeometryException ex) {
            AutoScaleAction.zoomTo(Arrays.asList(subjectObject, referenceObject));
            JOptionPane.showMessageDialog(Main.parent,
                    ex.getMessage(), tr("Cannot replace geometry."), JOptionPane.INFORMATION_MESSAGE);
        } finally {
            referenceObject.setKeys(savedReferenceTags);
            subjectObject.setKeys(savedSubjectTags);
        }
        return command;
    }

    public static List<Command> buildCopyPrimitiveCommand(OsmPrimitive referenceObject, OsmDataLayer layer) {
        List<PrimitiveData> newObjects = ConflationUtils.copyObjects(referenceObject.getDataSet(), referenceObject);
        List<Command> commands = new ArrayList<>(2);
        // We don't want to fireSelectionChangedEvent as it would degrade performance if we batch many Conflate commands together.
        // So we pass null as second argument to AddPrimitivesCommand, and select the new items ourself:
        commands.add(new AddPrimitivesCommand(newObjects, null, layer));
        commands.add(new SetSelectedCommand(layer.data, newObjects, false));
        return commands;
    }

    /**
     * Built Tag only Merging Command
     * @return the commands list or null if user canceled.
     */
    public static List<Command> buildTagMergingCommand(SimpleMatch match, SimpleMatchSettings settings) {
        // Temporarily remove relation membership to avoid conflict dialog about them, we won't really
        // combine the primitives, we just want to combine the tags:
        HashMap<Relation, RelationData> savedRelationsData = saveAndRemoveRelationMembersFor(match.getReferenceObject());
        saveAndRemoveRelationMembersFor(match.getSubjectObject()).forEach(savedRelationsData::putIfAbsent);
        try {
            return CombinePrimitiveResolverDialog.launchIfNecessary(
                    match.getMergingTagCollection(settings),
                    Arrays.asList(match.getReferenceObject(), match.getSubjectObject()),
                    Collections.singleton(match.getSubjectObject()));
        } catch (UserCancelException e) {
            return null;
        } finally {
            restoreRelationsData(savedRelationsData);
        }
    }

    private static HashMap<Relation, RelationData> saveAndRemoveRelationMembersFor(OsmPrimitive primitive) {
        HashMap<Relation, RelationData> savedData = new HashMap<>();
        for (Relation r: OsmPrimitive.getFilteredList(primitive.getReferrers(), Relation.class)) {
            savedData.put(r, r.save());
            r.removeMembersFor(primitive);
        }
        return savedData;
    }

    private static void restoreRelationsData(HashMap<Relation, RelationData> savedData) {
        savedData.entrySet().stream().forEach(entry -> entry.getKey().load(entry.getValue()));
    }

    private static TagMap saveAndRemoveTagsNotInCollection(OsmPrimitive primitive, TagCollection tagCollection) {
        TagMap savedTags = primitive.getKeys();
        for (Tag tag: savedTags.getTags()) {
            if (!tagCollection.contains(tag)) {
                primitive.remove(tag.getKey());
            }
        }
        return savedTags;
    }
}

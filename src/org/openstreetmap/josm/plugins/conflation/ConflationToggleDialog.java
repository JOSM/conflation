// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.PrimitiveRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.plugins.conflation.command.ConflateMatchCommand;
import org.openstreetmap.josm.plugins.conflation.command.ConflateUnmatchedObjectCommand;
import org.openstreetmap.josm.plugins.conflation.command.MoveMatchToUnmatchedCommand;
import org.openstreetmap.josm.plugins.conflation.command.RemoveMatchCommand;
import org.openstreetmap.josm.plugins.conflation.command.RemoveUnmatchedObjectCommand;
import org.openstreetmap.josm.plugins.conflation.command.StopOnErrorSequenceCommand;
import org.openstreetmap.josm.plugins.conflation.config.SettingsDialog;
import org.openstreetmap.josm.spi.preferences.IPreferences;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

public class ConflationToggleDialog extends ToggleDialog
implements DataSelectionListener, DataSetListener, SimpleMatchListListener, LayerChangeListener {

    public static final String TITLE_PREFIX = tr("Conflation");
    final JTabbedPane tabbedPane;
    final JTable matchTable;
    final JList<OsmPrimitive> referenceOnlyList;
    final UnmatchedObjectListModel referenceOnlyListModel;
    final JList<OsmPrimitive> subjectOnlyList;
    final UnmatchedObjectListModel subjectOnlyListModel;
    ConflationLayer conflationLayer; // may be null
    final SimpleMatchesTableModel matchTableModel;
    SimpleMatchList matches = new SimpleMatchList();;
    SimpleMatchSettings settings;  // may be null
    final SettingsDialog settingsDialog; // null if headless
    final ConflateAction conflateAction;
    final SideButton conflateButton;
    final RemoveAction removeAction;
    final SideButton removeButton;
    final ZoomToListSelectionAction zoomToListSelectionAction;
    final SelectionPopup selectionPopup;

    // Keep track of conflation cases automatically removed (because of corresponding primitives removal),
    // to be able to restore them (in case of Undo)
    private final HashSet<OsmPrimitive> primitivesRemovedReferenceOnly = new HashSet<>();
    private final HashSet<OsmPrimitive> primitivesRemovedSubjectOnly = new HashSet<>();
    private final HashMap<OsmPrimitive, SimpleMatch> primitivesRemovedMatchByReference = new HashMap<>();
    private final HashMap<OsmPrimitive, SimpleMatch> primitivesRemovedMatchBySubject = new HashMap<>();

    public ConflationToggleDialog(ConflationPlugin conflationPlugin, IPreferences pref) {
        // TODO: create shortcut?
        super(TITLE_PREFIX, "conflation.png", tr("Activates the conflation plugin"),
                null, 150);

        if (!GraphicsEnvironment.isHeadless()) {
            settingsDialog = new SettingsDialog(pref) {
                @Override
                protected void buttonAction(int buttonIndex, ActionEvent evt) {
                    if (buttonIndex == 0) {
                        if (checkValidityOrNotifyProblems()) {
                            super.buttonAction(buttonIndex, evt);
                            ConflationToggleDialog.this.clear(true, true, false);
                            ConflationToggleDialog.this.settings = this.getSettings();
                            ConflationToggleDialog.this.settingsDialog.savePreferences(pref);
                            ConflationToggleDialog.this.performMatching();
                        } else {
                            // do nothing
                        }
                    } else {
                        super.buttonAction(buttonIndex, evt);
                    }
                }
            };
            settingsDialog.setModalityType(Dialog.ModalityType.MODELESS);
        } else {
            settingsDialog = null;
        }

        // create table to show matches and allow multiple selections
        matchTableModel = new SimpleMatchesTableModel();
        matchTable = new JTable(matchTableModel);

        // add selection handler, to center/zoom view
        matchTable.getSelectionModel().addListSelectionListener(new MatchListSelectionHandler());

        matchTable.getColumnModel().getColumn(0).setCellRenderer(new PrimitiveRenderer());
        matchTable.getColumnModel().getColumn(1).setCellRenderer(new PrimitiveRenderer());
        matchTable.getColumnModel().getColumn(4).setCellRenderer(new ColorTableCellRenderer("Tags"));

        matchTable.setRowSelectionAllowed(true);
        matchTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        matchTable.setAutoCreateRowSorter(true);

        referenceOnlyListModel = new UnmatchedObjectListModel();
        referenceOnlyList = new JList<>(referenceOnlyListModel);
        referenceOnlyList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        referenceOnlyList.setCellRenderer(new PrimitiveRenderer());
        referenceOnlyList.setTransferHandler(null); // no drag & drop

        subjectOnlyListModel = new UnmatchedObjectListModel();
        subjectOnlyList = new JList<>(subjectOnlyListModel);
        subjectOnlyList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        subjectOnlyList.setCellRenderer(new PrimitiveRenderer());
        subjectOnlyList.setTransferHandler(null); // no drag & drop

        //add popup menu for zoom on selection
        zoomToListSelectionAction = new ZoomToListSelectionAction();
        selectionPopup = new SelectionPopup();
        SelectionPopupMenuLauncher launcher = new SelectionPopupMenuLauncher();
        matchTable.addMouseListener(launcher);
        subjectOnlyList.addMouseListener(launcher);
        referenceOnlyList.addMouseListener(launcher);

        //on enter key zoom to selection
        InputMapUtils.addEnterAction(matchTable, zoomToListSelectionAction);
        InputMapUtils.addEnterAction(subjectOnlyList, zoomToListSelectionAction);
        InputMapUtils.addEnterAction(referenceOnlyList, zoomToListSelectionAction);

        DoubleClickHandler dblClickHandler = new DoubleClickHandler();
        matchTable.addMouseListener(dblClickHandler);
        referenceOnlyList.addMouseListener(dblClickHandler);
        subjectOnlyList.addMouseListener(dblClickHandler);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab(tr("Matches"), new JScrollPane(matchTable));
        tabbedPane.addTab(tr("Reference only"), new JScrollPane(referenceOnlyList));
        tabbedPane.addTab(tr("Subject only"), new JScrollPane(subjectOnlyList));

        conflateAction = new ConflateAction();
        conflateButton = new SideButton(conflateAction);
        // TODO: don't need this arrow box now, but likely will shortly
        // conflateButton.createArrow(new ActionListener() {
        //     @Override
        //     public void actionPerformed(ActionEvent e) {
        //         ConflatePopupMenu.launch(conflateButton);
        //     }
        // });

        removeAction = new RemoveAction();
        removeButton = new SideButton(removeAction);

        // add listeners to update enable state of buttons
        matchTable.getSelectionModel().addListSelectionListener(conflateAction);
        matchTable.getSelectionModel().addListSelectionListener(removeAction);
        tabbedPane.addChangeListener(conflateAction);
        tabbedPane.addChangeListener(removeAction);
        referenceOnlyList.addListSelectionListener(conflateAction);
        referenceOnlyList.addListSelectionListener(removeAction);
        subjectOnlyList.addListSelectionListener(conflateAction);
        subjectOnlyList.addListSelectionListener(removeAction);

        UnmatchedListDataListener unmatchedListener = new UnmatchedListDataListener();
        subjectOnlyListModel.addListDataListener(unmatchedListener);
        referenceOnlyListModel.addListDataListener(unmatchedListener);

        createLayout(tabbedPane, false, Arrays.asList(new SideButton[]{
                new SideButton(new ConfigureAction()),
                conflateButton,
                removeButton
                // new SideButton("Replace Geometry", false),
                // new SideButton("Merge Tags", false),
                // new SideButton("Remove", false)
        }));
    }

    /* ---------------------------------------------------------------------------------- */
    /* SimpleMatchListListener                                                            */
    /* ---------------------------------------------------------------------------------- */

    @Override
    public void simpleMatchListChanged(SimpleMatchList list) {
        updateTabTitles();
    }

    @Override
    public void simpleMatchListIntervalAdded(SimpleMatchList list, int index0, int index1) {
        updateTabTitles();
    }

    @Override
    public void simpleMatchListIntervalRemoved(SimpleMatchList list, int index0, int index1) {
        updateTabTitles();
    }

    private void updateTabTitles() {
        tabbedPane.setTitleAt(
                tabbedPane.indexOfComponent(matchTable.getParent().getParent()),
                tr(marktr("Matches ({0})"), matches.size()));
        tabbedPane.setTitleAt(
                tabbedPane.indexOfComponent(referenceOnlyList.getParent().getParent()),
                tr(marktr("Reference only ({0})"), referenceOnlyListModel.getSize()));
        tabbedPane.setTitleAt(
                tabbedPane.indexOfComponent(subjectOnlyList.getParent().getParent()),
                tr(marktr("Subject only ({0})"), subjectOnlyListModel.getSize()));
    }

    private Component getSelectedTabComponent() {
        return ((JScrollPane) tabbedPane.getSelectedComponent()).getViewport().getView();
    }

    private Stream<SimpleMatch> getSelectedMatchesStream() {
        return IntStream.of(matchTable.getSelectedRows()).map(matchTable::convertRowIndexToModel).mapToObj(matches::get);
    }

    private Collection<SimpleMatch> getSelectedMatches() {
        return getSelectedMatchesStream().collect(Collectors.toList());
    }

    private List<OsmPrimitive> getSelectedReferencePrimitives() {
        List<OsmPrimitive> selection = new ArrayList<>();
        if (tabbedPane == null || getSelectedTabComponent() == null)
            return selection;

        if (getSelectedTabComponent().equals(matchTable)) {
            selection.addAll(getSelectedMatchesStream().map(SimpleMatch::getReferenceObject).collect(Collectors.toList()));
        } else if (getSelectedTabComponent().equals(referenceOnlyList)) {
            selection.addAll(referenceOnlyList.getSelectedValuesList());
        }
        return selection;
    }

    private List<OsmPrimitive> getSelectedSubjectPrimitives() {
        List<OsmPrimitive> selection = new ArrayList<>();
        if (tabbedPane == null || getSelectedTabComponent() == null)
            return selection;

        if (getSelectedTabComponent().equals(matchTable)) {
            selection.addAll(getSelectedMatchesStream().map(SimpleMatch::getSubjectObject).collect(Collectors.toList()));
        } else if (getSelectedTabComponent().equals(subjectOnlyList)) {
            selection.addAll(subjectOnlyList.getSelectedValuesList());
        }
        return selection;
    }

    private boolean isSelectionEmpty() {
        if (tabbedPane == null || getSelectedTabComponent() == null)
            return true;

        if (getSelectedTabComponent().equals(matchTable)) {
            return matchTable.getSelectedRow() < 0;
        } else if (getSelectedTabComponent().equals(referenceOnlyList)) {
            return referenceOnlyList.getSelectedIndex() < 0;
        } else if (getSelectedTabComponent().equals(subjectOnlyList)) {
            return subjectOnlyList.getSelectedIndex() < 0;
        } else {
            throw new AssertionError();
        }
    }

    private Collection<OsmPrimitive> getAllSelectedPrimitives() {
        Collection<OsmPrimitive> allSelected = new HashSet<>();
        allSelected.addAll(getSelectedReferencePrimitives());
        allSelected.addAll(getSelectedSubjectPrimitives());
        return allSelected;
    }

    private void selectAllListSelectedPrimitives() {
        List<OsmPrimitive> refSelected = getSelectedReferencePrimitives();
        List<OsmPrimitive> subSelected = getSelectedSubjectPrimitives();

        //clear current selection and add list-selected primitives, handling both
        //same and different reference/subject layers
        settings.referenceDataSet.clearSelection();
        settings.subjectDataSet.clearSelection();
        SelectionEventManager.getInstance().removeSelectionListener(this);
        try {
            settings.referenceDataSet.addSelected(refSelected);
            settings.subjectDataSet.addSelected(subSelected);
        } finally {
            SelectionEventManager.getInstance().addSelectionListener(this);
        }
    }

    class DoubleClickHandler extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() < 2 || !SwingUtilities.isLeftMouseButton(e))
                return;

            selectAllListSelectedPrimitives();
            // zoom/center on selection
            AutoScaleAction.zoomTo(getAllSelectedPrimitives());
        }
    }

    public class ConfigureAction extends JosmAction {

        public ConfigureAction() {
            // TODO: settle on sensible shortcuts
            super(tr("Configure"), "preference", tr("Configure conflation options"),
                    null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            settingsDialog.setVisible(true);
        }
    }

    @Override
    public void showNotify() {
        super.showNotify();
        SelectionEventManager.getInstance().addSelectionListener(this);
        MainApplication.getLayerManager().addLayerChangeListener(this);
    }

    @Override
    public void hideNotify() {
        super.hideNotify();
        SelectionEventManager.getInstance().removeSelectionListener(this);
        MainApplication.getLayerManager().removeLayerChangeListener(this);
        clear(true, true, true);
        if (settingsDialog != null) {
            settingsDialog.setVisible(false);
            settingsDialog.clear(true, true);
        }
        this.settings = null;
    }

    private void clear(boolean shouldClearReference, boolean shouldClearSubject, boolean shouldRemoveConflationLayer) {
        if (shouldRemoveConflationLayer && (conflationLayer != null)) {
            if (MainApplication.getLayerManager().containsLayer(conflationLayer)) {
                MainApplication.getLayerManager().removeLayer(conflationLayer);
            }
            conflationLayer = null;
        }
        if (settings != null) {
            if (shouldClearReference) {
                DataSet dataSet = settings.referenceDataSet;
                if (dataSet != null) {
                    dataSet.removeDataSetListener(this);
                    settings.referenceDataSet = null;
                }
                settings.referenceLayer = null;
                settings.referenceSelection = null;
            }
            if (shouldClearSubject) {
                DataSet dataSet = settings.subjectDataSet;
                if (dataSet != null) {
                    dataSet.removeDataSetListener(this);
                    settings.subjectDataSet = null;
                }
                settings.subjectLayer = null;
                settings.subjectSelection = null;
            }
        }
        clearListsContentAndListeners();
    }

    private void clearListsContentAndListeners() {
        primitivesRemovedReferenceOnly.clear();
        primitivesRemovedSubjectOnly.clear();
        primitivesRemovedMatchByReference.clear();
        primitivesRemovedMatchBySubject.clear();
        matches.clear();
        matches.removeAllConflationListChangedListener();
        referenceOnlyListModel.clear();
        subjectOnlyListModel.clear();
        updateTabTitles();
    }

    private void setListsContentAddListnersAndLayer(SimpleMatchList matchList,
            Collection<OsmPrimitive> referenceOnlyList, Collection<OsmPrimitive> subjectOnlyList) {
        clearListsContentAndListeners();
        matches = matchList;
        matchTableModel.setMatches(matches, settings);
        matches.addConflationListChangedListener(this);
        // add conflation layer
        try {
            if (conflationLayer == null) {
                conflationLayer = new ConflationLayer(matches, (i) -> matchTable.isRowSelected(matchTable.convertRowIndexToView(i)));
            }
            if (!MainApplication.getLayerManager().containsLayer(conflationLayer)) {
                MainApplication.getLayerManager().addLayer(conflationLayer);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(), ex.toString(), "Error adding conflation layer", JOptionPane.ERROR_MESSAGE);
        }
        if (conflationLayer != null) {
            conflationLayer.setMatches(matches);
        }
        referenceOnlyListModel.addAll(referenceOnlyList);
        subjectOnlyListModel.addAll(subjectOnlyList);
        updateTabTitles();
        settings.subjectDataSet.addDataSetListener(ConflationToggleDialog.this);
        settings.referenceDataSet.addDataSetListener(ConflationToggleDialog.this);
    }

    /* ---------------------------------------------------------------------------------- */
    /* SelectionChangedListener                                                           */
    /* ---------------------------------------------------------------------------------- */

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        if (!isShowing() || (isDocked && isCollapsed)) return;
        Set<OsmPrimitive> newSelection = event.getSelection();
        if (newSelection.size() > 0) {
            ListSelectionModel refOnlySelModel = referenceOnlyList.getSelectionModel();
            ListSelectionModel subOnlySelModel = subjectOnlyList.getSelectionModel();
            ListSelectionModel matchSelModel = matchTable.getSelectionModel();
            if ((newSelection.size() <= 50) &&
                    (matches.size() >= 400 || referenceOnlyListModel.getSize() >= 400 || subjectOnlyListModel.getSize() >= 400)) {
                refOnlySelModel.setValueIsAdjusting(true);
                refOnlySelModel.clearSelection();
                subOnlySelModel.setValueIsAdjusting(true);
                subOnlySelModel.clearSelection();
                matchSelModel.setValueIsAdjusting(true);
                matchSelModel.clearSelection();
                newSelection.forEach(this::addPrimitiveToSelection);
                refOnlySelModel.setValueIsAdjusting(false);
                subOnlySelModel.setValueIsAdjusting(false);
                matchSelModel.setValueIsAdjusting(false);
            } else {
                HashSet<OsmPrimitive> selectionSet = new HashSet<>(newSelection);
                Predicate<SimpleMatch> isMatchSelected = (m) ->
                    selectionSet.contains(m.getReferenceObject()) || selectionSet.contains(m.getSubjectObject());
                select(matchSelModel, matches.size(), (i) -> isMatchSelected.test(matches.get(matchTable.convertRowIndexToModel(i))));
                select(refOnlySelModel, referenceOnlyListModel.getSize(), (i) -> selectionSet.contains(referenceOnlyListModel.getElementAt(i)));
                select(subOnlySelModel, subjectOnlyListModel.getSize(), (i) -> selectionSet.contains(subjectOnlyListModel.getElementAt(i)));
            }
            if (matchSelModel.getMinSelectionIndex() >= 0) {
                tabbedPane.setSelectedIndex(0);
                matchTable.scrollRectToVisible(new Rectangle(matchTable.getCellRect(matchSelModel.getMinSelectionIndex(), 0, true)));
            } else if (refOnlySelModel.getMinSelectionIndex() >= 0) {
                tabbedPane.setSelectedIndex(1);
            } else if (subOnlySelModel.getMinSelectionIndex() >= 0) {
                tabbedPane.setSelectedIndex(2);
            }
            scrollListToSelected(referenceOnlyList);
            scrollListToSelected(subjectOnlyList);
        }
    }

    private static void select(ListSelectionModel selectionModel, int size, IntPredicate predicate) {
        selectionModel.setValueIsAdjusting(true);
        selectionModel.clearSelection();
        int i = 0;
        while (i < size) {
            while (i < size && !predicate.test(i)) i++;
            if (i == size) break;
            int j = i + 1;
            while (j < size && predicate.test(j)) j++;
            selectionModel.addSelectionInterval(i, j - 1);
            i = j + 1;
        }
        selectionModel.setValueIsAdjusting(false);
    }

    private static void scrollListToSelected(JList<?> jlist) {
        ListSelectionModel selectionModel = jlist.getSelectionModel();
        int index = selectionModel.getMinSelectionIndex();
        if (index >= 0)
            jlist.ensureIndexIsVisible(index);
    }

    private void addPrimitiveToSelection(OsmPrimitive object) {
        SimpleMatch c = matches.getMatchByReference(object);
        if (c == null) {
            c = matches.getMatchBySubject(object);
        }
        if (c != null) {
            int index = matches.indexOf(c);
            if (index >= 0) {
                index = matchTable.convertRowIndexToView(index);
                matchTable.getSelectionModel().addSelectionInterval(index, index);
            }
        }
        int index = referenceOnlyListModel.indexOf(object);
        if (index >= 0) {
            referenceOnlyList.getSelectionModel().addSelectionInterval(index, index);
        }
        index = subjectOnlyListModel.indexOf(object);
        if (index >= 0) {
            subjectOnlyList.getSelectionModel().addSelectionInterval(index, index);
        }
    }

    //    protected static class ConflateMenuItem extends JMenuItem implements ActionListener {
    //        public ConflateMenuItem(String name) {
    //            super(name);
    //            addActionListener(this); //TODO: is this needed?
    //        }
    //
    //        @Override
    //        public void actionPerformed(ActionEvent e) {
    //            //TODO: do something!
    //        }
    //    }
    //
    //    protected static class ConflatePopupMenu extends JPopupMenu {
    //
    //        public static void launch(Component parent) {
    //            JPopupMenu menu = new ConflatePopupMenu();
    //            Rectangle r = parent.getBounds();
    //            menu.show(parent, r.x, r.y + r.height);
    //        }
    //
    //        public ConflatePopupMenu() {
    //            add(new ConflateMenuItem("Use reference geometry, reference tags"));
    //            add(new ConflateMenuItem("Use reference geometry, subject tags"));
    //            add(new ConflateMenuItem("Use subject geometry, reference tags"));
    //        }
    //    }

    class MatchListSelectionHandler implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent lse) {
            if ((conflationLayer != null) && !lse.getValueIsAdjusting()) {
                MainApplication.getMap().mapView.repaint();
            }
        }
    }

    static class ColorTableCellRenderer extends JLabel implements TableCellRenderer {

        private final String columnName;

        ColorTableCellRenderer(String column) {
            this.columnName = column;
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Object columnValue = table.getValueAt(row, table.getColumnModel().getColumnIndex(columnName));

            if (value != null) {
                setText(value.toString());
            }
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
                if ("Conflicts!".equals(columnValue)) {
                    setBackground(java.awt.Color.red);
                } else if ("Difference".equals(columnValue)) {
                    setBackground(java.awt.Color.orange);
                } else {
                    setBackground(java.awt.Color.green);
                }
            }
            return this;
        }
    }

    //    public static class LayerListCellRenderer extends DefaultListCellRenderer {
    //
    //        @Override
    //        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
    //                boolean cellHasFocus) {
    //            Layer layer = (Layer) value;
    //            JLabel label = (JLabel) super.getListCellRendererComponent(list, layer.getName(), index, isSelected,
    //                    cellHasFocus);
    //            Icon icon = layer.getIcon();
    //            label.setIcon(icon);
    //            label.setToolTipText(layer.getToolTipText());
    //            return label;
    //        }
    //    }

    /**
     * Deactivate listener events while performing some batch actions.
     *
     * When performing actuals actions (conflate items, remove from list, etc.), we execute command
     * one for each row selected in the table. This result in datatable events emitted for each row.
     * Since we do NOT want to treat these events in the middle of the batch, we  decativate all
     * listeners before executing commands. On end, we recativate them.
     */
    abstract class BatchAction extends JosmAction {

        BatchAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean registerInToolbar) {
            super(name, iconName, tooltip, shortcut, registerInToolbar);
        }

        @Override
        public final void actionPerformed(ActionEvent e) {
            saveSelection();
            beginUpdate();
            try {
                actualActionPerformed(e);
            } finally {
                endUpdate();
                restoreSelection();
            }
        }

        public abstract void actualActionPerformed(ActionEvent e);

        private void beginUpdate() {
            SelectionEventManager.getInstance().removeSelectionListener(ConflationToggleDialog.this);
            if (settings.subjectDataSet != null) {
                settings.subjectDataSet.beginUpdate();
            }
            matches.beginUpdate();
            referenceOnlyListModel.beginUpdate();
            subjectOnlyListModel.beginUpdate();
        }

        private void endUpdate() {
            SelectionEventManager.getInstance().addSelectionListener(ConflationToggleDialog.this);
            if (settings.subjectDataSet != null) {
                settings.subjectDataSet.endUpdate();
            }
            matches.endUpdate();
            referenceOnlyListModel.endUpdate();
            subjectOnlyListModel.endUpdate();
        }

        private SimpleMatch nextMatchSelection;
        private HashSet<SimpleMatch> oldMatchesSelection = new HashSet<>();
        private HashSet<OsmPrimitive> oldReferenceOnlySelection = new HashSet<>();
        private HashSet<OsmPrimitive> oldSubjectOnlySelection = new HashSet<>();

        private void saveSelection() {
            nextMatchSelection = getNextMatchToSelect();
            oldMatchesSelection.addAll(getSelectedMatches());
            oldReferenceOnlySelection.addAll(referenceOnlyList.getSelectedValuesList());
            oldSubjectOnlySelection.addAll(subjectOnlyList.getSelectedValuesList());
        }

        private void restoreSelection() {
            select(matchTable.getSelectionModel(), matches.size(),
                    (i) -> oldMatchesSelection.contains(matches.get(matchTable.convertRowIndexToModel(i))));
            select(referenceOnlyList.getSelectionModel(), referenceOnlyListModel.getSize(),
                    (i) -> oldReferenceOnlySelection.contains(referenceOnlyListModel.getElementAt(i)));
            select(subjectOnlyList.getSelectionModel(), subjectOnlyListModel.getSize(),
                    (i) -> oldSubjectOnlySelection.contains(subjectOnlyListModel.getElementAt(i)));
            if ((matchTable.getSelectedRow() < 0) && nextMatchSelection != null) {
                // If there is no match selected, we select a new one
                int index = matchTable.convertRowIndexToView(matches.indexOf(nextMatchSelection));
                matchTable.setRowSelectionInterval(index, index);
            }
            nextMatchSelection = null;
            oldMatchesSelection.clear();
            oldReferenceOnlySelection.clear();
            oldSubjectOnlySelection.clear();
       }
    }

    class RemoveAction extends BatchAction implements ChangeListener, ListSelectionListener {

        RemoveAction() {
            super(tr("Remove"), "dialogs/delete", tr("Remove selected matches"),
                    null, false);
        }

        @Override
        public void actualActionPerformed(ActionEvent e) {
            Component selComponent = getSelectedTabComponent();
            if (selComponent.equals(matchTable)) {
                if ((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
                    UndoRedoHandler.getInstance().add(new MoveMatchToUnmatchedCommand(matches, getSelectedMatches(),
                            referenceOnlyListModel, subjectOnlyListModel));
                } else {
                    UndoRedoHandler.getInstance().add(new RemoveMatchCommand(matches, getSelectedMatches()));
                }
            } else if (selComponent.equals(referenceOnlyList)) {
                UndoRedoHandler.getInstance().add(
                        new RemoveUnmatchedObjectCommand(referenceOnlyListModel,
                                referenceOnlyList.getSelectedValuesList()));
            } else if (selComponent.equals(subjectOnlyList)) {
                UndoRedoHandler.getInstance().add(
                        new RemoveUnmatchedObjectCommand(subjectOnlyListModel,
                                subjectOnlyList.getSelectedValuesList()));
            }
        }

        @Override
        public void updateEnabledState() {
            Component selComponent = getSelectedTabComponent();
            int selSize = 0;
            if (selComponent.equals(matchTable)) {
                selSize = matchTable.getSelectedRowCount();
            } else if (selComponent.equals(referenceOnlyList)) {
                selSize = referenceOnlyList.getSelectedValuesList().size();
            } else if (selComponent.equals(subjectOnlyList)) {
                selSize = subjectOnlyList.getSelectedValuesList().size();
            }
            if (removeButton != null) {
                removeButton.setText((selSize > 1) ? tr(marktr("Remove ({0})"), selSize) : tr("Remove"));
            }
            setEnabled(selSize > 0);
        }

        /* ------------------------------------------------------------------------------ */
        /* ChangeListener                                                                 */
        /* ------------------------------------------------------------------------------ */

        @Override
        public void stateChanged(ChangeEvent ce) {
            updateEnabledState();
        }

        /* ------------------------------------------------------------------------------ */
        /* ListSelectionListener                                                          */
        /* ------------------------------------------------------------------------------ */

        @Override
        public void valueChanged(ListSelectionEvent lse) {
            if (!lse.getValueIsAdjusting()) {
                updateEnabledState();
            }
        }

    }

    /**
     * Get the next SimpleMatch to select when the selection will be empty.
     * @return the next SimpleMatch to select.
     */
    private SimpleMatch getNextMatchToSelect() {
        int index = matchTable.getSelectedRow() + 1;
        while (index < matches.size() && matchTable.isRowSelected(index)) index++;
        return (index >= 0 && index < matches.size()) ? matches.get(matchTable.convertRowIndexToModel(index)) : null;
    }

    class ConflateAction extends BatchAction implements ChangeListener, ListSelectionListener {

        ConflateAction() {
            // TODO: make sure shortcuts make sense
            super(tr("Conflate"), "dialogs/conflation", tr("Conflate selected objects"),
                    Shortcut.registerShortcut("conflation:replace", tr("Conflation: {0}", tr("Replace")),
                            KeyEvent.VK_F, Shortcut.ALT_CTRL), false);
        }

        @Override
        public void actualActionPerformed(ActionEvent e) {
            if (getSelectedTabComponent().equals(matchTable))
                conflateMatchActionPerformed();
            else if (getSelectedTabComponent().equals(referenceOnlyList))
                conflateUnmatchedObjectActionPerformed();
        }

        private void conflateUnmatchedObjectActionPerformed() {
            List<OsmPrimitive> unmatchedObjects = referenceOnlyList.getSelectedValuesList();
            Command cmd = new ConflateUnmatchedObjectCommand(settings.referenceLayer,
                    settings.subjectDataSet, unmatchedObjects, referenceOnlyListModel);
            UndoRedoHandler.getInstance().add(cmd);
            // TODO: change active layer to subject ?
        }

        private void conflateMatchActionPerformed() {
            List<Command> cmds = getSelectedMatchesStream().map(
                    (match) -> new ConflateMatchCommand(match, matches, settings)).collect(Collectors.toList());
            if (cmds.size() == 1) {
                UndoRedoHandler.getInstance().add(cmds.get(0));
            } else if (cmds.size() > 1) {
                Command seqCmd = new StopOnErrorSequenceCommand(
                        settings.subjectDataSet, tr(marktr("Conflate {0} objects"), cmds.size()), true, cmds);
                UndoRedoHandler.getInstance().add(seqCmd);
            }
        }

        @Override
        public void updateEnabledState() {
            Component selComponent = getSelectedTabComponent();
            int selSize = 0;
            if (selComponent.equals(matchTable)) {
                selSize = matchTable.getSelectedRowCount();
            } else if (selComponent.equals(referenceOnlyList)) {
                selSize = referenceOnlyList.getSelectedValuesList().size();
            }
            if (conflateButton != null) {
                conflateButton.setText((selSize > 1) ? tr(marktr("Conflate ({0})"), selSize) : tr("Conflate"));
            }
            setEnabled(selSize > 0);
        }

        /* ------------------------------------------------------------------------------ */
        /* ChangeListener                                                                 */
        /* ------------------------------------------------------------------------------ */

        @Override
        public void stateChanged(ChangeEvent ce) {
            updateEnabledState();
        }

        /* ------------------------------------------------------------------------------ */
        /* ListSelectionListener                                                          */
        /* ------------------------------------------------------------------------------ */

        @Override
        public void valueChanged(ListSelectionEvent lse) {
            if (!lse.getValueIsAdjusting()) {
                updateEnabledState();
            }
        }

    }

    /**
     * The action for zooming to the primitives which are currently selected in
     * the list (either matches or single primitives).
     *
     */
    class ZoomToListSelectionAction extends BatchAction implements ListSelectionListener {
        ZoomToListSelectionAction() {
            super(tr("Zoom to selected primitive(s)"), "dialogs/autoscale/selection", tr("Zoom to selected primitive(s)"),
                    null, false);
        }

        @Override
        public void actualActionPerformed(ActionEvent e) {
            if (matchTable == null)
                return;

            Collection<OsmPrimitive> sel = getAllSelectedPrimitives();
            if (sel.isEmpty())
                return;
            AutoScaleAction.zoomTo(sel);
        }

        @Override
        public void updateEnabledState() {
            setEnabled(!isSelectionEmpty());
        }

        /* ------------------------------------------------------------------------------ */
        /* ListSelectionListener                                                          */
        /* ------------------------------------------------------------------------------ */

        @Override
        public void valueChanged(ListSelectionEvent lse) {
            if (!lse.getValueIsAdjusting()) {
                updateEnabledState();
            }
        }
    }

    /**
     * The action for selecting the primitives which are currently selected in
     * the list (either matches or single primitives).
     *
     */
    class SelectListSelectionAction extends BatchAction implements ListSelectionListener {
        SelectListSelectionAction() {
            super(tr("Select selected primitive(s)"), "dialogs/select", tr("Select the primitives currently selected in the list"),
                    null, false);
        }

        @Override
        public void actualActionPerformed(ActionEvent e) {
            if (matchTable == null)
                return;
            selectAllListSelectedPrimitives();
        }

        @Override
        public void updateEnabledState() {
            setEnabled(!isSelectionEmpty());
        }

        /* ------------------------------------------------------------------------------ */
        /* ListSelectionListener                                                                 */
        /* ------------------------------------------------------------------------------ */

        @Override
        public void valueChanged(ListSelectionEvent lse) {
            if (!lse.getValueIsAdjusting()) {
                updateEnabledState();
            }
        }
    }

    /**
     * The popup menu launcher
     */
    class SelectionPopupMenuLauncher extends PopupMenuLauncher {

        @Override
        public void launch(MouseEvent evt) {
            //if none selected, select row under cursor
            Component c = getSelectedTabComponent();
            int rowIndex = -1;
            if (c == matchTable) {
                //FIXME: this doesn't seem to be working
                rowIndex = matchTable.rowAtPoint(evt.getPoint());
                if (rowIndex >= 0) {
                    matchTable.addRowSelectionInterval(rowIndex, rowIndex);
                }
            } else if (c == subjectOnlyList || c == referenceOnlyList) {
                rowIndex = ((JList<?>) c).locationToIndex(evt.getPoint());
                if (rowIndex >= 0)
                    ((JList<?>) c).addSelectionInterval(rowIndex, rowIndex);
            }
            if (rowIndex >= 0) {
                selectionPopup.show(c, evt.getX(), evt.getY());
            }
        }
    }

    /**
     * The popup menu for the selection list
     */
    class SelectionPopup extends JPopupMenu {
        public SelectListSelectionAction selectListSelectionAction;
        SelectionPopup() {
            matchTable.getSelectionModel().addListSelectionListener(zoomToListSelectionAction);
            subjectOnlyList.addListSelectionListener(zoomToListSelectionAction);
            referenceOnlyList.addListSelectionListener(zoomToListSelectionAction);
            add(zoomToListSelectionAction);

            selectListSelectionAction = new SelectListSelectionAction();
            matchTable.getSelectionModel().addListSelectionListener(selectListSelectionAction);
            subjectOnlyList.addListSelectionListener(selectListSelectionAction);
            referenceOnlyList.addListSelectionListener(selectListSelectionAction);
            add(selectListSelectionAction);
        }
    }

    /* ---------------------------------------------------------------------------------- */
    /* DataSetListener                                                                    */
    /* ---------------------------------------------------------------------------------- */

    @Override
    public void primitivesAdded(PrimitivesAddedEvent event) {
        // In case of primitive re-added because of Undo action, restore the
        // corresponding conflation lists case.
        if (settings != null) {
            DataSet dataSet = event.getDataset();
            if (dataSet == settings.referenceDataSet) {
                for (OsmPrimitive p : event.getPrimitives()) {
                    SimpleMatch m = primitivesRemovedMatchByReference.remove(p);
                    if (m != null) {
                        matches.add(m);
                    }
                    if (primitivesRemovedReferenceOnly.remove(p)) {
                        referenceOnlyListModel.addElement(p);
                    }
                }
            } else if (dataSet == settings.subjectDataSet) {
                for (OsmPrimitive p : event.getPrimitives()) {
                    SimpleMatch m = primitivesRemovedMatchBySubject.remove(p);
                    if (m != null) {
                        matches.add(m);
                    }
                    if (primitivesRemovedSubjectOnly.remove(p)) {
                        subjectOnlyListModel.addElement(p);
                    }
                }
            }
        }
    }

    @Override
    public void primitivesRemoved(PrimitivesRemovedEvent event) {
        // Remove the corresponding cases from the conflation lists.
        if (settings != null) {
            DataSet dataSet = event.getDataset();
            if (dataSet == settings.referenceDataSet) {
                for (OsmPrimitive p : event.getPrimitives()) {
                    SimpleMatch m = matches.getMatchByReference(p);
                    if (m != null) {
                        primitivesRemovedMatchByReference.put(p, m);
                        matches.remove(m);
                    }
                    if (referenceOnlyListModel.removeElement(p)) {
                        primitivesRemovedReferenceOnly.add(p);
                    }
                }
            } else if (dataSet == settings.subjectDataSet) {
                for (OsmPrimitive p : event.getPrimitives()) {
                    SimpleMatch m = matches.getMatchBySubject(p);
                    if (m != null) {
                        primitivesRemovedMatchBySubject.put(p, m);
                        matches.remove(m);
                    }
                    if (subjectOnlyListModel.removeElement(p)) {
                        primitivesRemovedSubjectOnly.add(p);
                    }
                }
            }
        }
    }

    @Override
    public void tagsChanged(TagsChangedEvent event) {
    }

    @Override
    public void nodeMoved(NodeMovedEvent event) {
    }

    @Override
    public void wayNodesChanged(WayNodesChangedEvent event) {
    }

    @Override
    public void relationMembersChanged(RelationMembersChangedEvent event) {
    }

    @Override
    public void otherDatasetChange(AbstractDatasetChangedEvent event) {
    }

    @Override
    public void dataChanged(DataChangedEvent event) {
        // In case of primitive re-added because of Undo action, restore the
        // corresponding conflation lists case.
        if (settings != null) {
            DataSet dataSet = event.getDataset();
            if (dataSet == settings.referenceDataSet) {
                primitivesRemovedMatchByReference.entrySet().removeIf(entry -> {
                    if (!entry.getKey().isDeleted()) {
                        matches.add(entry.getValue());
                        return true;
                    }
                    return false;
                });
                primitivesRemovedReferenceOnly.removeIf(osmPrimitive -> {
                    if (!osmPrimitive.isDeleted()) {
                        referenceOnlyListModel.addElement(osmPrimitive);
                        return true;
                    }
                    return false;
                });
            } else if (dataSet == settings.subjectDataSet) {
                primitivesRemovedMatchBySubject.entrySet().removeIf(entry -> {
                    if (!entry.getKey().isDeleted()) {
                        matches.add(entry.getValue());
                        return true;
                    }
                    return false;
                });
                primitivesRemovedSubjectOnly.removeIf(osmPrimitive -> {
                    if (!osmPrimitive.isDeleted()) {
                        subjectOnlyListModel.addElement(osmPrimitive);
                        return true;
                    }
                    return false;
                });
            }
        }
    }

    /* ---------------------------------------------------------------------------------- */
    /* LayerChangeListener                                                                */
    /* ---------------------------------------------------------------------------------- */

    @Override
    public void layerAdded(LayerAddEvent e) {
        // Do nothing
    }

    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        Layer removedLayer = e.getRemovedLayer();
        if (settings != null) {
            boolean shouldclearReferenceSettings = removedLayer == settings.referenceLayer;
            boolean shouldclearSubjectSettings = removedLayer == settings.subjectLayer;
            if (shouldclearReferenceSettings || shouldclearSubjectSettings) {
                clear(shouldclearReferenceSettings, shouldclearSubjectSettings, true);
            }
        }
        this.settingsDialog.layerRemoving(e);
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        // Do nothing
    }

    /**
     * Launch the matching computation in a PleaseWaitRunnable window.
     */
    private void performMatching() {
        MainApplication.worker.submit(new PleaseWaitRunnable(tr("Generating matches")) {

            private SimpleMatchList computedMatches;
            private Collection<OsmPrimitive> referenceOnlyList;
            private Collection<OsmPrimitive> subjectOnlyList;
            boolean executionOk = false;

            @Override
            protected void realRun() throws SAXException, IOException, OsmTransferException {
                computedMatches = new SimpleMatchList();
                computedMatches.addAll(MatchesComputation.generateMatches(settings, getProgressMonitor()));
                if (!getProgressMonitor().isCanceled()) {
                    referenceOnlyList = settings.referenceSelection.stream().filter(
                            r -> !computedMatches.hasMatchForReference(r)).collect(Collectors.toList());
                    subjectOnlyList = settings.subjectSelection.stream().filter(
                            s -> !computedMatches.hasMatchForSubject(s)).collect(Collectors.toList());
                }
                executionOk = true;
            }

            @Override
            protected void finish() {
                if (!getProgressMonitor().isCanceled() && executionOk) {
                    setListsContentAddListnersAndLayer(computedMatches, referenceOnlyList, subjectOnlyList);
                }
            }

            @Override
            protected void cancel() {}
        });
    }

    class UnmatchedListDataListener implements ListDataListener {

        @Override
        public void intervalAdded(ListDataEvent lde) {
            updateTabTitles();
        }

        @Override
        public void intervalRemoved(ListDataEvent lde) {
            updateTabTitles();
        }

        @Override
        public void contentsChanged(ListDataEvent lde) {
            updateTabTitles();
        }
    }
}

// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation.config;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.LayoutStyle;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.KeyValueVisitor;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.data.tagging.ac.AutoCompletionPriority;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.plugins.conflation.SimpleMatchSettings;
import org.openstreetmap.josm.spi.preferences.IPreferences;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Dialog for selecting objects and configuring conflation settings
 */
public class SettingsDialog extends ExtendedDialog {

    private JButton freezeReferenceButton;
    private JButton freezeSubjectButton;
    private JButton restoreReferenceButton;
    private JButton restoreSubjectButton;
    private JLabel referenceLayerLabel;
    private JLabel subjectLayerLabel;
    private JLabel nbReferenceNodesLabel;
    private JLabel nbReferenceWaysLabel;
    private JLabel nbReferenceRelationsLabel;
    private JLabel nbSubjectNodesLabel;
    private JLabel nbSubjectWaysLabel;
    private JLabel nbSubjectRelationsLabel;
    private AutoCompletionList referenceTagsAutoCompletionList = new AutoCompletionList();
    
    private MatchingPanel matchingPanel;
    private MergingPanel mergingPanel;

    static final Font italicLabelFont = UIManager.getFont("Label.font").deriveFont(Font.ITALIC);
    static final Font plainLabelFont = UIManager.getFont("Label.font").deriveFont(Font.PLAIN);
    static final String UNSELECTED_LAYER_NAME = tr("<Please select data>");

    List<OsmPrimitive> subjectSelection = null;
    List<OsmPrimitive> referenceSelection = null;
    OsmDataLayer referenceLayer;
    DataSet subjectDataSet;
    OsmDataLayer subjectLayer;
    DataSet referenceDataSet;

    public SettingsDialog(IPreferences pref) {
        super(MainApplication.getMainFrame(),
                tr("Configure conflation settings"),
                new String[]{tr("Generate matches"), tr("Cancel")},
                false);
        setButtonIcons(new Icon[] {ImageProvider.get("ok"),
                ImageProvider.get("cancel")});
        referenceSelection = new ArrayList<>();
        subjectSelection = new ArrayList<>();
        initComponents(pref);
        restoreFromPreferences(pref);
        initListeners();
        update();
    }

    /**
     * Build GUI components
     */
    private void initComponents(IPreferences pref) {
        matchingPanel = new MatchingPanel(referenceTagsAutoCompletionList, pref, () -> this.pack());
        matchingPanel.setBorder(createLightTitleBorder(tr("Matching")));
        mergingPanel = new MergingPanel(referenceTagsAutoCompletionList, pref);
        mergingPanel.setBorder(createLightTitleBorder(tr("Merging")));
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.PAGE_AXIS));
        pnl.setAlignmentX(LEFT_ALIGNMENT);
        pnl.add(createDataLayersPanel());
        pnl.add(matchingPanel);
        pnl.add(mergingPanel);
        setContent(pnl);
        setupDialog();
    }

    private void initListeners() {
        final DataSelectionListener selectionChangeListener = event -> updateFreezeButtons(!event.getSelection().isEmpty());
        final MainLayerManager.ActiveLayerChangeListener layerChangeListener = new MainLayerManager.ActiveLayerChangeListener() {
            @Override
            public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
                updateFreezeButtons();
            }

        };
        this.addComponentListener(new ComponentAdapter() {
            private boolean listenersAdded = false;

            @Override
            public void componentHidden(ComponentEvent e) {
                if (listenersAdded) {
                    SelectionEventManager.getInstance().removeSelectionListener(selectionChangeListener);
                    MainApplication.getLayerManager().removeActiveLayerChangeListener(layerChangeListener);
                    listenersAdded = false;
                }
            }

            @Override
            public void componentShown(ComponentEvent e) {
                if (!listenersAdded) {
                    SelectionEventManager.getInstance().addSelectionListener(selectionChangeListener);
                    MainApplication.getLayerManager().addActiveLayerChangeListener(layerChangeListener);
                    listenersAdded = true;
                }
                updateFreezeButtons();
            }
        });
    }

    public JPanel createDataLayersPanel() {
        JPanel panel = new JPanel();
        //panel.setBorder(createLightTitleBorder(tr("Data")));
        JLabel referenceLabel = new JLabel(tr("Reference:"));
        JLabel subjectLabel = new JLabel(tr("Subject:"));
        JLabel layerLabel = new JLabel(tr("Layer"));
        JLabel waysLabel = new JLabel("W");
        JLabel relationsLabel = new JLabel("R");
        JLabel nodesLabel = new JLabel("N");
        restoreReferenceButton = new JButton(new RestoreReferenceAction());
        freezeReferenceButton = new JButton(new FreezeReferenceAction());
        restoreSubjectButton = new JButton(new RestoreSubjectAction());
        freezeSubjectButton = new JButton(new FreezeSubjectAction());
        nbReferenceNodesLabel = new JLabel("0");
        nbReferenceWaysLabel = new JLabel("0");
        nbReferenceRelationsLabel = new JLabel("0");
        nbSubjectNodesLabel = new JLabel("0");
        nbSubjectWaysLabel = new JLabel("0");
        nbSubjectRelationsLabel = new JLabel("0");
        referenceLayerLabel = new JLabel();
        subjectLayerLabel = new JLabel();
        referenceLayerLabel.setOpaque(true);
        subjectLayerLabel.setOpaque(true);
        JLabel empty1 = new JLabel();
        JLabel empty2 = new JLabel();
        JLabel empty3 = new JLabel();

        nodesLabel.setFont(italicLabelFont);
        waysLabel.setFont(italicLabelFont);
        relationsLabel.setFont(italicLabelFont);
        nbReferenceNodesLabel.setFont(italicLabelFont);
        nbReferenceWaysLabel.setFont(italicLabelFont);
        nbReferenceRelationsLabel.setFont(italicLabelFont);
        nbSubjectNodesLabel.setFont(italicLabelFont);
        nbSubjectWaysLabel.setFont(italicLabelFont);
        nbSubjectRelationsLabel.setFont(italicLabelFont);

        layerLabel.setBorder(new MatteBorder(0, 0, 2, 0, Color.DARK_GRAY));;

        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                   .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 5, 5)
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(empty1)
                        .addComponent(referenceLabel)
                        .addComponent(subjectLabel))
                   .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 10, 10)
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(empty2)
                        .addComponent(freezeReferenceButton)
                        .addComponent(freezeSubjectButton))
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(empty3)
                        .addComponent(restoreReferenceButton)
                        .addComponent(restoreSubjectButton))
                   .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 10, 10)
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(layerLabel)
                        .addComponent(referenceLayerLabel, 200, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
                        .addComponent(subjectLayerLabel, 200, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE))
                   .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 30, 30)
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(nodesLabel)
                        .addComponent(nbReferenceNodesLabel)
                        .addComponent(nbSubjectNodesLabel))
                   .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 5, 5)
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(waysLabel)
                        .addComponent(nbReferenceWaysLabel)
                        .addComponent(nbSubjectWaysLabel))
                   .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 5, 5)
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(relationsLabel)
                        .addComponent(nbReferenceRelationsLabel)
                        .addComponent(nbSubjectRelationsLabel))
                   .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 5, Short.MAX_VALUE)
             );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(empty1)
                        .addComponent(empty2)
                        .addComponent(empty3)
                        .addComponent(layerLabel)
                        .addComponent(nodesLabel)
                        .addComponent(waysLabel)
                        .addComponent(relationsLabel))
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(referenceLabel)
                        .addComponent(freezeReferenceButton)
                        .addComponent(restoreReferenceButton)
                        .addComponent(referenceLayerLabel)
                        .addComponent(nbReferenceNodesLabel)
                        .addComponent(nbReferenceWaysLabel)
                        .addComponent(nbReferenceRelationsLabel))
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(subjectLabel)
                        .addComponent(freezeSubjectButton)
                        .addComponent(restoreSubjectButton)
                        .addComponent(subjectLayerLabel)
                        .addComponent(nbSubjectNodesLabel)
                        .addComponent(nbSubjectWaysLabel)
                        .addComponent(nbSubjectRelationsLabel))
                   .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 10, 10)
        );
        return panel;
    }

    private Border createLightTitleBorder(String title) {
        TitledBorder tileBorder = BorderFactory.createTitledBorder(title);
        tileBorder.setTitleFont(italicLabelFont);
        Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        return new CompoundBorder(tileBorder, emptyBorder);
    }


    public boolean checkValidityOrNotifyProblems() {
        if (referenceSelection.isEmpty() || subjectSelection.isEmpty()) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                    tr("Selections must be made for both reference and subject."), tr("Incomplete selections"),
                    JOptionPane.ERROR_MESSAGE);
            return false;
        } else {
            return true;
        }
    }

    /**
     * @return the settings
     */
    public SimpleMatchSettings getSettings() {
        SimpleMatchSettings settings = new SimpleMatchSettings();
        settings.referenceDataSet = referenceDataSet;
        settings.referenceLayer = referenceLayer;
        settings.referenceSelection = referenceSelection;
        settings.subjectDataSet = subjectDataSet;
        settings.subjectLayer = subjectLayer;
        settings.subjectSelection = subjectSelection;
        matchingPanel.fillSettings(settings);
        mergingPanel.fillSettings(settings);
        return settings;
    }

    public void savePreferences(IPreferences pref) {
        matchingPanel.savePreferences(pref);
        mergingPanel.savePreferences(pref);
    }

    public void restoreFromPreferences(IPreferences pref) {
        matchingPanel.restoreFromPreferences(pref);
        mergingPanel.restoreFromPreferences(pref);
    }

    class RestoreSubjectAction extends JosmAction {

        RestoreSubjectAction() {
            super(tr("Restore"), null, tr("Restore subject selection"), null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (subjectLayer != null && subjectDataSet != null && subjectSelection != null && !subjectSelection.isEmpty()) {
                MainApplication.getLayerManager().setActiveLayer(subjectLayer);
                subjectLayer.setVisible(true);
                subjectDataSet.setSelected(subjectSelection);
            }
        }
    }

    class RestoreReferenceAction extends JosmAction {

        RestoreReferenceAction() {
            super(tr("Restore"), null, tr("Restore reference selection"), null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (referenceLayer != null && referenceDataSet != null && referenceSelection != null && !referenceSelection.isEmpty()) {
                MainApplication.getLayerManager().setActiveLayer(referenceLayer);
                referenceLayer.setVisible(true);
                referenceDataSet.setSelected(referenceSelection);
            }
        }
    }

    class FreezeSubjectAction extends JosmAction {

        FreezeSubjectAction() {
            super(tr("Freeze"), null, tr("Freeze subject selection"), null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            subjectDataSet = MainApplication.getLayerManager().getEditDataSet();
            subjectLayer = MainApplication.getLayerManager().getEditLayer();
            if (subjectDataSet == null || subjectLayer == null) {
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                    tr("No valid OSM data layer present."), tr("Error freezing selection"),
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            subjectSelection.clear();
            subjectSelection.addAll(subjectDataSet.getSelected());
            if (subjectSelection.isEmpty()) {
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                        tr("Nothing is selected, please try again."), tr("Empty selection"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            update();
        }
    }

    class FreezeReferenceAction extends JosmAction {

        FreezeReferenceAction() {
            super(tr("Freeze"), null, tr("Freeze reference selection"), null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            referenceDataSet = MainApplication.getLayerManager().getEditDataSet();
            referenceLayer = MainApplication.getLayerManager().getEditLayer();
            if (referenceDataSet == null || referenceLayer == null) {
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                        tr("No valid OSM data layer present."), tr("Error freezing selection"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            referenceSelection.clear();
            referenceSelection.addAll(referenceDataSet.getSelected());
            if (referenceSelection.isEmpty()) {
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                        tr("Nothing is selected, please try again."), tr("Empty selection"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            update();
        }
    }

    /**
     * Update GUI elements
     */
    void update() {
        int numNodes = 0;
        int numWays = 0;
        int numRelations = 0;
        int totalRelations = 0;

        // if subject and reference sets are the same, hint user that this must be wrong
        if (subjectLayer != null && subjectLayer == referenceLayer && !subjectSelection.isEmpty()) {
            boolean identicalSet = (subjectSelection.size() == referenceSelection.size()
                    && new HashSet<>(subjectSelection).containsAll(referenceSelection));
            if (identicalSet) {
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                        tr("Reference and subject sets should better be different."), tr("Warning"),
                        JOptionPane.WARNING_MESSAGE);
            }
        }

        if (!subjectSelection.isEmpty()) {
            for (OsmPrimitive p : subjectSelection) {
                if (p instanceof Node) {
                    numNodes++;
                } else if (p instanceof Way) {
                    numWays++;
                } else if (p instanceof Relation) {
                    numRelations++;
                }
            }
            subjectLayerLabel.setText(subjectLayer.getName());
            subjectLayerLabel.setOpaque(false);

            nbSubjectNodesLabel.setText("" + numNodes);
            nbSubjectWaysLabel.setText("" + numWays);
            nbSubjectRelationsLabel.setText("" + numRelations);
            restoreSubjectButton.setEnabled(true);
        } else {
            subjectLayerLabel.setText(UNSELECTED_LAYER_NAME);
            subjectLayerLabel.setBackground(Color.YELLOW);
            subjectLayerLabel.setOpaque(true);
            nbSubjectNodesLabel.setText("0");
            nbSubjectWaysLabel.setText("0");
            nbSubjectRelationsLabel.setText("0");
            restoreSubjectButton.setEnabled(false);
        }
        totalRelations += numRelations;
        numNodes = 0;
        numWays = 0;
        numRelations = 0;
        if (!referenceSelection.isEmpty()) {
            HashSet<String> referenceKeys = new HashSet<>();
            KeyValueVisitor referenceKeysVisitor = (primitive, key, value) -> referenceKeys.add(key);
            for (OsmPrimitive p : referenceSelection) {
                if (p instanceof Node) {
                    numNodes++;
                } else if (p instanceof Way) {
                    numWays++;
                } else if (p instanceof Relation) {
                    numRelations++;
                }
                p.visitKeys(referenceKeysVisitor);
            }
            referenceKeys.removeAll(OsmPrimitive.getDiscardableKeys());
            referenceTagsAutoCompletionList.clear();
            referenceTagsAutoCompletionList.add(referenceKeys, AutoCompletionPriority.IS_IN_DATASET);
            referenceLayerLabel.setText(referenceLayer.getName());
            referenceLayerLabel.setOpaque(false);
            nbReferenceNodesLabel.setText("" + numNodes);
            nbReferenceWaysLabel.setText("" + numWays);
            nbReferenceRelationsLabel.setText("" + numRelations);
            restoreReferenceButton.setEnabled(true);
        } else {
            referenceTagsAutoCompletionList.clear();
            referenceLayerLabel.setText(UNSELECTED_LAYER_NAME);
            referenceLayerLabel.setBackground(Color.YELLOW);
            referenceLayerLabel.setOpaque(true);
            nbReferenceNodesLabel.setText("0");
            nbReferenceWaysLabel.setText("0");
            nbReferenceRelationsLabel.setText("0");
            restoreReferenceButton.setEnabled(false);
        }
        totalRelations += numRelations;
        if (totalRelations != 0) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                    tr("Relations are not supported yet, please do not select them."), tr("Error"),
                    JOptionPane.ERROR_MESSAGE);
        }
        updateFreezeButtons();
        this.pack();
    }

    public void updateFreezeButtons() {
        DataSet dataSet = MainApplication.getLayerManager().getEditDataSet();
        updateFreezeButtons((dataSet == null) ? false : !dataSet.getSelected().isEmpty());
    }

    public void updateFreezeButtons(boolean enabled) {
        freezeReferenceButton.setEnabled(enabled);
        freezeSubjectButton.setEnabled(enabled);
    }

    /**
     * To be called when a layer is removed.
     * Clear any reference to the removed layer.
     * @param e the layer remove event.
     */
    public void layerRemoving(LayerRemoveEvent e) {
        Layer removedLayer = e.getRemovedLayer();
        this.clear(removedLayer == referenceLayer, removedLayer == subjectLayer);
    }

    /**
     * Clear some settings.
     * @param shouldClearReference if "Reference" settings should be cleared.
     * @param shouldClearSubject if "Subject" settings should be cleared.
     */
    public void clear(boolean shouldClearReference, boolean shouldClearSubject) {
        if (shouldClearReference || shouldClearSubject) {
            if (shouldClearReference) {
                referenceLayer = null;
                referenceDataSet = null;
                referenceSelection.clear();
            }
            if (shouldClearSubject) {
                subjectLayer = null;
                subjectDataSet = null;
                subjectSelection.clear();
            }
            update();
        }
    }

}

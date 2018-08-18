// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.config;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.plugins.conflation.SimpleMatchSettings;
import org.openstreetmap.josm.spi.preferences.IPreferences;

/**
 * Matchingq panel, that allow to select between {@link SimpleMatchFinderPanel},
 * {@link AdvancedMatchFinderPanel} or {@link  ProgrammingMatchFinderPanel}.
 */
public class MatchingPanel extends JPanel {
    
    private SimpleMatchFinderPanel simpleMatchFinderPanel;
    private AdvancedMatchFinderPanel advancedMatchFinderPanel;
    private ProgrammingMatchFinderPanel programmingMatchFinderPanel;
    private Box selectedMatchFinderBox;
    private AutoCompletionList referenceTagsAutoCompletionList;
    private Runnable pack;
    
    public MatchingPanel(AutoCompletionList referenceKeysAutocompletionList, IPreferences pref, Runnable pack) {
        this.referenceTagsAutoCompletionList = referenceKeysAutocompletionList;
        this.pack = pack;
        this.initComponents(pref);
    }
    
    private void initComponents(IPreferences pref) {
        this.setLayout(new BorderLayout());
        simpleMatchFinderPanel = new SimpleMatchFinderPanel(referenceTagsAutoCompletionList, pref);
        advancedMatchFinderPanel = new AdvancedMatchFinderPanel(referenceTagsAutoCompletionList, pref);
        if (ExpertToggleAction.isExpert()) {
            programmingMatchFinderPanel = new ProgrammingMatchFinderPanel(pref);
        }

        JRadioButton simpleRadioButton = new JRadioButton(tr("Simple"));
        JRadioButton advancedRadioButton = new JRadioButton(tr("Advanced"));
        JRadioButton programmimgRadioButton = new JRadioButton(tr("Programming"));
        simpleRadioButton.setFont(SettingsDialog.plainLabelFont);
        advancedRadioButton.setFont(SettingsDialog.plainLabelFont);
        programmimgRadioButton.setFont(SettingsDialog.plainLabelFont);
        ButtonGroup complexitySelectionBGroup = new ButtonGroup();
        complexitySelectionBGroup.add(simpleRadioButton);
        complexitySelectionBGroup.add(advancedRadioButton);
        complexitySelectionBGroup.add(programmimgRadioButton);
        simpleRadioButton.setSelected(true);

        Box complexitySelectionBox = Box.createHorizontalBox();
        complexitySelectionBox.setBorder(BorderFactory.createLoweredBevelBorder());
        complexitySelectionBox.add(simpleRadioButton);
        complexitySelectionBox.add(advancedRadioButton);
        if (programmingMatchFinderPanel != null) {
            complexitySelectionBox.add(programmimgRadioButton);
        }

        selectedMatchFinderBox = Box.createHorizontalBox();
        selectedMatchFinderBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        selectedMatchFinderBox.add(simpleMatchFinderPanel);
        ActionListener modeChangedLiseter = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Component componentToSelect = null;
                if (event.getSource() == simpleRadioButton) {
                    componentToSelect = simpleMatchFinderPanel;
                } else if (event.getSource() == advancedRadioButton) {
                    componentToSelect = advancedMatchFinderPanel;
                } else if (event.getSource() == programmimgRadioButton) {
                    componentToSelect = programmingMatchFinderPanel;
                }
                Component currentComponent = (selectedMatchFinderBox.getComponentCount() > 0) ? selectedMatchFinderBox.getComponent(0) : null;
                if ((componentToSelect != currentComponent) && (componentToSelect != null)) {
                    selectedMatchFinderBox.remove(0);
                    selectedMatchFinderBox.add(componentToSelect);
                    selectedMatchFinderBox.revalidate();
                    MatchingPanel.this.pack.run();
                    selectedMatchFinderBox.repaint();
                }
            }
        };
        simpleRadioButton.addActionListener(modeChangedLiseter);
        advancedRadioButton.addActionListener(modeChangedLiseter);
        programmimgRadioButton.addActionListener(modeChangedLiseter);

        Box box = Box.createVerticalBox();
        box.add(complexitySelectionBox);
        box.add(Box.createRigidArea(new Dimension(1, 5)));
        box.add(selectedMatchFinderBox);
        this.add(box, BorderLayout.CENTER);
    }

    MatchFinderPanel getSelectedMatchFinderPanel() {
        return (MatchFinderPanel) selectedMatchFinderBox.getComponent(0);
    }    

    public void savePreferences(IPreferences pref) {
        simpleMatchFinderPanel.savePreferences(pref);
        advancedMatchFinderPanel.savePreferences(pref);
        if (programmingMatchFinderPanel != null) {
            programmingMatchFinderPanel.savePreferences(pref);
        }
    }
    
    public void restoreFromPreferences(IPreferences pref) {
        simpleMatchFinderPanel.restoreFromPreferences(pref);
        advancedMatchFinderPanel.restoreFromPreferences(pref);
        if (programmingMatchFinderPanel != null) {
            programmingMatchFinderPanel.restoreFromPreferences(pref);
        }
    }
    
    public void fillSettings(SimpleMatchSettings settings) {
        settings.matchFinder = getSelectedMatchFinderPanel().getMatchFinder();
    }
    
}

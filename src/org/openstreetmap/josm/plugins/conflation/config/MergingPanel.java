// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.config;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutStyle;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.plugins.conflation.SimpleMatchSettings;

/**
 * Panel to configure merging options (replace geometry, merge tags list).
 */
public class MergingPanel extends JPanel {

    private JCheckBox replaceGeometryCheckBox;
    private JCheckBox mergeTagsCheckBox;
    private JCheckBox mergeAllCheckBox;
    private DefaultPromptTextField mergeTagsField;
    private DefaultPromptTextField mergeTagsExceptField;
    private JLabel mergeTagsExceptLabel;
    private JCheckBox overwriteTagsCheckbox; // may be null
    private DefaultPromptTextField overwriteTagsField; // may be null
    private JLabel forceKeepTagsLabel; // may be null
    private DefaultPromptTextField forceKeepTagsField; // may be null
    private AutoCompletionList referenceTagsAutoCompletionList;

    public MergingPanel(AutoCompletionList referenceKeysAutocompletionList, Preferences pref) {
        this.referenceTagsAutoCompletionList = referenceKeysAutocompletionList;
        this.initComponents();
    }

    private void initComponents() {
        replaceGeometryCheckBox = new JCheckBox(tr("Replace Geometry"));
        mergeTagsCheckBox = new JCheckBox(tr("Merge Tags"));
        mergeAllCheckBox = new JCheckBox(tr("All"));
        mergeTagsField = new DefaultPromptTextField(20, tr("all"));
        mergeTagsField.setToolTipText(tr("List of tags to merge"));
        mergeTagsField.setAutoCompletionList(referenceTagsAutoCompletionList);
        mergeTagsExceptLabel = new JLabel(tr("except"));
        mergeTagsExceptField = new DefaultPromptTextField(20, tr("none"));
        mergeTagsExceptField.setToolTipText(tr("List of tags to NOT merge (they will be ignored)"));
        mergeTagsExceptField.setAutoCompletionList(referenceTagsAutoCompletionList);
        if (ExpertToggleAction.isExpert()) {
            overwriteTagsCheckbox = new JCheckBox(tr("Overwrite tags without confirmation"));
            overwriteTagsField = new DefaultPromptTextField(20, tr("none"));
            overwriteTagsField.setToolTipText(tr("List of tags to overwrite on conflict using reference layer without confirmation"));
            overwriteTagsField.setAutoCompletionList(referenceTagsAutoCompletionList);
            forceKeepTagsLabel = new JLabel(tr("and keep"));
            forceKeepTagsField = new DefaultPromptTextField(20, tr("none"));
            forceKeepTagsField.setToolTipText(tr("List of tags to keep on conflict from subject layer without confirmation"));
            forceKeepTagsField.setAutoCompletionList(referenceTagsAutoCompletionList);
            overwriteTagsCheckbox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean enable = overwriteTagsCheckbox.isSelected();
                    overwriteTagsField.setEnabled(enable);
                    forceKeepTagsField.setEnabled(enable);
                    forceKeepTagsLabel.setEnabled(enable);
                }
            });
        }
        mergeTagsCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean enable = mergeTagsCheckBox.isSelected();
                mergeAllCheckBox.setEnabled(enable);
                mergeTagsField.setEnabled(enable);
                mergeTagsExceptLabel.setEnabled(enable);
                mergeTagsExceptField.setEnabled(enable);
                if (enable) {
                    mergeTagsField.setText("");
                    mergeTagsExceptField.setText("");
                    mergeAllCheckBox.setSelected(true);
                }
            } });
        mergeAllCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mergeAllCheckBox.isSelected()) {
                    mergeTagsField.setText("");
                    mergeTagsExceptField.setText("");
                }
            }
        });
        DocumentListener documentListener = new DocumentListener() {
            private void checkMergeAllCheckBox() {
                boolean noTags = SimpleMatchFinderPanel.splitBySpaceComaOrSemicolon(mergeTagsField.getText()).isEmpty();
                boolean noExceptTags = SimpleMatchFinderPanel.splitBySpaceComaOrSemicolon(mergeTagsExceptField.getText()).isEmpty();
                mergeAllCheckBox.setSelected(noTags && noExceptTags);
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                checkMergeAllCheckBox();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                checkMergeAllCheckBox();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                checkMergeAllCheckBox();
            }
        };

        mergeTagsField.getDocument().addDocumentListener(documentListener);
        mergeTagsExceptField.getDocument().addDocumentListener(documentListener);

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        ParallelGroup horizonatGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(replaceGeometryCheckBox)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(mergeTagsCheckBox)
                        .addComponent(mergeAllCheckBox)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 3, 3)
                        .addComponent(mergeTagsField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 2, 2)
                        .addComponent(mergeTagsExceptLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 2, 2)
                        .addComponent(mergeTagsExceptField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 0, Short.MAX_VALUE)
        );
        SequentialGroup verticalGroup = layout.createSequentialGroup()
                .addComponent(replaceGeometryCheckBox)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(mergeTagsCheckBox)
                    .addComponent(mergeAllCheckBox)
                    .addComponent(mergeTagsField)
                    .addComponent(mergeTagsExceptLabel)
                    .addComponent(mergeTagsExceptField));
        if (ExpertToggleAction.isExpert()) {
            horizonatGroup.addGroup(layout.createSequentialGroup()
                    .addComponent(overwriteTagsCheckbox)
                    .addComponent(overwriteTagsField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 5, Short.MAX_VALUE)
                    .addComponent(forceKeepTagsLabel)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 5, Short.MAX_VALUE)
                    .addComponent(forceKeepTagsField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 5, Short.MAX_VALUE));
            verticalGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(overwriteTagsCheckbox)
                    .addComponent(overwriteTagsField)
                    .addComponent(forceKeepTagsLabel)
                    .addComponent(forceKeepTagsField));
        }
        layout.setHorizontalGroup(horizonatGroup);
        layout.setVerticalGroup(verticalGroup);
    }

    public void savePreferences(Preferences pref) {
        pref.put(getClass().getName() + ".replaceGeometryCheckBox", replaceGeometryCheckBox.isSelected());
        pref.put(getClass().getName() + ".mergeTagsCheckBox", mergeTagsCheckBox.isSelected());
        pref.put(getClass().getName() + ".mergeAllCheckBox", mergeAllCheckBox.isSelected());
        pref.put(getClass().getName() + ".mergeTagsField", mergeTagsField.getText());
        pref.put(getClass().getName() + ".mergeTagsExceptField", mergeTagsExceptField.getText());
        if (overwriteTagsCheckbox != null) {
            pref.put(getClass().getName() + ".overwriteTagsCheckbox", overwriteTagsCheckbox.isSelected());
            pref.put(getClass().getName() + ".overwriteTagsField", overwriteTagsField.getText());
            pref.put(getClass().getName() + ".forceKeepTagsField", forceKeepTagsField.getText());
        }
    }

    public void restoreFromPreferences(Preferences pref) {
        replaceGeometryCheckBox.setSelected(pref.getBoolean(getClass().getName() + ".replaceGeometryCheckBox", true));
        mergeTagsField.setText(pref.get(getClass().getName() + ".mergeTagsField", ""));
        mergeTagsExceptField.setText(pref.get(getClass().getName() + ".mergeTagsExceptField", ""));
        mergeAllCheckBox.setSelected(pref.getBoolean(getClass().getName() + ".mergeAllCheckBox", true));
        mergeTagsCheckBox.setSelected(pref.getBoolean(getClass().getName() + ".mergeTagsCheckBox", true));
        if (overwriteTagsCheckbox != null) {
            overwriteTagsField.setText(pref.get(getClass().getName() + ".overwriteTagsField", ""));
            forceKeepTagsField.setText(pref.get(getClass().getName() + ".forceKeepTagsField", ""));
            overwriteTagsCheckbox.setSelected(pref.getBoolean(getClass().getName() + ".overwriteTagsCheckbox", false));
        }
    }

    public void fillSettings(SimpleMatchSettings settings) {
        settings.isReplacingGeometry = replaceGeometryCheckBox.isSelected();
        if (mergeTagsCheckBox.isSelected()) {
            List<String> tagsList = SimpleMatchFinderPanel.splitBySpaceComaOrSemicolon(mergeTagsField.getText());
            List<String> tagsExceptList = SimpleMatchFinderPanel.splitBySpaceComaOrSemicolon(mergeTagsExceptField.getText());
            if (!tagsList.isEmpty()) {
                settings.mergeTags = tagsList;
            } else {
                settings.mergeTags = new SimpleMatchSettings.All<>();
            }
            if (!tagsExceptList.isEmpty()) {
                settings.mergeTags.removeAll(tagsExceptList);
            }
        } else {
            settings.mergeTags = new ArrayList<>(0);
        }

        settings.overwriteTags = new ArrayList<>(0);
        settings.forceKeepTags = new ArrayList<>(0);
        if ((overwriteTagsCheckbox != null) && overwriteTagsCheckbox.isSelected()) {
            if (overwriteTagsField != null)
                settings.overwriteTags = SimpleMatchFinderPanel.splitBySpaceComaOrSemicolon(overwriteTagsField.getText());
            if (forceKeepTagsField != null)
                settings.forceKeepTags = SimpleMatchFinderPanel.splitBySpaceComaOrSemicolon(forceKeepTagsField.getText());
        }
    }
}

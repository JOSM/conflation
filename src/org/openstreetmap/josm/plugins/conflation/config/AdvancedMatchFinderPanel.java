// License: GPL. For details, see LICENSE file.
// Based on the original code from Vidid Solutions
// com.vividsolutions.jcs.plugin.conflate.polygonmatch.ToolboxPanel
package org.openstreetmap.josm.plugins.conflation.config;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
//import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.plugins.conflation.matcher.AttributeMatcher;
import org.openstreetmap.josm.plugins.conflation.matcher.ExactValueMatcher;
import org.openstreetmap.josm.plugins.conflation.matcher.LevenshteinDistanceValueMatcher;
import org.openstreetmap.josm.plugins.conflation.matcher.OsmNormalizeRule;
import org.openstreetmap.josm.plugins.conflation.matcher.StandardDistanceMatcher;

import com.vividsolutions.jcs.conflate.polygonmatch.AngleHistogramMatcher;
import com.vividsolutions.jcs.conflate.polygonmatch.AreaFilterFCMatchFinder;
import com.vividsolutions.jcs.conflate.polygonmatch.BasicFCMatchFinder;
import com.vividsolutions.jcs.conflate.polygonmatch.CentroidAligner;
import com.vividsolutions.jcs.conflate.polygonmatch.CentroidDistanceMatcher;
import com.vividsolutions.jcs.conflate.polygonmatch.ChainMatcher;
import com.vividsolutions.jcs.conflate.polygonmatch.CompactnessMatcher;
import com.vividsolutions.jcs.conflate.polygonmatch.DisambiguatingFCMatchFinder;
import com.vividsolutions.jcs.conflate.polygonmatch.FCMatchFinder;
import com.vividsolutions.jcs.conflate.polygonmatch.FeatureMatcher;
import com.vividsolutions.jcs.conflate.polygonmatch.HausdorffDistanceMatcher;
import com.vividsolutions.jcs.conflate.polygonmatch.IdenticalFeatureFilter;
import com.vividsolutions.jcs.conflate.polygonmatch.SymDiffMatcher;
import com.vividsolutions.jcs.conflate.polygonmatch.TargetUnioningFCMatchFinder;
import com.vividsolutions.jcs.conflate.polygonmatch.WeightedMatcher;
import com.vividsolutions.jcs.conflate.polygonmatch.WindowFilter;
import com.vividsolutions.jcs.plugin.conflate.polygonmatch.MyValidatingTextField;


/**
 * This advanced panel is a modified version of the original code
 * from Vivd Solutions for the JUMP JCS Plugin ToolboxPanel.java.
 *
 *  See documentation in the sesction 7 page 26:
 *  https://github.com/joshdoe/jcs/blob/master/doc/JCS%20User%20Guide.pdf
 */
public class AdvancedMatchFinderPanel extends MatchFinderPanel {
    private JTabbedPane upperTabbedPane = new JTabbedPane();
    private JPanel filteringTab = new JPanel();
    private JPanel unioningTab = new JPanel();
    private JPanel matchingTab = new JPanel();
    private GridBagLayout gridBagLayout3 = new GridBagLayout();
    private GridBagLayout gridBagLayout4 = new GridBagLayout();
    //private MatchEngine engine = new MatchEngine();
    private GridBagLayout gridBagLayout5 = new GridBagLayout();
    private JPanel filterByAreaPanel = new JPanel();
    private GridBagLayout gridBagLayout6 = new GridBagLayout();
    private JCheckBox filterByAreaCheckBox = new JCheckBox("", true);
    private MyValidatingTextField filterByAreaMinField = new MyValidatingTextField("0",
            4, MyValidatingTextField.NON_NEGATIVE_DOUBLE_VALIDATOR, "0");
    private MyValidatingTextField filterByAreaMaxField = new MyValidatingTextField("9E6",
            4, MyValidatingTextField.NON_NEGATIVE_DOUBLE_VALIDATOR, "0");
    private JLabel filterByAreaLabel2 = new JLabel();
    private JLabel filterByAreaLabel1 = new JLabel();
    private JPanel filterByWindowPanel = new JPanel();
    private GridBagLayout gridBagLayout7 = new GridBagLayout();
    private JCheckBox filterByWindowCheckBox = new JCheckBox("", true);
    private JLabel filterByWindowLabel = new JLabel();
    private MyValidatingTextField filterByWindowField = new MyValidatingTextField("50",
            4, MyValidatingTextField.NON_NEGATIVE_DOUBLE_VALIDATOR, "0");
    private JCheckBox unionCheckBox = new JCheckBox("", false);
    private JLabel unionLabel1 = new JLabel();
    private MyValidatingTextField unionTextField = new MyValidatingTextField("2",
            1,
            new MyValidatingTextField.CompositeValidator(new MyValidatingTextField.Validator[] {
                    MyValidatingTextField.NON_NEGATIVE_INTEGER_VALIDATOR,
                    new MyValidatingTextField.GreaterThanValidator(1.5)
                }), "2");
    private JLabel unionLabel2 = new JLabel();
    private MyValidatingTextField angleBinField = new MyValidatingTextField("18",
            3,
            new MyValidatingTextField.CompositeValidator(new MyValidatingTextField.Validator[] {
                    MyValidatingTextField.NON_NEGATIVE_INTEGER_VALIDATOR,
                    new MyValidatingTextField.GreaterThanValidator(1.5)
                }), "2");
    private JLabel weightLabel = new JLabel();
    private JCheckBox stdDistanceCheckBox = new JCheckBox("", true);
    private MyValidatingTextField stdDistanceWeightField = new MyValidatingTextField("10",
            3, MyValidatingTextField.NON_NEGATIVE_DOUBLE_VALIDATOR, "0");
    private JPanel stdDistancePanel = new JPanel();
    private GridBagLayout stdDistanceLayout = new GridBagLayout();
    private JLabel stdDistanceLabel = new JLabel();
    private JLabel stdDistanceBelow = new JLabel();
    private MyValidatingTextField stdDistanceThresholdField = new MyValidatingTextField("50",
            4, MyValidatingTextField.NON_NEGATIVE_DOUBLE_VALIDATOR, "");
    private JCheckBox centroidCheckBox = new JCheckBox("", true);
    private MyValidatingTextField centroidDistanceWeightField = new MyValidatingTextField("10",
            3, MyValidatingTextField.NON_NEGATIVE_DOUBLE_VALIDATOR, "0");
    private JPanel centroidPanel = new JPanel();
    private GridBagLayout centroidLayout = new GridBagLayout();
    private JLabel centroidLabel = new JLabel();
    private JLabel centroidBelow = new JLabel();
    private MyValidatingTextField centroidThresholdField = new MyValidatingTextField("50",
            4, MyValidatingTextField.NON_NEGATIVE_DOUBLE_VALIDATOR, "");
    private JCheckBox hausdorffCheckBox = new JCheckBox("", true);
    private MyValidatingTextField hausdorffDistanceWeightField = new MyValidatingTextField("10",
            3, MyValidatingTextField.NON_NEGATIVE_DOUBLE_VALIDATOR, "0");
    private JLabel hausdorffLabel = new JLabel();
    private JCheckBox symDiffCheckBox = new JCheckBox("", true);
    private MyValidatingTextField symDiffWeightField = new MyValidatingTextField("10",
            3, MyValidatingTextField.NON_NEGATIVE_DOUBLE_VALIDATOR, "0");
    private JLabel symDiffLabel = new JLabel();
    private JCheckBox symDiffCentroidsAlignedCheckBox = new JCheckBox("", true);
    private MyValidatingTextField symDiffCentroidsAlignedWeightField = new MyValidatingTextField("10",
            3, MyValidatingTextField.NON_NEGATIVE_DOUBLE_VALIDATOR, "0");
    private JLabel symDiffCentroidsAlignedLabel = new JLabel();
    private JCheckBox compactnessCheckBox = new JCheckBox("", true);
    private MyValidatingTextField compactnessWeightField = new MyValidatingTextField("10",
            3, MyValidatingTextField.NON_NEGATIVE_DOUBLE_VALIDATOR, "0");
    private JLabel compactnessLabel = new JLabel();
    private JCheckBox angleCheckBox = new JCheckBox("", true);
    private MyValidatingTextField angleWeightField = new MyValidatingTextField("10",
            3, MyValidatingTextField.NON_NEGATIVE_DOUBLE_VALIDATOR, "0");
    private JPanel anglePanel = new JPanel();
    private GridBagLayout gridBagLayout8 = new GridBagLayout();
    private JLabel angleLabel = new JLabel();
    private JPanel matchingFillerPanel = new JPanel();
    private JTextArea filterByAreaTextArea = new JTextArea();
    private JTextArea unioningTextArea = new JTextArea();
    private GridBagLayout exactTagsdLayout = new GridBagLayout();
    private GridBagLayout levenshteinTagsLayout = new GridBagLayout();
    private JPanel exactTagsPanel = new JPanel();
    private JPanel levenshteinTagsPanel = new JPanel();
    private JCheckBox levenshteinTagsCheckBox = new JCheckBox("", true);
    private MyValidatingTextField levenshteinTagsWeightField = new MyValidatingTextField("50",
            3, MyValidatingTextField.NON_NEGATIVE_DOUBLE_VALIDATOR, "0");
    private JLabel exactTagsLabel = new JLabel();
    private JLabel levenshteinTagsLabel = new JLabel();
    private DefaultPromptTextField exactTagsField = new DefaultPromptTextField(15, tr("none"));
    private DefaultPromptTextField levenshteinTagsField = new DefaultPromptTextField(15, tr("none"));
    private JCheckBox identicalCheckBox = new JCheckBox("", true);

    public AdvancedMatchFinderPanel(AutoCompletionList referenceKeysAutocompletionList, Preferences pref) {
        super();
        filterByAreaTextArea.setFont(angleLabel.getFont().deriveFont(Font.ITALIC));
        unioningTextArea.setFont(angleLabel.getFont().deriveFont(Font.ITALIC));
        handleLabelClicks();
        jbInit();
        exactTagsField.setAutoCompletionList(referenceKeysAutocompletionList);
        levenshteinTagsField.setAutoCompletionList(referenceKeysAutocompletionList);
        restoreFromPreferences(pref);
    }

    public MyValidatingTextField getAngleBinField() {
        return angleBinField;
    }

    public JCheckBox getAngleCheckBox() {
        return angleCheckBox;
    }

    public MyValidatingTextField getAngleWeightField() {
        return angleWeightField;
    }

    public JCheckBox getCentroidCheckBox() {
        return centroidCheckBox;
    }

    public MyValidatingTextField getCentroidDistanceWeightField() {
        return centroidDistanceWeightField;
    }

    public JCheckBox getCompactnessCheckBox() {
        return compactnessCheckBox;
    }

    public MyValidatingTextField getCompactnessWeightField() {
        return compactnessWeightField;
    }

    public JCheckBox getHausdorffCheckBox() {
        return hausdorffCheckBox;
    }

    public MyValidatingTextField getHausdorffDistanceWeightField() {
        return hausdorffDistanceWeightField;
    }

    public JCheckBox getSymDiffCheckBox() {
        return symDiffCheckBox;
    }

    public MyValidatingTextField getSymDiffWeightField() {
        return symDiffWeightField;
    }

    public JCheckBox getSymDiffCentroidsAlignedCheckBox() {
        return symDiffCentroidsAlignedCheckBox;
    }

    public MyValidatingTextField getSymDiffCentroidsAlignedWeightField() {
        return symDiffCentroidsAlignedWeightField;
    }

    public void setFilterByAreaMaxField(
        MyValidatingTextField filterByAreaMaxField) {
        this.filterByAreaMaxField = filterByAreaMaxField;
    }

    public void setFilterByAreaMinField(
        MyValidatingTextField filterByAreaMinField) {
        this.filterByAreaMinField = filterByAreaMinField;
    }

    public void setFilterByWindowCheckBox(JCheckBox filterByWindowCheckBox) {
        this.filterByWindowCheckBox = filterByWindowCheckBox;
    }

    public void setUnionTextField(MyValidatingTextField unionTextField) {
        this.unionTextField = unionTextField;
    }

    public JCheckBox getFilterByAreaCheckBox() {
        return filterByAreaCheckBox;
    }

    public MyValidatingTextField getFilterByAreaMaxField() {
        return filterByAreaMaxField;
    }

    public MyValidatingTextField getFilterByAreaMinField() {
        return filterByAreaMinField;
    }

    public JCheckBox getFilterByWindowCheckBox() {
        return filterByWindowCheckBox;
    }

    public MyValidatingTextField getFilterByWindowField() {
        return filterByWindowField;
    }

    public JCheckBox getUnionCheckBox() {
        return unionCheckBox;
    }

    public MyValidatingTextField getUnionTextField() {
        return unionTextField;
    }

    private void handleLabelClicks() {
        handleClicks(filterByWindowLabel, filterByWindowCheckBox);
        handleClicks(filterByAreaLabel1, filterByAreaCheckBox);
        handleClicks(unionLabel1, unionCheckBox);
    }

    private void handleClicks(JLabel label, final JCheckBox checkBox) {
        label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    checkBox.doClick();
                }
            });
    }

    private void jbInit() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        filteringTab.setLayout(gridBagLayout3);
        unioningTab.setLayout(gridBagLayout4);
        matchingTab.setLayout(gridBagLayout5);
        filterByAreaPanel.setLayout(gridBagLayout6);
        filterByAreaLabel2.setText(tr(" Max: "));
        filterByAreaLabel1.setText(tr("Filter by area. Min: "));
        filterByWindowPanel.setLayout(gridBagLayout7);
        filterByWindowLabel.setText(tr("Filter by window. Buffer: "));
        unionLabel1.setText(tr("Union up to "));
        unionLabel2.setText(tr(" adjacent Reference features"));
        weightLabel.setText(tr("Weight"));
        stdDistanceLabel.setText(tr("Standard Distance"));
        stdDistancePanel.setLayout(stdDistanceLayout);
        stdDistanceBelow.setText(" < ");
        stdDistanceBelow.setToolTipText(tr("below"));
        stdDistanceThresholdField.setToolTipText(tr("Maximum Distance"));
        centroidLabel.setText(tr("Centroid Distance"));
        centroidPanel.setLayout(centroidLayout);
        centroidBelow.setText(" < ");
        centroidBelow.setToolTipText(tr("below"));
        centroidThresholdField.setToolTipText(tr("Maximum Distance"));
        hausdorffLabel.setText(tr("Hausdorff Distance (Centroids Aligned)"));
        symDiffLabel.setText(tr("Symmetric Difference"));
        symDiffCentroidsAlignedLabel.setText(tr(
            "Symmetric Difference (Centroids Aligned)"));
        compactnessLabel.setText(tr("Compactness"));
        anglePanel.setLayout(gridBagLayout8);
        angleLabel.setText(tr("Angle Histogram. Bins: "));
        exactTagsPanel.setLayout(exactTagsdLayout);
        levenshteinTagsPanel.setLayout(levenshteinTagsLayout);
        exactTagsLabel.setText(tr("Tags (Exact Match): "));
        levenshteinTagsLabel.setText(tr("Tags (Levenshtein Distance): "));
        levenshteinTagsField.setToolTipText(tr("List of tags to match"));
        exactTagsField.setToolTipText(tr("List of tags to match"));
        identicalCheckBox.setText(tr("Identical Elements Filter"));
        identicalCheckBox.setToolTipText(tr("Avoid matching an element with itself"));

        filterByAreaTextArea.setEnabled(false);
        filterByAreaTextArea.setBorder(null);
        filterByAreaTextArea.setOpaque(false);
        filterByAreaTextArea.setDisabledTextColor(Color.black);
        filterByAreaTextArea.setEditable(false);
        filterByAreaTextArea.setText(tr(
            "Filtering will speed up the matching process. Filter By Window weeds " +
            "out matches between features whose envelopes do not overlap. Filter " +
            "By Area is used to weed out very small and very large features."));
        filterByAreaTextArea.setLineWrap(true);
        filterByAreaTextArea.setWrapStyleWord(true);
        unioningTextArea.setWrapStyleWord(true);
        unioningTextArea.setLineWrap(true);
        unioningTextArea.setText(tr(
            "Better matches may be found by creating temporary unions of features " +
            "sharing a common edge."));
        unioningTextArea.setEditable(false);
        unioningTextArea.setDisabledTextColor(Color.black);
        unioningTextArea.setOpaque(false);
        unioningTextArea.setBorder(null);
        unioningTextArea.setEnabled(false);
        this.add(upperTabbedPane);

        filteringTab.add(filterByAreaPanel,
            new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        upperTabbedPane.add(matchingTab, tr("Matching"));
        upperTabbedPane.add(filteringTab, tr("Filtering"));
        upperTabbedPane.add(unioningTab, tr("Unioning"));
        matchingTab.add(weightLabel,
            new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        filterByAreaPanel.add(filterByAreaMinField,
            new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        filterByAreaPanel.add(filterByAreaMaxField,
            new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        filterByAreaPanel.add(filterByAreaLabel2,
            new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        filterByAreaPanel.add(filterByAreaLabel1,
            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        filterByAreaPanel.add(filterByAreaCheckBox,
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        filteringTab.add(filterByWindowPanel,
            new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        filterByWindowPanel.add(filterByWindowCheckBox,
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        filterByWindowPanel.add(filterByWindowLabel,
            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        filterByWindowPanel.add(filterByWindowField,
            new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        unioningTab.add(unionCheckBox,
            new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        unioningTab.add(unionLabel1,
            new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        unioningTab.add(unionTextField,
            new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        unioningTab.add(unionLabel2,
            new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        unioningTab.add(unioningTextArea,
            new GridBagConstraints(0, 10, 10, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(10, 4, 4, 4), 0, 0));
        filteringTab.add(identicalCheckBox,
            new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        filteringTab.add(filterByAreaTextArea,
            new GridBagConstraints(1, 10, 2, 2, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(10, 4, 4, 4), 0, 0));
        matchingTab.add(stdDistanceCheckBox,
            new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(stdDistanceWeightField,
            new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(stdDistancePanel,
            new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(centroidCheckBox,
            new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(centroidDistanceWeightField,
            new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(centroidPanel,
            new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(hausdorffCheckBox,
            new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(hausdorffDistanceWeightField,
            new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(hausdorffLabel,
            new GridBagConstraints(4, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(symDiffCheckBox,
            new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(symDiffWeightField,
            new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(symDiffLabel,
            new GridBagConstraints(4, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(symDiffCentroidsAlignedCheckBox,
            new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(symDiffCentroidsAlignedWeightField,
            new GridBagConstraints(3, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(symDiffCentroidsAlignedLabel,
            new GridBagConstraints(4, 5, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(compactnessCheckBox,
            new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(compactnessWeightField,
            new GridBagConstraints(3, 6, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(compactnessLabel,
            new GridBagConstraints(4, 6, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(angleCheckBox,
            new GridBagConstraints(2, 7, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(angleWeightField,
            new GridBagConstraints(3, 7, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(anglePanel,
            new GridBagConstraints(4, 7, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(levenshteinTagsCheckBox,
            new GridBagConstraints(2, 8, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(levenshteinTagsWeightField,
            new GridBagConstraints(3, 8, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(levenshteinTagsPanel,
            new GridBagConstraints(4, 8, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(exactTagsPanel,
            new GridBagConstraints(2, 9, 3, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        anglePanel.add(angleLabel,
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        anglePanel.add(angleBinField,
            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
        matchingTab.add(matchingFillerPanel,
            new GridBagConstraints(50, 50, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        stdDistancePanel.add(stdDistanceLabel,
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
        stdDistancePanel.add(stdDistanceBelow,
                new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
        stdDistancePanel.add(stdDistanceThresholdField,
                new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
        centroidPanel.add(centroidLabel,
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
        centroidPanel.add(centroidBelow,
                new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
        centroidPanel.add(centroidThresholdField,
                new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
        exactTagsPanel.add(exactTagsLabel,
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
        exactTagsPanel.add(exactTagsField,
                new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
        levenshteinTagsPanel.add(levenshteinTagsLabel,
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
        levenshteinTagsPanel.add(levenshteinTagsField,
                new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
    }

    public FeatureMatcher createFeatureMatcher() {
        ArrayList<FeatureMatcher> chainArgs = new ArrayList<>();
        ArrayList<Object> weightedArgs = new ArrayList<>();
        if (getFilterByWindowCheckBox().isSelected()) {
            chainArgs.add(new WindowFilter(filterByWindowField.getDouble()));
        }
        if (stdDistanceCheckBox.isSelected()) {
            double max = stdDistanceThresholdField.getDouble();
            FeatureMatcher matcher = new StandardDistanceMatcher(max);
            if (max > 0) {
                chainArgs.add(matcher);
            }
            weightedArgs.add(Double.valueOf(stdDistanceWeightField.getDouble()));
            weightedArgs.add(matcher);
        }
        if (getCentroidCheckBox().isSelected()) {
            double max = centroidThresholdField.getDouble();
            FeatureMatcher matcher = new CentroidDistanceMatcher(max);
            if (max > 0) {
                chainArgs.add(matcher);
            }
            weightedArgs.add(Double.valueOf(getCentroidDistanceWeightField().getDouble()));
            weightedArgs.add(matcher);
        }
        if (getHausdorffCheckBox().isSelected()) {
            weightedArgs.add(Double.valueOf(getHausdorffDistanceWeightField().getDouble()));
            weightedArgs.add(new CentroidAligner(new HausdorffDistanceMatcher()));
        }
        if (getSymDiffCheckBox().isSelected()) {
            weightedArgs.add(Double.valueOf(getSymDiffWeightField().getDouble()));
            weightedArgs.add(new SymDiffMatcher());
        }
        if (getSymDiffCentroidsAlignedCheckBox().isSelected()) {
            weightedArgs.add(Double.valueOf(getSymDiffCentroidsAlignedWeightField().getDouble()));
            weightedArgs.add(new CentroidAligner(new SymDiffMatcher()));
        }
        if (getCompactnessCheckBox().isSelected()) {
            weightedArgs.add(Double.valueOf(getCompactnessWeightField().getDouble()));
            weightedArgs.add(new CompactnessMatcher());
        }
        if (getAngleCheckBox().isSelected()) {
            weightedArgs.add(Double.valueOf(getAngleWeightField().getDouble()));
            weightedArgs.add(new AngleHistogramMatcher(getAngleBinField().getInteger()));
        }
        if (levenshteinTagsCheckBox.isSelected()) {
            double weight = levenshteinTagsWeightField.getDouble();
            List<String> tags = SimpleMatchFinderPanel.splitBySpaceComaOrSemicolon(levenshteinTagsField.getText());
            for (String tag: tags) {
                weightedArgs.add(Double.valueOf(weight / tags.size()));
                weightedArgs.add(new AttributeMatcher(tag,
                        LevenshteinDistanceValueMatcher.INSTANCE, OsmNormalizeRule.get(tag)));
            }
        }
        List<String> exactTags = SimpleMatchFinderPanel.splitBySpaceComaOrSemicolon(exactTagsField.getText());
        for (String tag: exactTags) {
            chainArgs.add(new AttributeMatcher(tag, ExactValueMatcher.INSTANCE));
        }
        chainArgs.add(new WeightedMatcher(weightedArgs.toArray()));
        if (identicalCheckBox.isSelected()) {
            chainArgs.add(new IdenticalFeatureFilter());
        }
        return new ChainMatcher(chainArgs.toArray(new FeatureMatcher[chainArgs.size()]));
    }

    @Override
    public FCMatchFinder getMatchFinder() {
        FCMatchFinder matchFinder = new BasicFCMatchFinder(createFeatureMatcher());
        //We definitely want to one-to-one before union (combinatorial) -- if after, we'll
        //wipe out some union members! [Jon Aquino]
        matchFinder = new DisambiguatingFCMatchFinder(matchFinder);
        TargetUnioningFCMatchFinder targetUnioningFCMatchFinder = null;
        if (getUnionCheckBox().isSelected()) {
            int maxUnionMembers = Integer.parseInt(getUnionTextField().getText());
            targetUnioningFCMatchFinder = new TargetUnioningFCMatchFinder(maxUnionMembers, matchFinder);
            matchFinder = targetUnioningFCMatchFinder;
        }
        if (getFilterByAreaCheckBox().isSelected()) {
            matchFinder = new AreaFilterFCMatchFinder(
                    getFilterByAreaMinField().getDouble(),
                    getFilterByAreaMaxField().getDouble(),
                    matchFinder);
        }
        return matchFinder;
    }

    @Override
    public void savePreferences(Preferences pref) {
        pref.putBoolean(getClass().getName() + ".filterByAreaCheckBox", filterByAreaCheckBox.isSelected());
        pref.putBoolean(getClass().getName() + ".filterByWindowCheckBox", filterByWindowCheckBox.isSelected());
        pref.putBoolean(getClass().getName() + ".unionCheckBox", unionCheckBox.isSelected());
        pref.putBoolean(getClass().getName() + ".stdDistanceCheckBox", stdDistanceCheckBox.isSelected());
        pref.putBoolean(getClass().getName() + ".centroidCheckBox", centroidCheckBox.isSelected());
        pref.putBoolean(getClass().getName() + ".hausdorffCheckBox", hausdorffCheckBox.isSelected());
        pref.putBoolean(getClass().getName() + ".symDiffCheckBox", symDiffCheckBox.isSelected());
        pref.putBoolean(getClass().getName() + ".symDiffCentroidsAlignedCheckBox", symDiffCentroidsAlignedCheckBox.isSelected());
        pref.putBoolean(getClass().getName() + ".compactnessCheckBox", compactnessCheckBox.isSelected());
        pref.putBoolean(getClass().getName() + ".angleCheckBox", angleCheckBox.isSelected());
        pref.putBoolean(getClass().getName() + ".levenshteinTagsCheckBox", levenshteinTagsCheckBox.isSelected());
        pref.putInt(getClass().getName() + ".unionTextField", unionTextField.getInteger());
        pref.putInt(getClass().getName() + ".angleBinField", angleBinField.getInteger());
        pref.putDouble(getClass().getName() + ".filterByAreaMinField", filterByAreaMinField.getDouble());
        pref.putDouble(getClass().getName() + ".filterByAreaMaxField", filterByAreaMaxField.getDouble());
        pref.putDouble(getClass().getName() + ".filterByWindowField", filterByWindowField.getDouble());
        pref.putDouble(getClass().getName() + ".stdDistanceWeightField", stdDistanceWeightField.getDouble());
        pref.putDouble(getClass().getName() + ".stdDistanceThresholdField", stdDistanceThresholdField.getDouble());
        pref.putDouble(getClass().getName() + ".centroidDistanceWeightField", centroidDistanceWeightField.getDouble());
        pref.putDouble(getClass().getName() + ".centroidThresholdField", centroidThresholdField.getDouble());
        pref.putDouble(getClass().getName() + ".hausdorffDistanceWeightField", hausdorffDistanceWeightField.getDouble());
        pref.putDouble(getClass().getName() + ".symDiffWeightField", symDiffWeightField.getDouble());
        pref.putDouble(getClass().getName() + ".symDiffCentroidsAlignedWeightField", symDiffCentroidsAlignedWeightField.getDouble());
        pref.putDouble(getClass().getName() + ".compactnessWeightField", compactnessWeightField.getDouble());
        pref.putDouble(getClass().getName() + ".angleWeightField", angleWeightField.getDouble());
        pref.putDouble(getClass().getName() + ".levenshteinTagsWeightField", levenshteinTagsWeightField.getDouble());
        pref.put(getClass().getName() + ".exactTagsField", exactTagsField.getText());
        pref.put(getClass().getName() + ".levenshteinTagsField", levenshteinTagsField.getText());
        pref.putBoolean(getClass().getName() + ".identicalCheckBox", identicalCheckBox.isSelected());
    }

    public void restoreFromPreferences(Preferences pref) {
        filterByAreaCheckBox.setSelected(pref.getBoolean(getClass().getName() + ".filterByAreaCheckBox", true));
        filterByWindowCheckBox.setSelected(pref.getBoolean(getClass().getName() + ".filterByWindowCheckBox", true));
        unionCheckBox.setSelected(pref.getBoolean(getClass().getName() + ".unionCheckBox", false));
        stdDistanceCheckBox.setSelected(pref.getBoolean(getClass().getName() + ".stdDistanceCheckBox", true));
        centroidCheckBox.setSelected(pref.getBoolean(getClass().getName() + ".centroidCheckBox", true));
        hausdorffCheckBox.setSelected(pref.getBoolean(getClass().getName() + ".hausdorffCheckBox", true));
        symDiffCheckBox.setSelected(pref.getBoolean(getClass().getName() + ".symDiffCheckBox", true));
        symDiffCentroidsAlignedCheckBox.setSelected(pref.getBoolean(getClass().getName() + ".symDiffCentroidsAlignedCheckBox", true));
        compactnessCheckBox.setSelected(pref.getBoolean(getClass().getName() + ".compactnessCheckBox", true));
        angleCheckBox.setSelected(pref.getBoolean(getClass().getName() + ".angleCheckBox", true));
        levenshteinTagsCheckBox.setSelected(pref.getBoolean(getClass().getName() + ".levenshteinTagsCheckBox", true));
        unionTextField.setText("" + Integer.max(2, pref.getInt(getClass().getName() + ".unionTextField", 2)));
        angleBinField.setText("" +Integer.max(2, pref.getInt(getClass().getName() + ".angleBinField", 18)));
        filterByAreaMinField.setText("" + Double.max(0.0, pref.getDouble(getClass().getName() + ".filterByAreaMinField", 0.0)));
        filterByAreaMaxField.setText("" + Double.max(0.0, pref.getDouble(getClass().getName() + ".filterByAreaMaxField", 9E6)));
        filterByWindowField.setText("" + Double.max(0.0, pref.getDouble(getClass().getName() + ".filterByWindowField", 50.0)));
        stdDistanceWeightField.setText("" + Double.max(0.0,
                pref.getDouble(getClass().getName() + ".stdDistanceWeightField", 10.0)));
        stdDistanceThresholdField.setText("" + Double.max(0.0, pref.getDouble(getClass().getName() + ".stdDistanceThresholdField", 50.0)));
        centroidDistanceWeightField.setText("" + Double.max(0.0,
                pref.getDouble(getClass().getName() + ".centroidDistanceWeightField", 10.0)));
        centroidThresholdField.setText("" + Double.max(0.0, pref.getDouble(getClass().getName() + ".centroidThresholdField", 50.0)));
        hausdorffDistanceWeightField.setText("" + Double.max(0.0,
                pref.getDouble(getClass().getName() + ".hausdorffDistanceWeightField", 10.0)));
        symDiffWeightField.setText("" + Double.max(0.0, pref.getDouble(getClass().getName() + ".symDiffWeightField", 10.0)));
        symDiffCentroidsAlignedWeightField.setText("" + Double.max(0.0,
                pref.getDouble(getClass().getName() + ".symDiffCentroidsAlignedWeightField", 10.0)));
        compactnessWeightField.setText("" + Double.max(0.0, pref.getDouble(getClass().getName() + ".compactnessWeightField", 10.0)));
        angleWeightField.setText("" + Double.max(0.0, pref.getDouble(getClass().getName() + ".angleWeightField", 10.0)));
        levenshteinTagsWeightField.setText("" + Double.max(0.0,
                pref.getDouble(getClass().getName() + ".levenshteinTagsWeightField", 50.0)));
        exactTagsField.setText(pref.get(getClass().getName() + ".exactTagsField", ""));
        levenshteinTagsField.setText(pref.get(getClass().getName() + ".levenshteinTagsField", ""));
        identicalCheckBox.setSelected(pref.getBoolean(getClass().getName() + ".identicalCheckBox", false));

    }

}

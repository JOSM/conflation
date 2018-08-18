// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.config;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.openstreetmap.josm.plugins.conflation.config.parser.InstanceConstructor;
import org.openstreetmap.josm.plugins.conflation.config.parser.InstanceEditor;
import org.openstreetmap.josm.spi.preferences.IPreferences;

import com.vividsolutions.jcs.conflate.polygonmatch.FCMatchFinder;
import com.vividsolutions.jcs.conflate.polygonmatch.FeatureMatcher;


/**
 * Programming Match Finder Panel.
 * Allow user to directly enter a constructor expression.
 */
public final class ProgrammingMatchFinderPanel extends MatchFinderPanel {

    private final InstanceEditor<FCMatchFinder> editorPanel;

    private static final String SIMPLE_EXAMPLE =
            "Disambiguating(\n" +
            "\tBasic(\n" +
            "\t\tChain(\n" +
            "\t\t\tWindow( 20 ),\n" +
            "\t\t\tHausdorffDistance( 20 )\n" +
            //"\t\t\t, IdenticalFilter)\n" +
            ")))\n";

    private static final String ADVANCED_EXAMPLE =
            "AreaFilter ( 0.0, 9E6, \n" +
            "\tTargetUnioning( 2,\n" +
            "\t\tDisambiguating(\n" +
            "\t\t\tBasic(\n" +
            "\t\t\t\tChain(\n" +
            "\t\t\t\t\tWindow( 50 ),\n" +
            "\t\t\t\t\tStandardDistance( 50 ),\n" +
            "\t\t\t\t\tCentroidDistance( 50 ),\n" +
            "\t\t\t\t\tAttribute( 'exact_tag', Exact, NONE ),\n" +
            "\t\t\t\t\tWeighted(\n" +
            "\t\t\t\t\t\t10, StandardDistance( 50 ),\n" +
            "\t\t\t\t\t\t10, CentroidDistance( 50 ),\n" +
            "\t\t\t\t\t\t10, CentroidAligner( HausdorffDistance( 0 ) ),\n" +
            "\t\t\t\t\t\t10, SymDiff,\n" +
            "\t\t\t\t\t\t10, CentroidAligner( SymDiff ),\n" +
            "\t\t\t\t\t\t10, Compactness,\n" +
            "\t\t\t\t\t\t10, AngleHistogram( 18 ),\n" +
            "\t\t\t\t\t\t50, Attribute( 'name', LevenshteinDistance( 0 ), OsmRule( 'name' ))\n" +
            "\t\t\t\t\t),\n" +
            "\t\t\t\t\tIdenticalFilter\n" +
            ")))))";

    public ProgrammingMatchFinderPanel(IPreferences pref) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        editorPanel = new InstanceEditor<>(FCMatchFinder.class, "Match Finder", jcsConstructors, 8, 70);
        this.add(editorPanel);

        this.add(createExampleButtonPanel());
        restoreFromPreferences(pref);
    }

    private JPanel createExampleButtonPanel() {
        JPanel buttonsPanel = new JPanel();
        JButton simpleButton = new JButton(tr("Simple Example"));
        JButton advancedButton = new JButton(tr("Advanced Example"));
        buttonsPanel.add(simpleButton);
        buttonsPanel.add(advancedButton);
        simpleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editorPanel.getTextArea().replaceRange(SIMPLE_EXAMPLE, 0, editorPanel.getTextArea().getText().length());
            }
        });
        advancedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editorPanel.getTextArea().replaceRange(ADVANCED_EXAMPLE, 0, editorPanel.getTextArea().getText().length());
            }
        });
        return buttonsPanel;
    }

    @Override
    public FCMatchFinder getMatchFinder() {
        return editorPanel.getEditedInstance();
    }

    public void restoreFromPreferences(IPreferences pref) {
        editorPanel.getTextArea().setText(pref.get(getClass().getName() + ".expression", SIMPLE_EXAMPLE));
    }

    @Override
    public void savePreferences(IPreferences pref) {
        pref.put(getClass().getName() + ".expression", editorPanel.getTextArea().getText());
    }


    /**
     * List of consturctors for the Java Conflation Suite FCMatchFinder.
     */
    public static final InstanceConstructor[] jcsConstructors = new InstanceConstructor[] {
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.AngleHistogramMatcher.class,
                    "AngleHistogram",
                    "Matches geometries by comparing their 'angle histograms'. An angle histogram "
                    + "is a histogram of segment angles (with the positive x-axis), weighted by "
                    + "segment length. Angles range from -pi to +pi.",
                    new String[] {
                            "the number of bins into which the angles (-pi to +pi) should be split"
                   }),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.AreaFilterFCMatchFinder.class,
                    "AreaFilter",
                    "Speeds up processing by ignoring target and candidate features with areas "
                    + "greater than a specified maximum or less than a specified minimum.",
                    new String[] {"mininimum area", "maximum area", "match finder"}),
            new InstanceConstructor(
                    org.openstreetmap.josm.plugins.conflation.matcher.AttributeMatcher.class,
                    "Attribute",
                    "Match a specific attribute",
                    new String[] {"the attribute name", "how to compute score", "normalize values"}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.BasicFCMatchFinder.class,
                    "Basic",
                    "Applies a FeatureMatcher to each item",
                    new String[] {"matcher"}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.CentroidAligner.class,
                    "CentroidAligner",
                    "Centroid Aligner",
                    new String[] {"matcher"}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.CentroidDistanceMatcher.class,
                    "CentroidDistance",
                    "Centroid Distance",
                    new String[] {"maximum distance, if 0 then score will be relative to the combined envelope diagonale."}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.ChainMatcher.class,
                    "Chain",
                    "Composes several FeatureMatchers into one. Candidate features are whittled "
                    + "down by applying each FeatureMatcher."
                    + "\n\n"
                    + "Note: Only the last FeatureMatcher's scores are preserved; the other scores "
                    + "are lost. However, this behaviour should be acceptable for most situations "
                    + "Typically you use the Chained Matcher to do some initial filtering before "
                    + "the 'real' matching. The scores from this initial filtering are usually "
                    + "ignored (they're usually just 1 or 0, as in the case of WindowFilter).",
                    new String[] {"matchers"}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.CombinatorialFCMatchFinder.class,
                    "Combinatorial",
                    "An FCMatchFinder wrapper that also treats pairs of adjacent target features "
                    + "as themselves target features. Such pairs are formed into composite target "
                    + "features. These composites are temporary -- before the results are returned, "
                    + "each composite is split into its constituent features. <P>"
                    + "\n\n"
                    + "The result returned is a one-to-one mapping of target feature to matched "
                    + "candidate feature; the one-to-one mapping is achieved by discarding all "
                    + "matches except for those with the highest scores, for each feature (target "
                    + "and matched candidate). <P>"
                    + "\n\n"
                    + "Note on composites: if a composite's top score is higher than the top score "
                    + "of each of its constituents, the composite match is retained and constituent "
                    + "matches are discarded; otherwise, the composite match is discarded and "
                    + "constituent matches are retained.",
                    new String[] {"maximum composite size", "match finder"}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.CompactnessMatcher.class,
                    "Compactness",
                    "Uses (4 x pi x Area) / (Perimeter^2) as a shape characteristic. The "
                    + "maximum value is 1 (for circles).",
                    new String[] {}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.DisambiguatingFCMatchFinder.class,
                    "Disambiguating",
                    "Enforces a one-to-one relationship between target features and "
                    + "matched candidate features, in the returned result set.\n"
                    + "'Aggressive' because 2nd, 3rd, 4th, etc. best "
                    + "matches are tried if the 1st, 2nd, 3rd, etc. match is 'taken' by another "
                    + "feature.",
                    new String[] {"match finder"}),
            //new InstanceConstructor(
            //        com.vividsolutions.jcs.conflate.polygonmatch.FeatureCollectionMatcher.class,
            //        "Applies a FeatureMatcher to each item in a FeatureCollection",
            //        new String[] {"matcher: typically a composite of other FeatureMatchers"}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.HausdorffDistanceMatcher.class,
                    "HausdorffDistance",
                    "Hausdorff Distance",
                    new String[] {"maximum distance, if 0 then score will be relative to the combined envelope diagonale."}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.IdenticalFeatureFilter.class,
                    "IdenticalFilter",
                    "Filters out matches where features are identical.",
                    new String[] {}),
            new InstanceConstructor(
                    org.openstreetmap.josm.plugins.conflation.matcher.StandardDistanceMatcher.class,
                    "StandardDistance",
                    "Standard Distance (i.e. the minimum) between the two geometries.",
                    new String[] {"maximum distance, if 0 then score will be relative to the combined envelope diagonale."}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.MinScoreMatcher.class,
                    "MinScore",
                    "Filters out shapes with a score below a given value.",
                    new String[] {"minimun score"}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.OneToOneFCMatchFinder.class,
                    "OneToOne",
                    "Enforces a one-to-one relationship between target features and "
                    + "matched candidate features, in the returned result set. The one-to-one "
                    + "constraint is achieved by discarding all matches except the top match, "
                    + "for each feature (target and candidate)."
                    + "\n\n"
                    + "Example: 'OneToOne' wraps a FCMatchFinder that returns the "
                    + "following matches (and scores):\nT1-C1 (0.8), T2-C1 (0.9), T2-C2 (0.8), "
                    + "T2-C3 (1.0), T3-C4 (0.5).\nT1 and T2 are from the target dataset, whereas "
                    + "C1, C2, and C3 are from the candidate dataset. 'OneToOne' filters "
                    + "out all matches except the top ones, for each feature, leaving:\n"
                    + "T2-C3 (1.0), T3-C4 (0.5).",
                    new String[] {"match finder"}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.OverlapMatcher.class,
                    "Overlap",
                    "",
                    new String[] {}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.ScaleScoresMatcher.class,
                    "ScaleScores",
                    "Re-scales the scores output from another FeatureMatcher",
                    new String[] {"newZeroScore: the score that will be warped to 0",
                            "newFullScore the score that will be warped to 1"}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.ScoreStretcher.class,
                    "ScoreStretcher",
                    "Re-scales the scores output from another FeatureMatcher",
                    new String[] {"minimum score, that will be warped to 0", "maximum ccore, that will be warped to 1"}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.SymDiffMatcher.class,
                    "SymDiff",
                    "Uses symmetric difference as the criterion for determining match scores.",
                    new String[] {}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.TargetUnioningFCMatchFinder.class,
                    "TargetUnioning",
                    "An FCMatchFinder wrapper that also treats unions of adjacent target features "
                    + "as themselves target features. Such unions are formed into composite target "
                    + "features. These composites are temporary -- before the results are returned, "
                    + "each composite is split into its constituent features."
                    + "\n\n"
                    + "The result returned is a one-to-one mapping of target feature to matched "
                    + "candidate feature; the one-to-one mapping is achieved by discarding all "
                    + "matches except for those with the highest scores, for each feature (target "
                    + "and matched candidate)."
                    + "\n\n"
                    + "Note on composites: if a composite's top score is higher than the top score "
                    + "of each of its constituents, the composite match is retained and constituent "
                    + "matches are discarded; otherwise, the composite match is discarded and "
                    + "constituent matches are retained.",
                    new String[] {
                            "maximum number of adjacent target features to try combining",
                            "match finder"
                   }),
            //new ConstructorDescription(
            //        com.vividsolutions.jcs.conflate.polygonmatch.ThresholdFilter.class,
            //        "Filters out shapes with a score below a given value.",
            //        new String[] {"minScore the score below which shapes will be filtered out"}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.TopMatchDisambiguatingFCMatchFinder.class,
                    "TopMatchDisambiguating",
                    "Enforces a one-to-one relationship between target features and "
                    + "matched candidate features, in the returned result set. "
                    + "'Conservative' because only top matches are allowed. "
                    + "\n\n"
                    + "Note: 'Disambiguating' finder seems to give better results."
                    + "\n\n"
                    + "Example: 'OneToOne' finder wraps a FCMatchFinder that returns the "
                    + "following matches (and scores):\nT1-C1 (0.8), T2-C1 (0.9), T2-C2 (0.8), "
                    + "T2-C3 (1.0), T3-C4 (0.5).\nT1 and T2 are from the target dataset, whereas "
                    + "C1, C2, and C3 are from the candidate dataset. 'OneToOne' filters "
                    + "out all matches except the top ones, for each feature, leaving:\n"
                    + "T2-C3 (1.0), T3-C4 (0.5).",
                    new String[] {"match finder"}),
            //new ConstructorDescription(
            //        com.vividsolutions.jcs.conflate.polygonmatch.TopScoreFilter.class,
            //        "Filters out all shapes except the one with the top score. Not needed"
            //        + "if you are using OneToOneFCMatchFinder.",
            //        new String[] {}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.TopScoreMatcher.class,
                    "TopScore",
                    "Filters out all shapes except the one with the top score.",
                    new String[] {}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.WeightedMatcher.class,
                    "Weighted",
                    "Runs multiple FeatureMatchers, and combines their scores using a weighted average."
                    + "\n\n"
                    + "Take as argument matchers and weights: alternates between FeatureMatchers and floating point numbers.",
                    new String[] {"weights and matchers: alternates between Doubles and FeatureMatchers"},
                    new Class<?>[] {Double.class, FeatureMatcher.class},
                    new String[] {"the weight given to scores returned by the matcher", "a matcher to add"}),
            //new ConstructorDescription(
            //        com.vividsolutions.jcs.conflate.polygonmatch.WindowFilter.class,
            //        "Quickly filters out shapes that lie outside a given distance from the feature's envelope.",
            //        new String[] {"buffer: for each feature, the window will be the envelope extended on each side by this amount"}),
            new InstanceConstructor(
                    com.vividsolutions.jcs.conflate.polygonmatch.WindowMatcher.class,
                    "Window",
                    "Quickly filters out shapes that lie outside a given distance from the feature's envelope.",
                    new String[] {"buffer: for each feature, the window will be the envelope extended on each side by this amount"}),

            // ---------------------------------------------------------------------------------------------------
            // ValueMatcher
            // ---------------------------------------------------------------------------------------------------

            new InstanceConstructor(
                    org.openstreetmap.josm.plugins.conflation.matcher.ExactValueMatcher.class,
                    "Exact",
                    "Match only value exactly identical",
                    new String[] {}),
            new InstanceConstructor(
                    org.openstreetmap.josm.plugins.conflation.matcher.LevenshteinDistanceValueMatcher.class,
                    "LevenshteinDistance",
                    "Score according to the Levenshtein distance\n\n" +
                    "If the given parameter is 0 then the score will be relative to the string length.",
                    new String[] {"max number of character modifications, defaut value 0 means max is the string length"}),

            // ---------------------------------------------------------------------------------------------------
            // NormalizeRule
            // ---------------------------------------------------------------------------------------------------

            new InstanceConstructor(
                    org.openstreetmap.josm.plugins.conflation.matcher.NoneNormalizeRule.class,
                    "None",
                    "No normalization",
                    new String[] {}),
            new InstanceConstructor(
                    org.openstreetmap.josm.plugins.conflation.matcher.AccentlessNormalizeRule.class,
                    "Accentless",
                    "Remove accent",
                    new String[] {}),
            new InstanceConstructor(
                    org.openstreetmap.josm.plugins.conflation.matcher.LetterOrDigitNormalizeRules.class,
                    "LetterOrDigit",
                    "Keep only letters or digits characters (remove spaces, punctuation,...)\n\n" +
                    "REM: there will be no more word separations",
                    new String[] {}),
            new InstanceConstructor(
                    org.openstreetmap.josm.plugins.conflation.matcher.LowerCaseNormalizeRule.class,
                    "LowerCase",
                    "Normalize to lower case",
                    new String[] {}),
            new InstanceConstructor(
                    org.openstreetmap.josm.data.validation.tests.SimilarNamedWays.RegExprRule.class,
                    "RegExpr",
                    "Apply regular expression substitution",
                    new String[] {"regular expression", "replacement"}),
            new InstanceConstructor(
                    org.openstreetmap.josm.data.validation.tests.SimilarNamedWays.SynonymRule.class,
                    "Synonym",
                    "Substitute synonyms by a common word",
                    new String[] {"replacement", "synomym words"}),
            new InstanceConstructor(
                    org.openstreetmap.josm.plugins.conflation.matcher.OsmNormalizeRule.class,
                    "OsmRule",
                    "Arbitrary normalization to be improoved....\n\n" +
                    "If the tags name contains the word 'name', is 'addr:street' or 'addr:place'," +
                    "then the following normalization list is applied:\n" +
                    " - Accentless, LowerCase, LetterOrDigit",
                    new String[] {"tag name (need to be specified again"}),

   };

}

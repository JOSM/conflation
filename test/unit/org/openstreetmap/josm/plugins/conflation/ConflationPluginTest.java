// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.io.importexport.OsmImporter;
import org.openstreetmap.josm.gui.io.importexport.OsmImporter.OsmImporterData;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.plugins.conflation.command.ConflateMatchCommand;
import org.openstreetmap.josm.plugins.conflation.command.ConflateUnmatchedObjectCommand;
import org.openstreetmap.josm.plugins.conflation.config.MatchingPanel;
import org.openstreetmap.josm.plugins.conflation.config.MergingPanel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.I18n;

/**
 * FIXME: this class is not functional yet
 */
public class ConflationPluginTest {
    
    @Rule
    public JOSMTestRules rules = new JOSMTestRules().preferences();
    
    static String[] testList = new String[]{"test1-buildings"};
        
    @Test
    @Ignore("FIXME: restore test when @{link #compare} method will be fixed")
    public void testConflationPlugin() throws FileNotFoundException, XMLStreamException, IOException, IllegalDataException {
        for (String testName: testList) {
            assertTrue(testName, testCase(testName));
        }
    }
    
    static boolean testCase(String name) throws FileNotFoundException, XMLStreamException, IOException, IllegalDataException {
        SimpleMatchSettings settings = openTestCase(name);
        OsmDataLayer expectedResult = openTestResult(name);        
        SimpleMatchList matchesList = new SimpleMatchList();
        matchesList.addAll(MatchesComputation.generateMatches(settings, NullProgressMonitor.INSTANCE));
        Collection<OsmPrimitive> referenceOnly = settings.referenceSelection.stream()
                .filter((p) -> matchesList.getMatchByReference(p) == null)
                .collect(Collectors.toList());; 
        Collection<OsmPrimitive> subjectOnly = settings.subjectSelection.stream()
                .filter((p) -> matchesList.getMatchBySubject(p) == null)
                .collect(Collectors.toList());; 
        while (matchesList.size() > 0) {
            SimpleMatch match = matchesList.get(matchesList.size()-1);
            new ConflateMatchCommand(match, matchesList, settings).executeCommand();
        }
        UnmatchedObjectListModel referenceOnlyListModel = new UnmatchedObjectListModel();
        referenceOnlyListModel.addAll(referenceOnly);
        Command cmd = new ConflateUnmatchedObjectCommand(settings.referenceLayer, settings.subjectDataSet, 
                referenceOnly, referenceOnlyListModel);
        assertTrue(cmd.executeCommand()); 
        subjectOnly.forEach((p) -> p.setDeleted(true));
        
        return compare(settings.subjectLayer, expectedResult);
    }
    
    static boolean compare(OsmDataLayer layer1, OsmDataLayer layer2) {
        //FIXME: we should compare as equals primitives that have different negative id.
        for (OsmPrimitive prim1: layer1.data.allPrimitives()) {
            OsmPrimitive prim2 = layer2.data.getPrimitiveById(prim1);
            if (prim2 == null) {
                System.out.println(prim1 + " don't exist in layer2");           
                return false;
            } else if (!prim1.save().equals(prim2.save())) {
                System.out.println(prim1 + " != " + prim2);
                return false;
            }
        }
        return true;
    }
    
    /**
     * Open a test case and return the corresponding settings.
     * 
     * A test case a directory "test/data/<b>name></b>/".
     * 
     * This directory should contains a files "reference.osm", a file "subject.osm"
     * or a file "reference-subject.osm" if the same layer should be used
     * for both reference and subject.
     * 
     * The directory should also contain a file named "result.xml" for
     * the expected result of the conflation.
     * 
     * The directory could contain a file named "preferences.xml" to specify
     * specific settings.
     */
    static SimpleMatchSettings openTestCase(String name) 
            throws FileNotFoundException, XMLStreamException, IOException, IllegalDataException {
        SimpleMatchSettings settings = new SimpleMatchSettings();
        File path = new File(new File("test", "data"), name);
        assertTrue(path.isDirectory());
        OsmDataLayer reference = importOsmFile(new File(path, "reference.osm"));
        OsmDataLayer subject;
        if (reference == null) {
            reference = importOsmFile(new File(path, "reference-subject.osm"));
            assertNotNull("referece.osm or reference-subject.osm file not found in directory " + path, reference);
            subject = reference;
        } else {
            subject = importOsmFile(new File(path, "subject.osm"));
            assertNotNull("subject.osm file not found in directory " + path, subject);
        }
        Preferences pref = openPreferencesXML(new File(path, "preferences.xml"));
        settings.subjectLayer = subject;
        settings.subjectDataSet = subject.getDataSet();
        settings.referenceLayer = reference;
        settings.referenceDataSet = reference.getDataSet();
        settings.referenceSelection = reference.data.allPrimitives().stream().filter(
                (p) -> "yes".equals(p.get("reference"))).collect(Collectors.toList());
        settings.subjectSelection = subject.data.allPrimitives().stream().filter(
                (p) -> "yes".equals(p.get("subject"))).collect(Collectors.toList());;
        new MatchingPanel(null, pref, () -> { }).fillSettings(settings);
        new MergingPanel(null, pref).fillSettings(settings);
        return settings;
    }

    static OsmDataLayer openTestResult(String name) throws IllegalDataException {
        File path = new File(new File("test", "data"), name);
        return importOsmFile(new File(path, "result.osm"));
    }
    
    public static Preferences openPreferencesXML(File file) throws FileNotFoundException, XMLStreamException, IOException {
        Preferences pref = new Preferences();
        if (file.exists()) {
            pref.fromXML(new BufferedReader(new FileReader(file)));
        }
        return pref;
    }

    public static OsmDataLayer importOsmFile(File file) throws IllegalDataException {
        OsmImporter importer = new OsmImporter();
        try {
            InputStream in = new FileInputStream(file);
            OsmImporterData oid = importer.loadLayer(in, file, file.getName(), NullProgressMonitor.INSTANCE);
            OsmDataLayer layer = oid.getLayer();
            return layer;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    static boolean isInitialized;
    @BeforeClass
    public static void setUpBeforeClass() {
      if (!isInitialized) {
          //System.setProperty("josm.home", "test/data/preferences");
          Preferences.main().enableSaveOnPut(false);
          I18n.init();
          Preferences.main().init(false);
          I18n.set(Config.getPref().get("language", "en"));
          ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator
          isInitialized = true;
          ExpertToggleAction.isExpert(); // to be sure it is initialized
        }
    }

}

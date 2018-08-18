// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation;

import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.spi.preferences.Config;

public class ConflationPlugin extends Plugin {

    /**
     * constructor
     */
    public ConflationPlugin(PluginInformation info) {
        super(info);
    }

    // add dialog the first time the mapframe is loaded
    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        if (oldFrame == null && newFrame != null) {
            newFrame.addToggleDialog(new ConflationToggleDialog(this, Config.getPref()));
        }
    }
}

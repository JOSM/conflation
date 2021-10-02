// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.config;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;

/**
 * Display a default text in the middle of the component instead of empty field.
 */
public class DefaultPromptTextField extends AutoCompletingTextField {

    String promptText;

    public DefaultPromptTextField(int column, String promptText) {
        super(column);
        this.promptText = promptText;
    }

    public void setPrompt(String newPromptText) {
        this.promptText = newPromptText;
        this.invalidate();
    }

    public String getPrompt() {
        return promptText;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!hasFocus() && getText().isEmpty()) {
            Graphics2D g2 = (Graphics2D) g;
            Font font = getFont().deriveFont(Font.ITALIC);
            g2.setFont(font);
            g2.setColor(getDisabledTextColor());
            Rectangle2D rect = font.getStringBounds(promptText, g2.getFontRenderContext());
            g2.drawString(promptText,
                    (getWidth() - (int) rect.getWidth()) / 2,
                    getBaseline(getWidth(), getHeight()));
        }
    }

}

// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.config.parser;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import org.openstreetmap.josm.gui.widgets.JosmTextArea;

/**
 * A JTextArea that validate the content against an {@link IParser}.
 */
public class ParsedTextArea extends JosmTextArea {

    final IParser parser;
    final JMenuItem errorPopupItem = new JMenuItem();
    final JMenuItem infoPopupItem = new JMenuItem();

    public ParsedTextArea(IParser parser) {
        this("", 0, 0, parser);
    }

    public ParsedTextArea(String text, int rows, int columns, IParser parser) {
        super(text, rows, columns);
        this.parser = parser;
        this.setTabSize(4);
        this.setFont(new Font("monospaced", Font.PLAIN, this.getFont().getSize()));
        this.setRows(rows);
        this.setColumns(columns);
        JPopupMenu errorPopup = new JPopupMenu();
        errorPopupItem.setBackground(Color.RED);
        errorPopup.add(errorPopupItem);
        errorPopupItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                errorPopup.setVisible(false);
            }
        });
        JPopupMenu infoPopup = new JPopupMenu();
        infoPopup.add(infoPopupItem);
        this.infoPopupItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                infoPopup.setVisible(false);
            }
        });
        this.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateStatusMessage();
            }
        });
        this.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                updateStatusMessage();
            }
        });
        this.addComponentListener(new ComponentListener() {
            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentResized(ComponentEvent e) {
                updateStatusMessage();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                updateStatusMessage();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                hideStatusMessage();
            }
        });
        this.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                hideStatusMessage();
            }
        });
    }

    public void updateStatusMessage() {
        if (isShowing()) {
            updateErrorMessage();
            updateInfoMessage();
        }
    }

    public void hideStatusMessage() {
        ((JPopupMenu) errorPopupItem.getParent()).setVisible(false);
        ((JPopupMenu) infoPopupItem.getParent()).setVisible(false);
    }

    void updateErrorMessage() {
        parser.parse(getText());
        updatePopup(errorPopupItem, parser.getErrorMessage(), parser.getLastTokenIndex(), false);
    }

    private void updateInfoMessage() {
        parser.parse(getText().substring(0, getCaretPosition()));
        if (parser.isFullyParsed() && !parser.isValid()) {
            updatePopup(infoPopupItem, parser.getLastTokenDescription(), parser.getLastTokenIndex(), true);
        } else {
            ((JPopupMenu) infoPopupItem.getParent()).setVisible(false);
        }
    }

    private static void repackPopupMenuWithoutFlicker(JPopupMenu popupMenu) {
        Window window = SwingUtilities.getWindowAncestor(popupMenu);
        if (window != null) {
            window.pack();
            window.validate();
        } else {
            popupMenu.pack();
            popupMenu.validate();
        }
    }

    private void updatePopup(JMenuItem popupMenuItem, String text, int position, boolean above) {
        JPopupMenu popup = (JPopupMenu) popupMenuItem.getParent();
        if ((text != null) && !text.isEmpty()) {
            Point textArealocation = getLocationOnScreen();
            Rectangle positionRect;
            try {
                positionRect = modelToView(position);
            } catch (BadLocationException e) {
                positionRect = new Rectangle(0, 0, getSize().width, getSize().height);
            }
            popupMenuItem.setText(text);
            repackPopupMenuWithoutFlicker(popup);
            if (above) {
                popup.setLocation(textArealocation.x + positionRect.x,
                        textArealocation.y + positionRect.y - positionRect.height - popup.getHeight());
            } else {
                popup.setLocation(textArealocation.x + positionRect.x,
                        textArealocation.y + positionRect.y + positionRect.height + 10);
            }
            popup.setVisible(true);
        } else {
            popup.setVisible(false);
        }
    }

    @Override
    protected Document createDefaultModel() {
        return new AutoCompletionDocument();
    }

    class AutoCompletionDocument extends PlainDocument {
        @Override
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            // if the current offset isn't at the end of the document we don't
            // autocomplete.
            if (offs < getLength()) {
                super.insertString(offs, str, a);
                return;
            }
            String currentText = getText(0, getLength());
            parser.parse(currentText + str);
            // System.out.println(parser.toString());
            List<String> autoCompleteList = parser.getCompletionList();
            if (parser.isFullyParsed() && (autoCompleteList.size() > 0)
                    && ((parser.getLastTokenIndex() <= getLength()) || (autoCompleteList.size() == 1))) {
                String matchingString = autoCompleteList.get(0);
                if (parser.getLastTokenIndex() <= getLength()) {
                    remove(parser.getLastTokenIndex(), currentText.length() - parser.getLastTokenIndex());
                    super.insertString(parser.getLastTokenIndex(), matchingString, a);
                } else {
                    super.insertString(offs, str, a);
                    super.insertString(parser.getLastTokenIndex(), matchingString, a);
                }
                // highlight from insert position to end position to put the
                // caret at the end
                setCaretPosition(offs + str.length());
                moveCaretPosition(getLength());
            } else {
                super.insertString(offs, str, a);
            }
        }
    }
}

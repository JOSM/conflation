// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.config.parser;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import org.openstreetmap.josm.gui.widgets.JosmTextArea;

public class InstanceEditor<T> extends JPanel {

    HashMap<String, JLabel> classLabelMap = new HashMap<>();
    HashMap<String, InstanceConstructor> constructorNameMap = new HashMap<>();
    JTextArea classDescriptionTextArea;
    ParsedTextArea finderExpressionTextArea;
    InstanceParser<T> parser;

    public InstanceEditor(Class<T> mainType, String mainDescription, InstanceConstructor[] constructors, int rows, int cols) {
        parser = new InstanceParser<>(mainType, mainDescription, constructors);
        for (InstanceConstructor cnstr: constructors) {
            constructorNameMap.put(cnstr.name.toLowerCase(), cnstr);
        }

        JPanel classListPanel = new JPanel();
        classListPanel.setLayout(new BoxLayout(classListPanel, BoxLayout.Y_AXIS));
        JScrollPane classListPanelScrollPane = new JScrollPane(classListPanel);

        for (InstanceConstructor cnstrDscr: constructors) {
            JLabel label = new JLabel(cnstrDscr.name);
            classLabelMap.put(cnstrDscr.name.toLowerCase(), label);
            label.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    displayConstructorDescription(cnstrDscr);
                    selectConstructor(cnstrDscr);
                }

                @Override
                public void mousePressed(MouseEvent e) {}

                @Override
                public void mouseReleased(MouseEvent e) {}

                @Override
                public void mouseEntered(MouseEvent e) {
                    displayConstructorDescription(cnstrDscr);
                }

                @Override
                public void mouseExited(MouseEvent e) {}
            });
            label.setVisible(false);
            classListPanel.add(label);
        }

        classDescriptionTextArea = new JosmTextArea(10, 16);
        classDescriptionTextArea.setWrapStyleWord(true);
        classDescriptionTextArea.setLineWrap(true);;
        classDescriptionTextArea.setEditable(false);
        classDescriptionTextArea.setBackground(classListPanel.getBackground());
        JScrollPane classDescriptionScrollPane = new JScrollPane(classDescriptionTextArea);

        finderExpressionTextArea = new ParsedTextArea("", rows, cols, parser);
        finderExpressionTextArea.setWrapStyleWord(true);
        finderExpressionTextArea.setLineWrap(true);;
        JScrollPane finderExpressionScrollPane = new JScrollPane(finderExpressionTextArea);
        finderExpressionTextArea.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                String text = finderExpressionTextArea.getText();
                int position = finderExpressionTextArea.getCaretPosition();
                String identifier = getIdentifierAtPosition(text, position).toLowerCase();
                if (constructorNameMap.containsKey(identifier)) {
                    displayConstructorDescription(constructorNameMap.get(identifier));
                }
                parser.parse(text.substring(0, getBeginingOfIdentifierAtPosition(text, position)));
                if (parser.isFullyParsed()) {
                    HashSet<String> completionList = new HashSet<>(
                            parser.getCompletionList().stream().map((s) -> s.toLowerCase()).collect(Collectors.toList()));
                    for (String name: classLabelMap.keySet()) {
                        classLabelMap.get(name).setVisible(completionList.contains(name));
                    }
                }
            }
        });
        this.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorRemoved(AncestorEvent event) {
                finderExpressionTextArea.hideStatusMessage();
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
                finderExpressionTextArea.hideStatusMessage();
            }

            @Override
            public void ancestorAdded(AncestorEvent event) {
                finderExpressionTextArea.hideStatusMessage();
            }
        });
        this.addComponentListener(new ComponentListener() {
            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentResized(ComponentEvent e) {
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                finderExpressionTextArea.hideStatusMessage();
            }
        });

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(classListPanelScrollPane)
                                .addComponent(classDescriptionScrollPane)
                                )
                        .addComponent(finderExpressionScrollPane)));
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(classListPanelScrollPane)
                        .addComponent(classDescriptionScrollPane))
                   .addComponent(finderExpressionScrollPane));
    }

    public T getEditedInstance() {
        return parser.parse(finderExpressionTextArea.getText(), true);
    }

    public boolean isEditionValid() {
        return parser.parse(finderExpressionTextArea.getText());
    }

    public JTextArea getTextArea() {
        return finderExpressionTextArea;
    }

    private static String getIdentifierAtPosition(String text, int position) {
        int start = getBeginingOfIdentifierAtPosition(text, position);
        int end = position;
        while ((end < text.length()) && Character.isJavaIdentifierPart(text.charAt(end))) end++;
        return text.substring(start, end);
    }

    private static int getBeginingOfIdentifierAtPosition(String text, int position) {
        while ((position > 0) && Character.isJavaIdentifierPart(text.charAt(position-1))) position--;
        return position;
    }

    private void displayConstructorDescription(InstanceConstructor cnstrDscr) {
        classDescriptionTextArea.setText(
                cnstrDscr.name + "\n\n"
                + cnstrDscr.description
                + constructorSignature(cnstrDscr));
    }

    private void selectConstructor(InstanceConstructor cnstrDscr) {
        String text = cnstrDscr.name;
        if (cnstrDscr.paramsDescriptipon.length > 0) {
            text = text + "(  )";
        }
        finderExpressionTextArea.replaceSelection(text);
        if (cnstrDscr.paramsDescriptipon.length > 0) {
            finderExpressionTextArea.setCaretPosition(finderExpressionTextArea.getCaretPosition()-2);
        }
    }

    private String constructorSignature(InstanceConstructor c) {
        StringBuilder sb = new StringBuilder();
        if (c.paramsDescriptipon.length > 0) {
            if ((c.paramsDescriptipon.length > 1) || (c.varrgsTypes != null)) {
                sb.append("\n\nArguments:\n");
            } else {
                sb.append("\n\nArgument:\n");
            }
            for (String argDesc: c.paramsDescriptipon) {
                sb.append(" - " + argDesc + "\n");
            }
        }
        return sb.toString();
    }

}

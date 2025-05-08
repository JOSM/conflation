package com.vividsolutions.jcs.plugin.conflate.polygonmatch;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JTextField;

import com.vividsolutions.jump.workbench.ui.ValidatingTextField;

/**
 * Prevents the user from entering invalid data.
 * @deprecated Use ValidatingTextField instead
 */
@Deprecated
public class MyValidatingTextField extends ValidatingTextField {

    public MyValidatingTextField(
        String text,
        int columns,
        Validator validator,
        String emptyStringReplacement) {
        this(text, columns, validator, emptyStringReplacement, JTextField.RIGHT);
    }

    public MyValidatingTextField(
        String text,
        int columns,
        Validator validator,
        final String emptyStringReplacement,
        int alignment) {
        super(text, columns, validator);
        setHorizontalAlignment(alignment);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (getText().trim().length() == 0) {
                    setText(emptyStringReplacement);
                }
            }
        });
        //JUMP 1.0.0 bug workaround [Jon Aquino]
        setText(text);
    }

    public static final Validator NON_NEGATIVE_DOUBLE_VALIDATOR =
        new ValidatingTextField.Validator() {
        @Override
        public boolean isValid(String text) {
            if (text.length() == 0) {
                return true;
            }
            try {
                //Surround with zeros to allow user to enter something like "1E5"
                //(before she's hit "5"). [Jon Aquino]
                return Double.parseDouble("0" + text.trim() + "0") >= 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    };

    @Override
    public double getDouble() {
        try {
            return Double.parseDouble(getText().trim());
        } catch (NumberFormatException e1) {
            try {
                //e.g. "E5" [Jon Aquino]
                return Double.parseDouble("0" + getText().trim());
            } catch (NumberFormatException e2) {
                try {
                    //e.g. "5E" [Jon Aquino]
                    return Double.parseDouble(getText().trim() + "0");
                } catch (NumberFormatException e3) {
                    //e.g. "E" [Jon Aquino]
                    return Double.parseDouble("0" + getText().trim() + "0");
                }
            }
        }
    }

    public static final Validator NON_NEGATIVE_INTEGER_VALIDATOR =
        new ValidatingTextField.Validator() {
        @Override
        public boolean isValid(String text) {
            if (text.length() == 0) {
                return true;
            }
            try {
                return Integer.parseInt(text.trim()) >= 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    };

    public static class CompositeValidator implements Validator {
        private Validator[] validators;
        public CompositeValidator(Validator[] validators) {
            this.validators = validators;
        }
        @Override
        public boolean isValid(String text) {
            for (int i = 0; i < validators.length; i++) {
                if (!validators[i].isValid(text)) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class GreaterThanValidator implements Validator {
        private double threshold;
        public GreaterThanValidator(double threshold) {
            this.threshold = threshold;
        }
        @Override
        public boolean isValid(String text) {
            return text.trim().length() == 0
                || Double.parseDouble(text.trim()) > threshold;
        }
    }

}

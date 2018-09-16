// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.config.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parse a String representing the instantiation of an object using
 * a given potential list of constructors.
 */
class InstanceParser<M> implements IParser {

    private final Class<M> mainType;
    private final String mainDescription;
    private String text;
    private int pos;
    private int tokenPos;
    private boolean instantiate;
    private String description;
    private String error;
    private boolean valid;
    private List<String> completionList = new ArrayList<>();

    final InstanceConstructor[] constructors;
    final HashMap<String, InstanceConstructor> constructorNameMap = new HashMap<>();

    static final Pattern INTEGER_PATTERN = Pattern.compile("^[+-]?[0-9]+");
    static final Pattern FLOAT_PATTERN = Pattern.compile("^[+-]?[0-9]+(\\.[0-9]*)?([Ee][+-]?[0-9]+)?");
    static final Pattern BOOLEAN_PATTERN = Pattern.compile("^(true)|(false)", Pattern.CASE_INSENSITIVE);
    static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^([a-zA-Z][a-zA-Z0-9_]*)?");
    static final Pattern STRING_PATTERN = Pattern.compile("(\"(?:[^\"\\\\]|\\\\.)*\")|('(?:[^'\\\\]|\\\\.)*')");

    InstanceParser(Class<M> mainType, String mainDescription, InstanceConstructor[] constructors) {
        this.mainType = mainType;
        this.mainDescription = mainDescription;
        this.constructors = constructors;
        for (InstanceConstructor cnstr: constructors) {
            constructorNameMap.put(cnstr.name.toLowerCase(), cnstr);
        }
    }

    @Override
    public boolean parse(String text) {
        parse(text, false);
        //System.out.println(toString());
        return isValid();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("InstanceParser(" + mainType.getSimpleName() + ")\n");
        sb.append("text:    '" + text + "'\n");
        sb.append("tokenPos: ");
        for (int i = 0; i < tokenPos; i++) sb.append(" ");
        sb.append("|\n");
        sb.append("pos:      ");
        for (int i = 0; i < pos; i++) sb.append(" ");
        sb.append("|\n");
        sb.append("length: " + text.length() + "\n");
        sb.append("error: " + error + "\n");
        sb.append("valid: " + valid + "\n");
        sb.append("description: " + description + "\n");
        sb.append("completion: " + String.join(", ", completionList) + "\n");
        return sb.toString();
    }

    public M parse(String text, boolean instantiate) {
        this.text = text;
        this.pos = 0;
        this.tokenPos = 0;
        this.instantiate = instantiate;
        this.description = null;
        this.error = null;
        this.valid = false;
        M result = null;
        try {
            result = parse(mainType, true, mainDescription);
            if (nextChar() == 0) {
                this.valid = true;
            } else {
                this.error = "Unexpected trailing text:" + text.substring(pos);
            }
        } catch (Throwable t) {
            this.error = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
            if (instantiate) {
                throw t;
            }
        }
        return result;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public boolean isFullyParsed() {
        return pos >= text.length();
    }

    @Override
    public int getParsedIndex() {
        return pos;
    }

    @Override
    public int getLastTokenIndex() {
        return tokenPos;
    }

    @Override
    public String getLastTokenDescription() {
        return description;
    }

    @Override
    public String getErrorMessage() {
        return error;
    }

    @Override
    public List<String> getCompletionList() {
        return new ArrayList<>(completionList);
    }

    @SuppressWarnings(value = "unchecked")
    private <T> T parse(Class<T> type, boolean lastArg, String description) {
        this.completionList.clear();
        if (Byte.TYPE.isAssignableFrom(type) || Byte.class.isAssignableFrom(type)) {
            return (T) Byte.valueOf(findToken(INTEGER_PATTERN, description, "Integer number"));
        } else if (Short.TYPE.isAssignableFrom(type) || Short.class.isAssignableFrom(type)) {
            return (T) Short.valueOf(findToken(INTEGER_PATTERN, description, "Integer number"));
        } else if (Integer.TYPE.isAssignableFrom(type) || Integer.class.isAssignableFrom(type)) {
            return (T) Integer.valueOf(findToken(INTEGER_PATTERN, description, "Integer number"));
        } else if (Long.TYPE.isAssignableFrom(type) || Long.class.isAssignableFrom(type)) {
            return (T) Long.valueOf(findToken(INTEGER_PATTERN, description, "Integer number"));
        } else if (Float.TYPE.isAssignableFrom(type) || Float.class.isAssignableFrom(type)) {
            return (T) Float.valueOf(findToken(FLOAT_PATTERN, description, "Floating-point number"));
        } else if (Double.TYPE.isAssignableFrom(type) || Double.class.isAssignableFrom(type)) {
            return (T) Double.valueOf(findToken(FLOAT_PATTERN, description, "Floating-point number"));
        } else if (Boolean.TYPE.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type)) {
            completionList.add("true");
            completionList.add("false");
            return (T) Boolean.valueOf(findToken(BOOLEAN_PATTERN, description, "True or false"));
        } else if (Character.TYPE.isAssignableFrom(type) || Character.class.isAssignableFrom(type)) {
            String s = findToken(STRING_PATTERN, description, "Quoted char");
            //TODO: correctly unescaping the string
            s = s.replaceAll("\\\\" + s.charAt(0), s.substring(0, 1));
            if (s.length() != 3) {
                throw new Error("A single character was exepcted");
            }
            return (T) Character.valueOf(s.charAt(1));
        } else if (String.class.isAssignableFrom(type)) {
            String s = findToken(STRING_PATTERN, description, "Quoted string");
            //TODO: correctly unescaping the string
            s = s.replaceAll("\\\\" + s.charAt(0), s.substring(0, 1));
            return (T) s.substring(1, s.length()-1);
        } else if (type.isPrimitive()) {
            throw new UnsupportedOperationException("Unssuported primitive type " + type);
        } else if (type.isArray()) {
            if (lastArg && nextChar() != '[') {
                T result = (T) parseArray(type.getComponentType(), description);
                completionList.add(",");
                return result;
            } else {
                expect('[');
                T result = (T) parseArray(type.getComponentType(), description);
                completionList.add(",");
                expect(']');
                return result;
            }
        } else {
            return parseConstructor(type, description);
        }
    }

    @SuppressWarnings(value = "unchecked")
    private <T> T parseConstructor(Class<T> type, String description) {
        String identifier = findToken(IDENTIFIER_PATTERN, description, getName(type));
        if (isFullyParsed()) {
            for (InstanceConstructor cnstr: constructorNameMap.values()) {
                if (cnstr.name.toLowerCase().startsWith(identifier.toLowerCase())
                        && type.isAssignableFrom(cnstr.type))
                    completionList.add(cnstr.name);
            }
        }
        InstanceConstructor cnstr = constructorNameMap.get(identifier.toLowerCase());
        if (cnstr == null) {
            if (identifier.length() > 0) {
                throw new Error("Class not found: " + identifier);
            } else {
                throw new Error("Missing " + getName(type));
            }
        }
        if (!type.isAssignableFrom(cnstr.type))
            throw new Error("Class " + identifier + " is not a " + getName(type));
        Class<?>[] paramTypes = cnstr.constructor.getParameterTypes();
        Object[] params = new Object[paramTypes.length];
        if ((paramTypes.length > 0) || (nextChar() == '(')) {
            expect('(');
            for (int i = 0; i < paramTypes.length; i++) {
                if (i > 0) {
                    expect(',');
                }
                if ((i == paramTypes.length-1) && paramTypes[i].isArray() && (cnstr.varrgsTypes != null)) {
                    params[i] = parseArray(paramTypes[i].getComponentType(), cnstr.varrgsTypes, cnstr.varArgsDescriptions);
                } else {
                    params[i] = parse(paramTypes[i], i+1 == paramTypes.length, cnstr.paramsDescriptipon[i]);
                }
            }
            expect(')');
        }
        try {
            if (instantiate) {
                return (T) cnstr.constructor.newInstance(params);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private Object[] parseArray(Class<?> type, String description) {
        return parseArray(type, new Class<?>[] {type}, new String[]{description});
    }

    private Object[] parseArray(Class<?> type, Class<?>[] types, String[] descriptions) {
        ArrayList<Object> list = new ArrayList<>();
        char c = nextChar();
        boolean first = true;
        while (first || (c != ')' && c != ']' && c != 0)) {
            for (int i = 0; i < types.length; i++) {
                if (!first) {
                    expect(',');
                }
                first = false;
                list.add(parse(types[i], false, descriptions[i]));
            }
            c = nextChar();
        }
        return list.toArray((Object[]) java.lang.reflect.Array.newInstance(type, list.size()));
    }

    private void expect(char c) {
        if (nextChar() != c) {
            if (nextChar() == 0) {
                completionList.add("" + c);
            }
            throw new Error("Expecting " + c);
        }
        completionList.clear();
        pos = pos + 1;
    }

    private char nextChar() {
        skipSpaces();
        return (pos < text.length()) ? text.charAt(pos) : 0;
    }

    private String findToken(Pattern p, String description, String defaultDescription) {
        skipSpaces();
        this.description = ((description == null) || description.isEmpty()) ? defaultDescription : description;
        Matcher m = p.matcher(text.substring(pos));
        if (m.find() && m.start() == 0) {
            pos = pos + m.end();
            return m.group();
        }
        throw new Error("Invalid " + defaultDescription);
    }

    private boolean skipSpaces() {
        boolean result = false;
        while ((pos < text.length()) && Character.isWhitespace(text.charAt(pos))) {
            result = true;
            pos++;
        }
        tokenPos = pos;
        return result;
    }

    private String getName(Class<?> c) {
        for (InstanceConstructor cnstrDscr: constructorNameMap.values()) {
            if (cnstrDscr.type == c) {
                return cnstrDscr.name;
            }
        }
        return c.getSimpleName();
    }

}

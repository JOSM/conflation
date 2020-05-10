// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.config.parser;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Description of a class constructor.
 * Actually only the constructor with me largest number of arguments
 * is used.
 */
public class InstanceConstructor {

    /**
     * The type being constructed.
     */
    public final Class<?> type;

    /**
     * The name to use;
     */
    public final String name;


    /**
     * The constructor used.
     * The constructor with me largest number of arguments is used.
     */
    public final Constructor<?> constructor;

    /**
     * Description
     */
    public final String description;

    /**
     * Constructor parameters descriptions.
     */
    public final String[] paramsDescriptipon;

    /**
     * Types of the variable arguments (last Object[] parameter) if they must be forced.
     */
    public final Class<?>[] varrgsTypes;

    /**
     * Descriptions of the variable arguments (last Object[] parameter) if they must be forced.
     */
    public final String[] varArgsDescriptions;

    /**
     * Create an InstarnceConstructor description.
     * @param type The type being constructed
     * @param description Description
     * @param paramsDescriptipon Constructor parameters descriptions.
     */
    public InstanceConstructor(
            Class<?> type,
            String name,
            String description,
            String[] paramsDescriptipon) {
        this(type, name, description, paramsDescriptipon, null, null);
    }

    /**
     * Create an InstarnceConstructor description.
     * @param type The type being constructed
     * @param description Description
     * @param paramsDescriptipon Constructor parameters descriptions.
     * @param varArgsTypes Types of the variable arguments (last Object[] parameter) if they must be forced.
     * @param varArgsDescriptions Descriptions of the variable arguments (last Object[] parameter) if they must be forced.
     */
    public InstanceConstructor(
            Class<?> type,
            String name,
            String description,
            String[] paramsDescriptipon,
            Class<?>[] varArgsTypes,
            String[] varArgsDescriptions) {
        this.type = type;
        this.name = (name != null) ? name : type.getSimpleName();
        this.constructor = getLargestPublicConstructor(type);
        this.description = description;
        this.paramsDescriptipon = paramsDescriptipon;
        this.varrgsTypes = varArgsTypes;
        this.varArgsDescriptions = varArgsDescriptions;
        if (constructor == null)
            throw new IllegalArgumentException("No Public constructor found");
        if (constructor.getParameterCount() != paramsDescriptipon.length)
            throw new IllegalArgumentException(type.getSimpleName() + " paramDescription.length is != " + constructor.getParameterCount());
        if (varArgsTypes != null) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            assert (paramTypes[paramTypes.length -1].isArray());
        }

    }

    /**
     * Return the class' public constructor with the largest number of arguments.
     */
    private Constructor<?> getLargestPublicConstructor(Class<?> type) {
        Constructor<?> bestCnstr = null;
        for (Constructor<?> cnstr : type.getConstructors()) {
            if (Modifier.isPublic(cnstr.getModifiers())
                    && ((bestCnstr == null)
                        || (cnstr.getParameterTypes().length > bestCnstr.getParameterTypes().length)))
                bestCnstr = cnstr;
        }
        return bestCnstr;
    }
}

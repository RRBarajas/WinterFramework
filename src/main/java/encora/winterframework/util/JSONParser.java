package encora.winterframework.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// https://www.baeldung.com/java-reflection
// https://www.oracle.com/technical-resources/articles/java/javareflection.html
public class JSONParser {

    private static final int MAX_NESTED_CLASSES = 10;

    private static int currentNestedClasses;

    private static final Set<Class<?>> WRAPPER_TYPES;

    static {
        WRAPPER_TYPES = new HashSet<>(10);
        WRAPPER_TYPES.add(Byte.class);
        WRAPPER_TYPES.add(Short.class);
        WRAPPER_TYPES.add(Integer.class);
        WRAPPER_TYPES.add(Long.class);
        WRAPPER_TYPES.add(Float.class);
        WRAPPER_TYPES.add(Double.class);
        WRAPPER_TYPES.add(Character.class);
        WRAPPER_TYPES.add(Boolean.class);
        WRAPPER_TYPES.add(String.class);
        WRAPPER_TYPES.add(Date.class);
    }

    /**
     * Convert an object to JSON value
     *
     * @param o The object to convert
     * @return JSON representation of the object
     */
    public static <T> String toJSON(T o) throws IllegalAccessException {
        if (o == null) {
            return null;
        }
        Class<?> oClass = o.getClass();
        if (WRAPPER_TYPES.contains(oClass)) {
            return formatCommonTypes(o, oClass);
        } else if (oClass.isArray()) {
            int arrayLength = Array.getLength(o);
            ArrayList<Object> arrayValues = new ArrayList<>(arrayLength);
            for (int i = 0; i < arrayLength; i++) {
                arrayValues.add(toJSON(Array.get(o, i)));
            }
            return arrayValues.toString();
        }

        // Validation to identify possible infinite recursion cases
        if (++currentNestedClasses > MAX_NESTED_CLASSES) {
            throw new RuntimeException(
                String.format("Possible recursion. Found %d nested classes in object '%s'.", MAX_NESTED_CLASSES, oClass));
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        for (Field field : oClass.getDeclaredFields()) {
            field.setAccessible(true);
            String fieldName = String.format("\"%s\"", field.getName());
            Object fieldValue = toJSON(field.get(o));
            json.append(String.format("%s: %s, ", fieldName, fieldValue));
        }
        json.replace(json.length() - 2, json.length(), "}");
        currentNestedClasses--;
        return json.toString();
    }

    /**
     * Convert a JSON into an object of the specified class
     *
     * @param s        The JSON to convert into object
     * @param theClass The class of the desired object
     * @return Object representation of the JSON
     */
    public static <T> T toObject(String s, Class<T> theClass)
        throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchFieldException {
        if (s == null) {
            return null;
        } else if (WRAPPER_TYPES.contains(theClass)) {
            return theClass.cast(s);
        }
        if ((s.startsWith("{") && s.endsWith("}"))
        ) {
            T instance = theClass.getConstructor().newInstance();
            for (String fieldPairs : tokenizeByCharacter(s.substring(1, s.length() - 1), ',', 0)) {
                List<String> pairs = tokenizeByCharacter(fieldPairs, ':', 2);

                String fieldName = cleanDoubleQuotes(pairs.get(0), String.class);
                assert fieldName != null;
                Field actualField = theClass.getDeclaredField(fieldName);
                actualField.setAccessible(true);
                String fieldValue = cleanDoubleQuotes(pairs.get(1), actualField.getType());
                if (!setPrimitiveValueToInstance(instance, actualField, fieldValue)) {
                    // Not a primitive or common type
                    if (fieldValue == null || !actualField.getType().isArray()) {
                        // Regular object
                        actualField.set(instance, toObject(fieldValue, actualField.getType()));
                    } else {
                        // Array
                        actualField.set(instance, toObjectArray(fieldValue, actualField.getType().getComponentType()));
                    }
                }
            }
            return instance;
        }
        throw new ClassFormatError();
    }

    /**
     * Convert a JSON into an array of the specified class
     *
     * @param s        The JSON to convert into array
     * @param theClass The class of the desired array
     * @return Object with the array representation of the JSON
     */
    @SuppressWarnings("unchecked")
    private static <T> T toObjectArray(String s, Class<T> theClass)
        throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        List<String> arrayTokens = tokenizeByCharacter(s.substring(1, s.length() - 1), ',', 0);
        Object arrayChildren = Array.newInstance(theClass, arrayTokens.size());
        for (int i = 0; i < arrayTokens.size(); i++) {
            String child = cleanDoubleQuotes(arrayTokens.get(i), theClass);
            if (child != null && !setPrimitiveValueToArray(arrayChildren, i, child)) {
                if (theClass.isArray()) {
                    Array.set(arrayChildren, i, toObjectArray(child, theClass.getComponentType()));
                } else {
                    Array.set(arrayChildren, i, toObject(child, theClass));
                }
            }
        }
        return (T) arrayChildren;
    }

    private static String formatCommonTypes(Object value, Class<?> type) {
        if (type.equals(String.class) || type.equals(Character.class)) {
            return String.format("\"%s\"", value);
        }
        return String.format("%s", value);
    }

    /**
     * w
     * Tokenize a String based on the (single) passed character
     * The tokens are limited to the passed number
     * If a zero or negative limit is passed, return al possible tokens
     * <p>
     * It does not tokenize by multiple characters, not by regex
     *
     * @param s     The string we want to tokenize
     * @param token The character used to tokenize the string
     * @param limit The limit of tokens to be returned
     * @return The list of parsed tokens
     */
    private static List<String> tokenizeByCharacter(String s, char token, int limit) {
        List<String> tokens = new ArrayList<>();
        boolean limitResults = limit > 0;
        boolean doubleQuote = false;
        boolean singleQuote = false;
        int inBracket = 0;
        int i = 0;
        for (int j = 0; j < s.length(); j++) {
            char c = s.charAt(j);
            if (c == '\"') {
                doubleQuote = !doubleQuote;
            } else if (c == '\'') {
                singleQuote = !singleQuote;
            } else if (c == '[' || c == '{') {
                inBracket++;
            } else if (c == ']' || c == '}') {
                inBracket--;
            } else if (c == token && !doubleQuote && !singleQuote && inBracket == 0) {
                if (!limitResults || tokens.size() < limit - 1) {
                    tokens.add(s.substring(i, j));
                    i = j + 1;
                } else {
                    tokens.add(s.substring(i));
                    i = s.length();
                    break;
                }
            }
        }

        // In case we haven't reached the token limit
        if (!limitResults || tokens.size() < limit) {
            tokens.add(s.substring(i));
        }
        return tokens;
    }

    /**
     * Clean the double (surrounding) quotes of a string
     *
     * @param s    String that we want to clean
     * @param type Type of the object represented by the string
     * @return Cleaned string
     */
    private static String cleanDoubleQuotes(String s, Class<?> type) {
        String trimmed = s.trim();
        if (trimmed.equals("null")) {
            return null;
        } else if (type.equals(String.class) || type.equals(Character.TYPE)) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    /**
     * Attempt to set a value into an array in case it's a primitive
     *
     * @param instance The object to set the value into to which we want to set the value
     * @param field    The field that we want to modify
     * @param value    The string representation of the value to set
     * @return Boolean indicating if we were able to set the value
     */
    private static boolean setPrimitiveValueToInstance(Object instance, Field field, String value) throws IllegalAccessException {
        Class<?> type = field.getType();
        if (type.equals(Byte.TYPE)) {
            field.setByte(instance, Byte.parseByte(value));
        } else if (type.equals(Short.TYPE)) {
            field.setShort(instance, Short.parseShort(value));
        } else if (type.equals(Integer.TYPE)) {
            field.setInt(instance, Integer.parseInt(value));
        } else if (type.equals(Long.TYPE)) {
            field.setLong(instance, Long.parseLong(value));
        } else if (type.equals(Float.TYPE)) {
            field.setFloat(instance, Float.parseFloat(value));
        } else if (type.equals(Double.TYPE)) {
            field.setDouble(instance, Double.parseDouble(value));
        } else if (type.equals(Character.TYPE)) {
            field.setChar(instance, value.charAt(0));
        } else if (type.equals(Boolean.TYPE)) {
            field.setBoolean(instance, Boolean.parseBoolean(value));
        } else {
            field.set(instance, null);
            return false;
        }
        return true;
    }

    /**
     * Attempt to set a primitive value into an array
     *
     * @param array The array to which we want to set the value
     * @param index The position to put the value into
     * @param value The string representation of the value to set
     * @return Boolean indicating if we were able to set the value
     */
    private static boolean setPrimitiveValueToArray(Object array, int index, String value) {
        Class<?> arrayType = array.getClass().getComponentType();
        if (arrayType.equals(Byte.TYPE)) {
            Array.setByte(array, index, Byte.parseByte(value));
        } else if (arrayType.equals(Short.TYPE)) {
            Array.setShort(array, index, Short.parseShort(value));
        } else if (arrayType.equals(Integer.TYPE)) {
            Array.setInt(array, index, Integer.parseInt(value));
        } else if (arrayType.equals(Long.TYPE)) {
            Array.setLong(array, index, Long.parseLong(value));
        } else if (arrayType.equals(Float.TYPE)) {
            Array.setFloat(array, index, Float.parseFloat(value));
        } else if (arrayType.equals(Double.TYPE)) {
            Array.setDouble(array, index, Double.parseDouble(value));
        } else if (arrayType.equals(Character.TYPE)) {
            Array.setChar(array, index, value.charAt(0));
        } else if (arrayType.equals(Boolean.TYPE)) {
            Array.setBoolean(array, index, Boolean.parseBoolean(value));
        } else {
            return false;
        }
        return true;
    }

}
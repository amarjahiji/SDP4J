package com.sdp4j.core.util;

import java.util.Collection;

public class CommonUtil {
    /**
     * Prevents instantiation of this utility class.
     */
    private CommonUtil() {
    }

    /**
     * Convert a string to snake_case.
     *
     * Trims the input, converts uppercase letters to lowercase and inserts underscores
     * before them (except at the start or when the previous character is already
     * '_'), and replaces whitespace and '-' with single underscores while avoiding
     * consecutive underscores. If the input is null or blank, it is returned
     * unchanged.
     *
     * @param input the string to convert; may be null or blank
     * @return the converted snake_case string, or the original input if it is null or blank
     */
    public static String toSnakeCase(String input) {
        if (!isValidString(input)) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        char[] chars = input.trim().toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (Character.isUpperCase(c)) {
                if (i > 0 && chars[i - 1] != '_') {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else if (Character.isWhitespace(c) || c == '-') {
                if (!result.isEmpty() && result.charAt(result.length() - 1) != '_') {
                    result.append('_');
                }
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Checks whether a collection is non-null and contains at least one element.
     *
     * @return `true` if the collection is non-null and not empty, `false` otherwise.
     */
    public static boolean isValidCollection (Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    /**
     * Checks whether a string is not null and contains at least one non-whitespace character.
     *
     * @return `true` if the string is not null and contains at least one non-whitespace character, `false` otherwise.
     */
    public static boolean isValidString (String str) {
        return str != null && !str.isBlank();
    }
}
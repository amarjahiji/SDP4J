package com.sdp4j.sq4j.validations;

import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.sq4j.metadata.EntityDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WhereClauseValidator {

    private static final Set<String> RESERVED_KEYWORDS = Set.of(
            "AND", "OR", "NOT", "IS", "NULL", "IN", "LIKE", "ILIKE", "BETWEEN",
            "TRUE", "FALSE", "ASC", "DESC", "ANY", "ALL", "EXISTS", "DISTINCT",
            "AS", "ON", "USING"
    );

    public PreparedWhereClause prepareNamed(String sqlFragment, Map<String, Object> namedValues, FromScope scope) {
        if (sqlFragment == null || sqlFragment.isBlank()) {
            throw new Sdp4jValidationException("SQL fragment must not be blank");
        }
        Map<String, Object> values = namedValues == null ? Map.of() : namedValues;
        StringBuilder rewrittenSql = new StringBuilder(sqlFragment.length());
        List<Object> flatBindings = new ArrayList<>();
        Set<String> boundNames = new LinkedHashSet<>();
        int i = 0;
        int length = sqlFragment.length();
        EntityDescriptor pendingQualifierDescriptor = null;
        boolean pendingPostgresCastType = false;

        while (i < length) {
            char c = sqlFragment.charAt(i);

            if (c == '\'') {
                int closingQuote = consumeStringLiteral(sqlFragment, i);
                rewrittenSql.append(sqlFragment, i, closingQuote + 1);
                i = closingQuote + 1;
                pendingQualifierDescriptor = null;
                continue;
            }

            if (c == ':' && i + 1 < length && sqlFragment.charAt(i + 1) == ':') {
                rewrittenSql.append("::");
                i += 2;
                pendingPostgresCastType = true;
                continue;
            }

            if (c == ':') {
                if (i + 1 >= length || !isIdentifierStart(sqlFragment.charAt(i + 1))) {
                    throw new Sdp4jValidationException(
                            "Malformed parameter placeholder at index " + i + " in: " + sqlFragment);
                }
                int endExclusive = consumeIdentifier(sqlFragment, i + 1);
                String name = sqlFragment.substring(i, endExclusive);
                if (!values.containsKey(name)) {
                    throw new Sdp4jValidationException(
                            "No value bound for parameter '" + name + "'. Add .set(\"" + name + "\", ...)");
                }
                Object value = values.get(name);
                if (value instanceof Collection<?> collection) {
                    appendCollectionPlaceholder(rewrittenSql, collection, flatBindings);
                } else {
                    rewrittenSql.append('?');
                    flatBindings.add(value);
                }
                boundNames.add(name);
                i = endExclusive;
                pendingQualifierDescriptor = null;
                continue;
            }

            if (c == '?') {
                throw new Sdp4jValidationException(
                        "Positional '?' placeholders are not supported — use named ':name' parameters");
            }

            if (isIdentifierStart(c)) {
                int endExclusive = consumeIdentifier(sqlFragment, i);
                String identifier = sqlFragment.substring(i, endExclusive);
                if (pendingPostgresCastType) {
                    rewrittenSql.append(identifier);
                    i = endExclusive;
                    pendingPostgresCastType = false;
                    pendingQualifierDescriptor = null;
                    continue;
                }
                int nextNonWhitespace = skipWhitespace(sqlFragment, endExclusive);
                boolean isFunctionCall = nextNonWhitespace < length && sqlFragment.charAt(nextNonWhitespace) == '(';
                boolean isTableQualifier = nextNonWhitespace < length && sqlFragment.charAt(nextNonWhitespace) == '.';
                boolean precededByDot = isPrecededByDot(sqlFragment, i);
                boolean isReservedKeyword = RESERVED_KEYWORDS.contains(identifier.toUpperCase());

                if (!isFunctionCall && !isReservedKeyword) {
                    if (isTableQualifier) {
                        pendingQualifierDescriptor = resolveQualifier(identifier, scope);
                    } else if (precededByDot && pendingQualifierDescriptor != null) {
                        validateColumnOnDescriptor(identifier, pendingQualifierDescriptor);
                        pendingQualifierDescriptor = null;
                    } else {
                        resolveBareColumn(identifier, scope);
                        pendingQualifierDescriptor = null;
                    }
                } else {
                    pendingQualifierDescriptor = null;
                }

                rewrittenSql.append(identifier);
                i = endExclusive;
                continue;
            }

            rewrittenSql.append(c);
            i++;
            if (!Character.isWhitespace(c) && c != '.') {
                pendingQualifierDescriptor = null;
            }
        }

        return new PreparedWhereClause(rewrittenSql.toString(), flatBindings, boundNames);
    }

    private EntityDescriptor resolveQualifier(String qualifier, FromScope scope) {
        if (!scope.hasQualifier(qualifier)) {
            throw new Sdp4jValidationException(
                    "Unknown table qualifier '" + qualifier + "' in SQL fragment. Known qualifiers: "
                            + scope.qualifiers());
        }
        return scope.descriptorFor(qualifier);
    }

    private void validateColumnOnDescriptor(String column, EntityDescriptor descriptor) {
        if (!descriptor.hasColumn(column)) {
            throw new Sdp4jValidationException(
                    "Unknown column '" + column + "' on table '" + descriptor.tableName() + "'");
        }
    }

    private void resolveBareColumn(String columnCandidate, FromScope scope) {
        List<String> matchingQualifiers = scope.qualifiersWhereColumnExists(columnCandidate);
        if (matchingQualifiers.isEmpty()) {
            throw new Sdp4jValidationException(
                    "Unknown column '" + columnCandidate + "' — not found on any table in scope: "
                            + scope.qualifiers());
        }
        if (matchingQualifiers.size() > 1) {
            throw new Sdp4jValidationException(
                    "Ambiguous column '" + columnCandidate + "' — exists on tables "
                            + matchingQualifiers + ". Qualify it (e.g. " + matchingQualifiers.getFirst()
                            + "." + columnCandidate + ").");
        }
    }

    private void appendCollectionPlaceholder(StringBuilder rewrittenSql, Collection<?> collection, List<Object> flatBindings) {
        if (collection.isEmpty()) {
            throw new Sdp4jValidationException(
                    "Collection value for a '?' placeholder must not be empty");
        }
        rewrittenSql.append('(');
        boolean first = true;
        for (Object element : collection) {
            if (!first) rewrittenSql.append(", ");
            rewrittenSql.append('?');
            flatBindings.add(element);
            first = false;
        }
        rewrittenSql.append(')');
    }

    private int consumeStringLiteral(String sql, int openingQuoteIndex) {
        int i = openingQuoteIndex + 1;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (c == '\'') {
                if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    i += 2;
                    continue;
                }
                return i;
            }
            i++;
        }
        throw new Sdp4jValidationException("Unterminated string literal in SQL fragment");
    }

    private int consumeIdentifier(String sql, int start) {
        int i = start;
        while (i < sql.length() && isIdentifierPart(sql.charAt(i))) {
            i++;
        }
        return i;
    }

    private int skipWhitespace(String sql, int start) {
        int i = start;
        while (i < sql.length() && Character.isWhitespace(sql.charAt(i))) {
            i++;
        }
        return i;
    }

    private boolean isPrecededByDot(String sql, int identifierStart) {
        int i = identifierStart - 1;
        while (i >= 0 && Character.isWhitespace(sql.charAt(i))) i--;
        return i >= 0 && sql.charAt(i) == '.';
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}

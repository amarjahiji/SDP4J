package com.sdp4j.sq4j.validation;

import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.sq4j.metadata.EntityDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class WhereClauseValidator {

    private static final Set<String> RESERVED_KEYWORDS = Set.of(
            "AND", "OR", "NOT", "IS", "NULL", "IN", "LIKE", "ILIKE", "BETWEEN",
            "TRUE", "FALSE", "ASC", "DESC", "ANY", "ALL", "EXISTS", "DISTINCT",
            "AS", "ON", "USING"
    );

    public PreparedWhereClause prepare(String sqlFragment, Object[] values, FromScope scope) {
        if (sqlFragment == null || sqlFragment.isBlank()) {
            throw new Sdp4jValidationException("SQL fragment must not be blank");
        }
        Object[] valueArray = values == null ? new Object[0] : values;
        StringBuilder rewrittenSql = new StringBuilder(sqlFragment.length());
        List<Object> flatBindings = new ArrayList<>();
        int valueCursor = 0;
        int i = 0;
        int length = sqlFragment.length();
        EntityDescriptor pendingQualifierDescriptor = null;

        while (i < length) {
            char c = sqlFragment.charAt(i);

            if (c == '\'') {
                int closingQuote = consumeStringLiteral(sqlFragment, i);
                rewrittenSql.append(sqlFragment, i, closingQuote + 1);
                i = closingQuote + 1;
                pendingQualifierDescriptor = null;
                continue;
            }

            if (c == '?') {
                if (valueCursor >= valueArray.length) {
                    throw new Sdp4jValidationException(
                            "SQL fragment has more '?' placeholders than provided values");
                }
                Object nextValue = valueArray[valueCursor++];
                if (nextValue instanceof Collection<?> collection) {
                    appendCollectionPlaceholder(rewrittenSql, collection, flatBindings);
                } else {
                    rewrittenSql.append('?');
                    flatBindings.add(nextValue);
                }
                i++;
                pendingQualifierDescriptor = null;
                continue;
            }

            if (isIdentifierStart(c)) {
                int endExclusive = consumeIdentifier(sqlFragment, i);
                String identifier = sqlFragment.substring(i, endExclusive);
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

        if (valueCursor < valueArray.length) {
            throw new Sdp4jValidationException(
                    "SQL fragment provided " + valueArray.length + " values but only "
                            + valueCursor + " '?' placeholders were found");
        }

        return new PreparedWhereClause(rewrittenSql.toString(), flatBindings);
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

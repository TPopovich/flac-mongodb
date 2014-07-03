package com.mongodb.flac;

/**
 * This class provides a basic implementation of <code>RedactionExpression<code>,
 * allowing for a <code>redactionExpression</code> <code>String</code> to be provided
 * along with a <code>securityFieldName</code> which corresponds to the document field name 
 * used for the security attributes/markings within a document.
 * 
 *  <p>The <code>redactionExpression</code> <code>String</code> that is provided should include
 *  2 <code>String.format()</code> placeholders (e.g. <code>%s</code>). The first should be
 *  a placeholder within the expression for the <code>securityFieldName</code>, and the second
 *  placeholder should be for the string encoded user {@link com.mongodb.flac.SecurityAttributes}. </p>
 *
 */
public class StringRedactExpression implements RedactExpression {
    
    private String redactExpression;
    private String securityFieldName;
    
    /**
     * 
     * @param securityFieldName      a field name, like sl , that we use in our documentation describing FLAC
     * @param redactExpression       for $redact phase of mongodb aggregation-pipeline
     */
    public StringRedactExpression(String securityFieldName, String redactExpression) {
        final int indexOf = redactExpression.indexOf("%s");
        if (-1 == indexOf) { throw new IllegalArgumentException("redactExpression should have 2 %s placeholders"); }
        if (-1 == (redactExpression.indexOf("%s", indexOf+1))) { throw new IllegalArgumentException("redactExpression should have 2 %s placeholders"); }
        this.redactExpression = redactExpression;
    }

    /**
     * Return the redaction expression <code>String</code> based on the
     * specified {@link com.mongodb.flac.SecurityAttributes}</code>.
     * 
     * @param securityAttributes         {@link com.mongodb.flac.SecurityAttributes}
     * @return the redaction expression <code>String</code>
     */
    public String getRedactExpression(SecurityAttributes securityAttributes) {
        String visibilityAttributesForUser = securityAttributes.encodeAttributes();
        if (visibilityAttributesForUser == null || visibilityAttributesForUser.trim().length() == 0) {
            visibilityAttributesForUser = "[ ]";
        }
        
        return String.format(redactExpression, securityFieldName, visibilityAttributesForUser);
    }

}

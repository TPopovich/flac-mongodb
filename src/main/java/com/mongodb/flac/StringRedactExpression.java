package com.mongodb.flac;

/**
 * This class provides a basic implementation of <code>RedactionExpression<code>,
 * allowing for a <code>redactionExpression</code> <code>String</code> to be provided
 * along with a <code>securityFieldName</code> which corresponds to the document field name 
 * used for the security attributes/markings within a document.
 * 
 *  The <code>redactionExpression</code> <code>String</code> that is provided should include
 *  2 <code>String.format()</code> placeholders (e.g. <code>%s</code>). The first should be
 *  a placeholder within the expression for the <code>securityFieldName</code>, and the second
 *  placeholder should be for the string encoded user <code>SecurityAttributes</code>.
 *
 */
public class StringRedactExpression implements RedactExpression {
    
    private String redactExpression;
    private String securityFieldName;
    
    /**
     * 
     * @param securityFieldName
     * @param redactExpression
     */
    public StringRedactExpression(String securityFieldName, String redactExpression) {
        this.redactExpression = redactExpression;
    }

    /**
     * Return the redaction expression <code>String</code> based on the
     * specified <code>SecurityAttributes</code>.
     * 
     * @param securityAttributes
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

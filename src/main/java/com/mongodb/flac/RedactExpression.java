package com.mongodb.flac;

/**
 * This interface encapsulates the redaction expression required for the
 * implementation of a specific security model. 
 *
 */
public interface RedactExpression {
    
    /**
     * Return the redaction expression <code>String</code> based on the
     * specified <code>SecurityAttributes</code>.
     * 
     * @param securityAttributes
     * @return the redaction expression <code>String</code>
     */
    public String getRedactExpression(SecurityAttributes securityAttributes);

}

package com.mongodb.flac;

public class StringRedactExpression implements RedactExpression {
    
    private String redactExpression;
    private String securityFieldName;
    
    public StringRedactExpression(String securityFieldName, String redactExpression) {
        this.redactExpression = redactExpression;
    }

    public String getRedactExpression(SecurityAttributes securityAttributes) {
        String visibilityAttributesForUser = securityAttributes.encodeAttributes();
        if (visibilityAttributesForUser == null || visibilityAttributesForUser.trim().length() == 0) {
            visibilityAttributesForUser = "[ ]";
        }
        
        return String.format(redactExpression, securityFieldName, visibilityAttributesForUser);
    }

}

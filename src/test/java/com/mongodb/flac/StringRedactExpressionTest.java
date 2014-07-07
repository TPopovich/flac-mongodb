package com.mongodb.flac;

import com.mongodb.flac.capco.CapcoSecurityAttributes;
import junit.framework.TestCase;
import org.junit.Ignore;
import org.junit.Test;

public class StringRedactExpressionTest extends TestCase {

    //    @Ignore
    //    @Test(expected = java.lang.IllegalArgumentException.class)
    //    public void testGetRedactExpressionFailsToHaveTwoPlaceholds() throws Exception {
    //        StringRedactExpression stringRedactExpression = new StringRedactExpression("sl", "");  // lacks 2 %s  placeholders
    //        final CapcoSecurityAttributes securityAttributes = new CapcoSecurityAttributes();
    //        securityAttributes.setClearance("TS");
    //        final String redactExpression = stringRedactExpression.getRedactExpression(securityAttributes);
    //        System.out.println(redactExpression + " has error we should never reach this line!");
    //    }

    @Test
    public void testGetRedactExpression() throws Exception {
        StringRedactExpression stringRedactExpression = new StringRedactExpression("sl", "a %s b %s");
        //                note that we have 2 %s placeholders
        final CapcoSecurityAttributes securityAttributes = new CapcoSecurityAttributes();
        securityAttributes.setClearance("TS");
        final String redactExpression = stringRedactExpression.getRedactExpression(securityAttributes);
        assertEquals("a sl b [ { c:\"TS\" }, { c:\"S\" }, { c:\"C\" }, { c:\"U\" } ]", redactExpression);
    }
}
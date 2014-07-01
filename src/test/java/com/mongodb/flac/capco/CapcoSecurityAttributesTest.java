package com.mongodb.flac.capco;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class CapcoSecurityAttributesTest extends TestCase {
    @Test
    public void testExpandVisibilityString() throws Exception {

        CapcoSecurityAttributes userSecurityAttributesMap = new CapcoSecurityAttributes();

        // (1) first:  try 1 string "TK"       => should generate: [ { sci:"TK" } ]
        userSecurityAttributesMap.setSci(Arrays.asList("TK"));
        Assert.assertEquals("[ { sci:\"TK\" } ]", userSecurityAttributesMap.encodeAttributes());

        // (2) then: replace that "sci" with the list format: try 2 list of string "TK", "SI"
        //  =>   should generate: [ { sci:"TK" }, { sci:"SI" } ]
        userSecurityAttributesMap.setSci(Arrays.asList("TK", "SI"));
        String actual = userSecurityAttributesMap.encodeAttributes();
        Assert.assertEquals(true, actual.contains("{ sci:\"TK\" }"));
        Assert.assertEquals(true, actual.contains("{ sci:\"SI\" }"));

        // (3) then:  try additionally add a c:X to the  2 list of string "TK", "SI"  =>   should generate: [ { sci:"TK" }, { sci:"SI" } ]
        userSecurityAttributesMap.setClearance("X");
        actual = userSecurityAttributesMap.encodeAttributes();
        Assert.assertEquals(true, actual.contains("{ sci:\"TK\" }"));
        Assert.assertEquals(true, actual.contains("{ sci:\"SI\" }"));
        Assert.assertEquals(true, actual.contains("c:\"X\""));
    }
}
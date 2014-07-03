package com.mongodb.flac;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class SecurityAttributesTest {

    @Test
    public void testExpandVisibilityStringNoAttrs() throws Exception {

        SecurityAttributes userSecurityAttributesMap = new SecurityAttributes();
        String attributes = userSecurityAttributesMap.encodeAttributes();
        Assert.assertEquals("[  ]", attributes);
    }

    @Test
    public void testExpandVisibilityStringStringArgOnly() throws Exception {
        SecurityAttributes userSecurityAttributesMap = new SecurityAttributes();

        // try 1 string "TK"   , not list version   => should generate: [ { sci:"TK" } ]
        userSecurityAttributesMap.put( "sci", Arrays.asList("TK"));
        Assert.assertEquals("[ { sci:\"TK\" } ]", userSecurityAttributesMap.encodeAttributes());
    }

    @Test
    public void testExpandVisibilityString() throws Exception {

        SecurityAttributes userSecurityAttributesMap = new SecurityAttributes();

        // (1) first:  try 1 string "TK"       => should generate: [ { sci:"TK" } ]
        userSecurityAttributesMap.put("sci", Arrays.asList("TK"));
        Assert.assertEquals("[ { sci:\"TK\" } ]", userSecurityAttributesMap.encodeAttributes());

        //userSecurityAttributesMap.put("", Arrays.asList("DE", "US"))
        // (2) then: replace that "sci" with the list format: try 2 list of string "TK", "SI"
        //  =>   should generate: [ { sci:"TK" }, { sci:"SI" } ]
        userSecurityAttributesMap.put("sci", Arrays.asList("TK", "SI"));
        String actual = userSecurityAttributesMap.encodeAttributes();
        Assert.assertEquals(true, actual.contains("{ sci:\"TK\" }"));
        Assert.assertEquals(true, actual.contains("{ sci:\"SI\" }"));

        // (3) then:  try additionally add a c:X to the  2 list of string "TK", "SI"  =>   should generate: [ { sci:"TK" }, { sci:"SI" } ]
        userSecurityAttributesMap.put("c", "X");
        actual = userSecurityAttributesMap.encodeAttributes();
        Assert.assertEquals(true, actual.contains("{ sci:\"TK\" }"));
        Assert.assertEquals(true, actual.contains("{ sci:\"SI\" }"));
        Assert.assertEquals(true, actual.contains("c:\"X\""));
    }
}
package com.mongodb.flac;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

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


    private void compareListsOrderNotImportant(List<String> e, List<String> actual) {
        Assert.assertEquals(new HashSet(e), new HashSet(actual));
    }

    private void compareListsOrderNotImportant(String oneElement, List<String> actual) {
        Assert.assertEquals(new HashSet(Arrays.asList(oneElement)), new HashSet(actual));
    }
}
package com.mongodb.flac;

import com.mongodb.flac.UserSecurityAttributesMap;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

public class UserSecurityAttributesMapTest {

    @Test
    public void testExpandVisibilityStringNoAttrs() throws Exception {

        UserSecurityAttributesMap userSecurityAttributesMap = new UserSecurityAttributesMap();
        String attributes = userSecurityAttributesMap.encodeFlacSecurityAttributes();
        Assert.assertEquals("[  ]", attributes);
    }

    @Test
    public void testExpandVisibilityStringStringArgOnly() throws Exception {
        UserSecurityAttributesMap userSecurityAttributesMap = new UserSecurityAttributesMap();

        // try 1 string "TK"   , not list version   => should generate: [ { sci:"TK" } ]
        userSecurityAttributesMap.put( "sci", Arrays.asList("TK"));
        Assert.assertEquals("[ { sci:\"TK\" } ]", userSecurityAttributesMap.encodeFlacSecurityAttributes());
    }

    @Test
    public void testExpandVisibilityString() throws Exception {

        UserSecurityAttributesMap userSecurityAttributesMap = new UserSecurityAttributesMap();

        // (1) first:  try 1 string "TK"       => should generate: [ { sci:"TK" } ]
        userSecurityAttributesMap.put("sci", Arrays.asList("TK"));
        Assert.assertEquals("[ { sci:\"TK\" } ]", userSecurityAttributesMap.encodeFlacSecurityAttributes());

        //userSecurityAttributesMap.put("", Arrays.asList("DE", "US"))
        // (2) then: replace that "sci" with the list format: try 2 list of string "TK", "SI"
        //  =>   should generate: [ { sci:"TK" }, { sci:"SI" } ]
        userSecurityAttributesMap.put("sci", Arrays.asList("TK", "SI"));
        String actual = userSecurityAttributesMap.encodeFlacSecurityAttributes();
        Assert.assertEquals(true, actual.contains("{ sci:\"TK\" }"));
        Assert.assertEquals(true, actual.contains("{ sci:\"SI\" }"));

        // (3) then:  try additionally add a c:X to the  2 list of string "TK", "SI"  =>   should generate: [ { sci:"TK" }, { sci:"SI" } ]
        userSecurityAttributesMap.put("c", "X");
        actual = userSecurityAttributesMap.encodeFlacSecurityAttributes();
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
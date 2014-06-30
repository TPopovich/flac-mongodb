package com.mongodb.flac.capco;

import com.mongodb.flac.UserSecurityAttributesMap;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class UserSecurityAttributesMapCapcoTest extends TestCase {
    @Test
    public void testExpandVisibilityString() throws Exception {

        UserSecurityAttributesMapCapco userSecurityAttributesMap = new UserSecurityAttributesMapCapco();

        // (1) first:  try 1 string "TK"       => should generate: [ { sci:"TK" } ]
        userSecurityAttributesMap.setSci(Arrays.asList("TK"));
        Assert.assertEquals("[ { sci:\"TK\" } ]", userSecurityAttributesMap.encodeFlacSecurityAttributes());

        // (2) then: replace that "sci" with the list format: try 2 list of string "TK", "SI"
        //  =>   should generate: [ { sci:"TK" }, { sci:"SI" } ]
        userSecurityAttributesMap.setSci(Arrays.asList("TK", "SI"));
        String actual = userSecurityAttributesMap.encodeFlacSecurityAttributes();
        Assert.assertEquals(true, actual.contains("{ sci:\"TK\" }"));
        Assert.assertEquals(true, actual.contains("{ sci:\"SI\" }"));

        // (3) then:  try additionally add a c:X to the  2 list of string "TK", "SI"  =>   should generate: [ { sci:"TK" }, { sci:"SI" } ]
        userSecurityAttributesMap.setClearance("X");
        actual = userSecurityAttributesMap.encodeFlacSecurityAttributes();
        Assert.assertEquals(true, actual.contains("{ sci:\"TK\" }"));
        Assert.assertEquals(true, actual.contains("{ sci:\"SI\" }"));
        Assert.assertEquals(true, actual.contains("c:\"X\""));
    }
}
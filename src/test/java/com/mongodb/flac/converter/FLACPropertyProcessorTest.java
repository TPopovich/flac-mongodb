package com.mongodb.flac.converter;

import com.mongodb.flac.converter.FLACPropertyProvider;
import com.mongodb.flac.converter.FLACAnnotationException;
import com.mongodb.flac.converter.FLACProperty;
import com.mongodb.flac.converter.FLACPropertyProcessor;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class FLACPropertyProcessorTest {


    private static class TestClass1 implements FLACPropertyProvider {
        private String clearance;
        private List<String> sci;
        private List<String> citizenship;



        private TestClass1(String clearance, List<String> sci, List<String> citizenship) {
            this.clearance = clearance;
            this.sci = sci;
            this.citizenship = citizenship;
        }

        @FLACProperty(attributeNameInSl = "c")
        public String getClearance() {
            return clearance;
        }

        public void setClearance(String clearance) {
            this.clearance = clearance;
        }

        @FLACProperty(attributeNameInSl = "sci")
        public List<String> getSci() {
            return sci;
        }

        public void setSci(List<String> sci) {
            this.sci = sci;
        }

        @FLACProperty(attributeNameInSl = "citizenship")
        public List<String> getCitizenship() {
            return citizenship;
        }

        public void setCitizenship(List<String> citizenship) {
            this.citizenship = citizenship;
        }
    }

    @Test
    public void testFLACPropertyProcessor() throws FLACAnnotationException {
        final TestClass1 cValue = new TestClass1("c_sl_value", Arrays.asList("TK"), Arrays.asList("US"));
        Map<String, Object> actual = FLACPropertyProcessor.findMethodsAnnotatedPullOutSLFieldInfo(cValue);
        Assert.assertEquals(3, actual.size());

        Assert.assertEquals("c_sl_value", actual.get("c"));
        Assert.assertEquals(Arrays.asList("US"), actual.get("citizenship"));
        Assert.assertEquals(Arrays.asList("TK"), actual.get("sci"));


    }

    private static class TestRepr1 {

        public TestRepr1() throws Exception {
            byte[] hello = "hello\n\tworld\n\n\t".getBytes();
            System.out.println(new String(hexToByte(stringToHex(hello).replaceAll("0a", "5c6e")
                    .replaceAll("09", "5c74"))));
        }

        public static void main(String[] args) throws Exception {
            new TestRepr1();
        }

        public static String stringToHex(byte[] b) throws Exception {
            String result = "";
            for (int i = 0; i < b.length; i++) {
                result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            }
            return result;
        }

        public static byte[] hexToByte(String s) {
            int len = s.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
            }
            return data;
        }
    }


    @Test
    public void testRepr2() {


        try {
            ( new TestRepr1()).main( new String[]{});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
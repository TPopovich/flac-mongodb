package com.mongodb.flac;


import java.util.*;

/**
 * UserSecurityAttributesMap describes the User Security attributes for the user.
 *
 * @see com.mongodb.flac.capco.UserSecurityAttributesMapCapco   for a CAPCO specific subclass
 */
public class UserSecurityAttributesMap extends HashMap<String, Object> {

    public UserSecurityAttributesMap() {
    }

    public UserSecurityAttributesMap(Map<? extends String, ?> map) {
        super(map);
    }


    //    The following VARARG style constructor is commented out, but you might find it useful.
    //    /**
    //     * An easy inline way to create UserSecurityAttributes Mapping:  e.g.
    //     * <p><tt>
    //     *      new UserSecurityAttributesMap(
    //     *           "c", "TS",
    //     *           "sci", Arrays.asList( "TK", "SI", "G", "HCS"),
    //     *           "relto", Arrays.asList( "US"));
    //     * </tt></p>
    //     *
    //     * @param key1               first key of some user attribute, e.g. "c", sort for clearance
    //     * @param value1             first value of some user attribute, e.g. "TS"
    //     * @param keyValuePairs      vararg style key/value pairs that you can specify - see above sample code
    //     */
    //    public UserSecurityAttributesMap(String key1, Object value1, Object... keyValuePairs) {
    //        super(createAttributeMap(key1, value1, keyValuePairs));
    //    }
    //
    //    private static Map<String, Object> createAttributeMap(String key1, Object value1, Object[] keyValuePairs) {
    //        Map<String, Object> map = new HashMap<String, Object>();
    //
    //        if (key1 == null) throw new IllegalArgumentException("key1 must not be null");
    //        if (value1 != null) map.put(key1, value1);
    //        for (int i = 0; i<keyValuePairs.length; i += 2) {
    //            if (value1 != null) {
    //                final String valuePairKey = (String) keyValuePairs[i];
    //                if (valuePairKey == null) throw new IllegalArgumentException("key for any value must not be null");
    //                final Object valuePairValue = keyValuePairs[i + 1];
    //                if (valuePairValue != null) map.put(valuePairKey, valuePairValue);
    //            }
    //
    //        }
    //        return map;
    //    }


    /**
     * Convert the list of security attributes into a FLAC encoded string in canonical format.
     *
     * <p>Specifically look at the key/value pairs stored in this Map.  And then
     * convert a java list of simple strings like: key:value, e.g. "c:TS" (from our long running sample
     * application we have been discussing in the documentation) into an appropriate canonical VisibilityString.</p>m
     *
     * <p> The specific list of security attributes is system dependend, below we will describe
     *     and Capco like Visibility Strings to make a concrete example and also point out that
     *     you need to expand the setting if
     *     a setting , below see, c:TS, has domain meaning to be a superset of
     *     other lower level security settings. E.g. in CAPCO TS also implies
     *     that you have S C U.
     *
     *     <br/>  To make it easy to do this conversion we have a hook to install any custom logic, see
     *     method: com.mongodb.flac.UserSecurityAttributesMap#expandVisibilityString(java.lang.String)
     *
     * </p>
     *
     *
     *
     * </tt>
     * <h3>See Examples in the test code for more details.</h3>
     *
     * @param
     * @return    canonical user Flac Security Strings defined by the map that might look like e.g.
     * [ { c:\"TS\" }, { c:\"S\" }, { c:\"U\" }, { c:\"C\" }, { sci:\"TK\" }, { sci:\"SI\" }, { sci:\"G\" }, { sci:\"HCS\" } ]
     */
    public String encodeFlacSecurityAttributes() {

        StringBuilder stringBuilder = new StringBuilder();

        final HashSet<String> secAttrSetFormattedKeyValue = new LinkedHashSet<String>();

        boolean first = true;
        for (String key : this.keySet()) {
            final Object obj = this.get(key);
            List<String> valList = null;
            if (obj instanceof List) {
                valList = (List<String>)obj;
            } else {
                valList = Arrays.asList( (String) obj );
            }
            for (String val : valList) {
                if (val != null) {
                    val = val.trim();
                    final String formattedKeyValue = String.format("%s:%s", key, val);  // generates a term like c:TS
                    final List<String> userVisibilityStrings = expandVisibilityString(formattedKeyValue);
                    secAttrSetFormattedKeyValue.addAll(userVisibilityStrings);
                }
            }
        }
        // Now that we have all terms, format into a list
        stringBuilder.append("[ ");
        for (String val : secAttrSetFormattedKeyValue) {

            final String[] splitTerms = val.split(":");
            final String formattedKeyValue = String.format("{ %s:\"%s\" }", splitTerms[0], splitTerms[1]);  // generates a term like { c:"TS" } from  "c:TS"

            if (!first) {
                stringBuilder.append(", ");
            }
            first = false;
            stringBuilder.append(formattedKeyValue);
        }
        stringBuilder.append(" ]");

        return stringBuilder.toString();

    }

    /**
     * THIS IS A EXTENSION POINT TO HANDLE CUSTOM SECURITY HIERARCHIES.  The default is to just return the
     * argument without any interpretation.
     *
     * <p/>
     * This will encode Flac Security attribute as needed. THIS NEEDS TO FULLY EXPAND ANY IMPLIED ATTRIBUTES,
     * as by default we simply use the attribute as is, if you need to  EXPAND you need to override this method.
     * @param userAttrValue    an encoded value like "c:TS"
     * @return List of expanded encoded Flac Security attributes, e.g. for a DOD system you might need to expand
     *         c:TS into the list {  c:TS , c:S, c:C, c:U } etc
     */
    protected List<String> expandVisibilityString(final String userAttrValue) {
        return Arrays.asList(userAttrValue);

    }

}


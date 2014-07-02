package com.mongodb.flac;


import java.util.*;

/**
 * SecurityAttributes describes the User Security attributes for the user.
 *
 * @see com.mongodb.flac.capco.CapcoSecurityAttributes   for a CAPCO specific subclass
 */
public class SecurityAttributes extends HashMap<String, Object> {

    public SecurityAttributes() {
    }

    public SecurityAttributes(Map<? extends String, ?> map) {
        super(map);
    }


    /**
     * Convert the list of security attributes stored in the map into a FLAC encoded string in canonical format.
     * The goal is a java List of simple strings like: 'c:"TS"'  formed from the Map of key/value pairs
     * and placed into encoded string in canonical format that is
     * appropriate for a user's VisibilityString used in the $redact stage of aggregate.
     *
     * <p>Specifically look at the key/value pairs stored in this Map.  And then
     * convert a java list of simple strings like: key:value, e.g. "c:TS" (from our long running sample
     * application we have been discussing in the documentation) into an appropriate canonical VisibilityString.</p>m
     *
     * <p> The specific list of security attributes is system dependent, below we will describe
     *     and Capco like Visibility Strings to make a concrete example and also point out that
     *     you need to expand the setting if
     *     a setting , below see, c:TS, has domain meaning to be a superset of
     *     other lower level security settings. E.g. in CAPCO TS also implies
     *     that you have S C U.
     *
     * </p>
     *
     * </tt>
     * <h3>See Examples in the test code for more details.</h3>
     *
     * @param
     * @return    canonical user Flac Security Strings defined by the map that might look like e.g.
     * [ { c:\"TS\" }, { c:\"S\" }, { c:\"U\" }, { c:\"C\" }, { sci:\"TK\" }, { sci:\"SI\" }, { sci:\"G\" }, { sci:\"HCS\" } ]
     */
    public String encodeAttributes() {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("[ ");

        boolean first = true;
        for (String key : this.keySet()) {
            final Object obj = this.get(key);
            List<String> valList = null;
            if (obj instanceof List) {        // we allow items to be either a String or List<String>
                valList = (List<String>)obj;
            } else {
                valList = Arrays.asList( (String) obj );
            }
            for (String val : valList) {
                if (val != null) {
                    val = val.trim();
                    final String formattedKeyValue = String.format("{ %s:\"%s\" }", key, val); // generates a term like { c:"TS" }

                    if (!first) {
                        stringBuilder.append(", ");
                    }
                    first = false;
                    stringBuilder.append(formattedKeyValue);

                }
            }
        }

        stringBuilder.append(" ]");

        return stringBuilder.toString();
    }

}


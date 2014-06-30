package com.mongodb.flac.capco;

import com.mongodb.flac.converter.FLACPropertyProvider;

import java.util.*;

/**
 * UserSecurityAttributesMap for Capco,  describes the User Security attributes for the user.
 *
 * <p> This is a UserSecurityAttributesMapCapco class that implements CAPCO behavior such
 * as clearance TS also means a person with TS also inherits clearances (S, C, and U). </p>
 *
 * <p>For more info on CAPCO see http://fas.org/sgp/othergov/intel/capco_reg.pdf </p>
 *
 */
public class UserSecurityAttributesMapCapco extends com.mongodb.flac.UserSecurityAttributesMap {
    public UserSecurityAttributesMapCapco(int i, float v) {
        super(i, v);
    }

    public UserSecurityAttributesMapCapco(int i) {
        super(i);
    }

    public UserSecurityAttributesMapCapco() {
    }

    public UserSecurityAttributesMapCapco(Map<? extends String, ?> map) {
        super(map);
    }


    /**
     * An easy inline way to create UserSecurityAttributes Mapping:  e.g.
     * <p><tt>
     *      new UserSecurityAttributesMapCapco(
     *           "c", "TS",
     *           "sci", Arrays.asList( "TK", "SI", "G", "HCS"),
     *           "relto", Arrays.asList("US"));
     * </tt></p>
     *
     * @param key1               first key of some user attribute, e.g. "c", sort for clearance
     * @param value1             first value of some user attribute, e.g. "TS"
     * @param keyValuePairs      vararg style key/value pairs that you can specify - see above sample code
     */
    public UserSecurityAttributesMapCapco(String key1, Object value1, Object... keyValuePairs) {
        super(key1, value1, keyValuePairs);
    }

    private String clearance;
    private List<String> sci;
    private List<String> citizenship;

    public String getClearance() {
        return clearance;
    }

    public void setClearance(String clearance) {
        this.clearance = clearance;
        this.put("c", clearance) ;
    }

    public List<String> getSci() {
        return sci;
    }

    public void setSci(List<String> sci) {
        this.sci = sci;
        this.put("sci", sci) ;
    }

    public List<String> getCitizenship() {
        return citizenship;
    }

    public void setCitizenship(List<String> citizenship) {
        this.citizenship = citizenship;
        this.put("citizenship", citizenship) ;
    }

    /**
     * Convert java List of simple strings like: "c:TS"  into an appropriate CapcoVisibilityString.
     * </tt>
     * <p> <b>See Examples below for more details:</b>
     * </p>
     * <p>
     * <tt>
     * UserSecurityAttributes.EncodingUtils.expandCapcoVisibility(new String[]{"c:TS", "c:S"})
     * note here we deal with S being contained in TS
     * </tt>
     * generates:
     * <br/>
     * <tt>
     * "[ { c:\"TS\" }, { c:\"S\" }, { c:\"C\" }, { c:\"U\" } ]"
     * </tt>
     * </p>
     * <p>
     * <tt>
     * UserSecurityAttributes.EncodingUtils.expandCapcoVisibility(new String[]{"c:TS",  "sci:TK",  "sci:SI",  "sci:G",  "sci:HCS"})
     * </tt>
     * generates:
     * <br/>
     * <tt>
     * "[ { c:\"TS\" }, { c:\"S\" }, { c:\"U\" }, { c:\"C\" }, { sci:\"TK\" }, { sci:\"SI\" }, { sci:\"G\" }, { sci:\"HCS\" } ]";
     * </tt>
     * </p>
     * <p/>
     * <p> NOTES: we fully support generating lower level of TS S C and U  , for all others you need to expand yourself.</p>
     *
     * @param
     * @return    user Flac Security Strings defined by the map
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
     * encode Flac Security attribute as needed.  By default we simply use the attribute as is, if you need to
     * @param userAttrValue    an encoded value like "c:TS"
     * @return List of expanded encoded Flac Security attributes, e.g. for a DOD system you might need to expand
     *         c:TS into the list {  c:TS , c:S, c:C, c:U } etc
     */
    protected List<String> expandVisibilityString(final String userAttrValue) {
        return CapcoVisibilityUtil.recusivelyExpandCapcoVisibility(userAttrValue);

    }

}


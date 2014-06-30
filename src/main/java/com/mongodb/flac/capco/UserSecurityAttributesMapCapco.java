package com.mongodb.flac.capco;

import com.mongodb.flac.converter.FLACPropertyProvider;

import java.util.*;

/**
 * UserSecurityAttributesMap for Capco,  describes the User Security attributes for the user.
 *
 * <p> This is a UserSecurityAttributesMap class that implements CAPCO behavior such
 * as clearance TS also means a person with TS also inherits clearances (S, C, and U). </p>
 *
 * <p> It also has getter/setters for the CAPCO attributes of: clearances, sci, and citizenship.</p>
 *
 * <p>For more info on CAPCO see http://fas.org/sgp/othergov/intel/capco_reg.pdf </p>
 *
 * @see com.mongodb.flac.UserSecurityAttributesMap
 *
 */
public class UserSecurityAttributesMapCapco extends com.mongodb.flac.UserSecurityAttributesMap {


    public UserSecurityAttributesMapCapco() {
    }

    public UserSecurityAttributesMapCapco(Map<? extends String, ?> map) {
        super(map);
    }



    public String getClearance() {
        return (String) this.get("c");
    }

    public void setClearance(String clearance) {
        this.put("c", clearance) ;
    }

    public List<String> getSci() {
        return (List<String>) this.get("sci");
    }

    public void setSci(List<String> sci) {
        this.put("sci", sci) ;
    }

    public List<String> getCitizenship() { return (List<String>) this.get("citizenship");}

    public void setCitizenship(List<String> citizenship) {
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

        // the super class has a plugin call to expandVisibilityString()  so we do not need any changes to the
        // superclass's method.  All our changes are i
        return super.encodeFlacSecurityAttributes();

    }

    /**
     * CAPCO specific encode function for Flac Security attribute. THIS NEEDS TO FULLY EXPAND ANY IMPLIED ATTRIBUTES,
     * as by default we simply use the attribute as is, if you need to  EXPAND you need to override this method.
     * @param userAttrValue    an encoded value like "c:TS"
     * @return List of expanded encoded Flac Security attributes, e.g. for a DOD system you might need to expand
     *         c:TS into the list {  c:TS , c:S, c:C, c:U } etc
     */
    @Override
    protected List<String> expandVisibilityString(final String userAttrValue) {
        return CapcoVisibilityUtil.recusivelyExpandCapcoVisibility(userAttrValue);

    }

}


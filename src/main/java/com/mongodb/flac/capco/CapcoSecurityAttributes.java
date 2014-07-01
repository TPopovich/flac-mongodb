package com.mongodb.flac.capco;


import com.mongodb.flac.SecurityAttributes;

import java.util.*;

/**
 * SecurityAttributes for Capco,  describes the User Security attributes for the user.
 *
 * <p> This is a SecurityAttributes class that implements CAPCO behavior such
 * as clearance TS also means a person with TS also inherits clearances (S, C, and U). </p>
 *
 * <p> It also has getter/setters for the CAPCO attributes of: clearances, sci, and citizenship.</p>
 *
 * <p>For more info on CAPCO see http://fas.org/sgp/othergov/intel/capco_reg.pdf </p>
 *
 * @see com.mongodb.flac.SecurityAttributes
 *
 */
public class CapcoSecurityAttributes extends SecurityAttributes {


    public CapcoSecurityAttributes() {
    }

    public CapcoSecurityAttributes(Map<? extends String, ?> map) {
        super(map);
    }



    public List<String> getClearance() {
        return (List<String>) this.get("c");
    }

    /**
     * CAPCO specific Clearance levels have a nesting. THIS NEEDS TO FULLY EXPAND ANY IMPLIED ATTRIBUTES,
     * as by default we simply use the attribute as is, if you need to  EXPAND you need to override this method
     * and use your tuned CapcoSecurityAttributes class.
     *
     * <p> IMPLIED TS => S => C => U  is supported by this method, so if you provide a Clearance of TS we
     *     will generate <tt>TS , S , C , U </tt>  for you similarly
     *     if you specify S we generate <tt> S , C , U </tt>  etc
     * </p>
     *
     * @param clearance    an value like "TS"     (for top secret)
     */
    public void setClearance(String clearance) {
        this.put("c", expandClearance(clearance) );
    }

    private List<String> expandClearance(String clearance) {

        final HashSet<String> capco = new LinkedHashSet<String>();

        if ("TS".equalsIgnoreCase(clearance)) {
            capco.add("TS");
            capco.add("S");
            capco.add("C");
            capco.add("U");
        } else if ("S".equalsIgnoreCase(clearance)) {
            capco.add("S");
            capco.add("C");
            capco.add("U");
        } else if ("C".equalsIgnoreCase(clearance)) {
            capco.add("C");
            capco.add("U");
        } else if ("U".equalsIgnoreCase(clearance)) {
            capco.add("U");
        } else {
            capco.add(clearance);
        }


        return new ArrayList<String>( capco );
    }

    public List<String> getSci() {
        return (List<String>) this.get("sci");
    }

    public void setSci(List<String> sci) {
        this.put("sci", sci) ;
    }

    public List<String> getCitizenship() { return (List<String>) this.get("relto");}

    /**
     * set the citizenship of the user, in relto capabilities. Note in CAPCO, e.g. US citizenship
     * maps to a bunch of different relto values including USA and NOFORN.  This method should
     * expand to appropriate values, similar to the setClearance above, as needed in your system.
     *
     * <p> <b>NOTE: this is not a complete implementation and does not expand citizenship values. </b>
     * @param citizenship
     */
    public void setCitizenship(List<String> citizenship) {
        this.put("relto", citizenship) ;     // TODO:  this is not a complete implementation ; the full expansion is not done.
    }

    /**
     * Convert java List of simple strings like: "c:TS"  formed from the Map of key/value pairs
     * into encoded string in canonical format,
     * and one appropriate for a Capco VisibilityString used in the $redact stage of aggregate.
     *
     * <p>Specifically look at the key/value pairs stored in this Map.  And then
     * convert a java list of simple strings like: key:value, e.g. "c:TS" (from our long running sample
     * application we have been discussing in the documentation) into an appropriate canonical VisibilityString.</p>
     *
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
    public String encodeAttributes() {

        // the super class encodeAttributes() will suffice for the CAPCO specific logic.  All the CAPCO specifics are in how
        // the values expands, e.g. TS =>  expands to TS S C U
        return super.encodeAttributes();

    }



}


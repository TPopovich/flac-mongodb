package com.mongodb.flac.capco;

import java.util.*;

/**
 * CapcoVisibilityString Utilities.
 *
 * <p>
 *    The goal of these utilities are to allow easy use of CAPCO specifications, and easy integration in other
 *    client software systems.  Our reference implementation needs a particular encoding of the CAPCO settings
 *    fully spelled out.
 *    Taking a simple case of TS or top-secret, a person with TS will have S C and U.  (The lower level of this
 *    reference implementation of the FLAC utility needs to have all capabilities spelled out
 *    in order to make database operations fast.)
 * </p>
 *
 * <p>
 *    By using the expandCapcoVisibility method you can pass in high level capabilities in a concise manner
 *    which might match what you have stored in other systems, that will then be properly recursively Expanded.
 * </p>
 *
 * <p>
 *    Overview:
 *    <br/> this makes it easy to encode security settings that you have in systems.
 *    Consider having to encode a Top Secret CAPCO setting, you can call this one call that will expand to
 *    lower levels as necessary:
 *    <tt>
 *        UserSecurityAttributes.EncodingUtils.expandCapcoVisibility(new String[]{"c:TS"}) =>
 *           That call in fact under the covers expands to or generates:
 *
 *        [ { c:"TS" }, { c:"S" }, { c:"U" }, { c:"C" } ]   (( in javascript notation which is needed by the
 *                                                             reference FLAC implementation))
 *    </tt>
 *
 *
 * </p>
 *
 * <p> <b>See Examples below for more details:</b>
 * </p>
 * <p>
 *     <tt>
 *     UserSecurityAttributes.EncodingUtils.expandCapcoVisibility(new String[]{"c:TS", "c:S"})
 *          note here we deal with S being contained in TS
 *     </tt>
 *     generates:
 *     <br/>
 *     <tt>
 *     "[ { c:\"TS\" }, { c:\"S\" }, { c:\"C\" }, { c:\"U\" } ]"
 *     </tt>
 * </p>
 * <p>
 *     <tt>
 *      UserSecurityAttributes.EncodingUtils.expandCapcoVisibility(new String[]{"c:TS",  "sci:TK",  "sci:SI",  "sci:G",  "sci:HCS"})
 *     </tt>
 *     generates:
 *     <br/>
 *     <tt>
 *      "[ { c:\"TS\" }, { c:\"S\" }, { c:\"U\" }, { c:\"C\" }, { sci:\"TK\" }, { sci:\"SI\" }, { sci:\"G\" }, { sci:\"HCS\" } ]";
 *     </tt>
 * </p>
 *
 * <p> The above long strings are an encoded javascript like format that is used by our REDAC engine to efficiently support FLAC.</p>
 *
 */
public class CapcoVisibilityUtil {

    /** Expand Capco Visibility, as c:TS (clearance of Top Secret) implies c:S (clearance of Secret) etc
     *
     * @param userCapcoVisibilityEncodedString specifies a user security encoded value, e.g. considering a sample
     *                                         military application, one that has only high level user CAPCO e.g.
     *                                         c:TS elements and may not have lower levels and if given c:TS, this
     *                                         would generate c:TS c:S c:C c:U  as a result
     *
     * @return  all user CAPCO entries with duplicates suppressed.
     */
    public static List<String> expandClassification(String userCapcoVisibilityEncodedString) {
        final List<String> userCapcoVisibilityStrings = Arrays.asList(userCapcoVisibilityEncodedString);
        return expandClassification(userCapcoVisibilityStrings);
    }

    /** Expand Capco Visibility, as c:TS (clearance of Top Secret) implies c:S (clearance of Secret) etc
     *
     * <p> This gives an concrete application where a user might specify a recursive model:
     *                                          a user security encoded value, e.g. considering a sample
     *                                         military application, one that has only high level user CAPCO e.g.
     *                                         c:TS elements and may not have lower levels and if given c:TS, this
     *                                         would generate c:TS c:S c:C c:U  as a result
     * @param userCapcoVisibilityEncodedStrings that has only high level user CAPCO e.g. c:TS elements and may not have
     *        lower levels
     * @return  all user CAPCO entries with duplicates suppressed.
     */
    public static List<String> expandClassification(List<String> userCapcoVisibilityEncodedStrings) {
        if (userCapcoVisibilityEncodedStrings == null) return null;

        final HashSet<String> capco = new LinkedHashSet<String>();

        for (String c : userCapcoVisibilityEncodedStrings) {

            if ("c:TS".equalsIgnoreCase(c)) {
                capco.add("c:TS");
                capco.add("c:S");
                capco.add("c:C");
                capco.add("c:U");
            } else if ("c:S".equalsIgnoreCase(c)) {
                capco.add("c:S");
                capco.add("c:C");
                capco.add("c:U");
            } else if ("c:C".equalsIgnoreCase(c)) {
                capco.add("c:C");
                capco.add("c:U");
            } else if ("c:U".equalsIgnoreCase(c)) {
                capco.add("c:U");
            } else {
                capco.add(c);
            }
            break;
        }

        return new ArrayList<String>( capco );
    }



}

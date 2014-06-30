package com.mongodb.flac;

/**
 * RedactedDBCollection Constants.
 *
 * <p> The method <tt> getSecurityExpression() </tt> will either fetch the default
 * mongodb $redact code needed to implement FLAC sufficient for CAPCO controls, or one
 * that you have redefined by calling setSecurityExpression(String).
 *
 * </p>
 */
public class RedactedDBCollectionConstants {
    private static String securityExpression;

    /**
     * get the SecurityExpression redact phase needed for mongodb to implement FLAC.
     *
     * <p> we will either fetch the default
     * mongodb $redact code needed to implement FLAC sufficient for CAPCO controls, or one
     * that you have redefined by calling setSecurityExpression(String) </p>
     * @return
     */
    public static String getSecurityExpression() {
        if (securityExpression == null) { securityExpression = getDefaultSecurityExpression(); }
        return securityExpression;
    }

    /**
     * override the default mongodb redact coded needed to implement FLAC, the default is
     * also stored in file securityExpression.json.
     *
     * @param securityExpression
     */
    public void setSecurityExpression(String securityExpression) {          // non static so users can use spring easily
        RedactedDBCollectionConstants.securityExpression = securityExpression;
    }

    /** Note the default is
     * also stored in file securityExpression.json.  But having the code here allows this
     * source code to easily be lean.
     */
    private static String getDefaultSecurityExpression() {
        return "{\n" +
                "        $cond: {\n" +
                "            if: {\n" +
                "                $allElementsTrue : {\n" +
                "                    $map : {\n" +
                "                        input: {$ifNull:[\"$sl\",[[]]]},\n" +
                "                        \"as\" : \"setNeeded\",\n" +
                "                            \"in\" : {\n" +
                "                            $cond: {\n" +
                "                                if: {\n" +
                "                                    $or: [\n" +
                "                                        { $eq: [ { $size: \"$$setNeeded\" }, 0 ] },\n" +
                "                                        { $gt: [ { $size: { $setIntersection: [ \"$$setNeeded\", %s ] } }, 0 ] }\n" +
                "                                    ]\n" +
                "                                },\n" +
                "                                then: true,\n" +
                "                            else: false\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "                }\n" +
                "            },\n" +
                "            then: \"$$DESCEND\",\n" +
                "        else: \"$$PRUNE\"\n" +
                "        }\n" +
                "}\n";
    }
}

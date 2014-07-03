package com.mongodb.flac.capco;

import com.mongodb.flac.RedactExpression;
import com.mongodb.flac.StringRedactExpression;

/**
 * A <code>RedactExpression</code> implementation specific to CAPCO.
 *
 */
public class CapcoRedactExpression extends StringRedactExpression implements RedactExpression {
    
    public final static String CAPCO_REDACT_EXPRESSION = "{"
            + "  $cond: {"
            + "    if: {"
            + "      $allElementsTrue : {"
            + "        $map : {"
            + "          input: {$ifNull:[\"$%s\",[[]]]},"
            + "          \"as\" : \"setNeeded\","
            + "            \"in\" : {"
            + "            $cond: {"
            + "              if: {"
            + "                $or: ["
            + "                  { $eq: [ { $size: \"$$setNeeded\" }, 0 ] },"
            + "                  { $gt: [ { $size: { $setIntersection: [ \"$$setNeeded\", %s ] } }, 0 ] }"
            + "                ]"
            + "              },"
            + "              then: true,"
            + "              else: false"
            + "            }"
            + "          }"
            + "        }"
            + "      }"
            + "    },"
            + "    then: \"$$DESCEND\","
            + "    else: \"$$PRUNE\""
            + "  }"
            + "}";

    public CapcoRedactExpression(String securityFieldName) {
        super(securityFieldName, CAPCO_REDACT_EXPRESSION);
    }

}

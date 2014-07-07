package com.mongodb.flac.capco;

import com.mongodb.flac.RedactExpression;
import com.mongodb.flac.StringRedactExpression;

/**
 * A <code>RedactExpression</code> implementation specific to CAPCO.
 *
 * <p>This is designed to feed into the mongodb aggregation pipeline $redact
 *    stage.  It has 2 %s place holders, that wants the field name holding the
 *    document markings, and  the json-ified security settings for the current
 *    user.
 *    <br/>
 *    Since the below CapcoRedactExpression is long consider that it is just
 *    'a <b>%s</b> b <b>%s</b>'.
 *    <br/>Code later on will plug values into the %s, e.g. we might
 *    have <pre><tt>field-name=sl</tt></pre>
 *    and   <pre><tt>user-security=[ { c:"TS" }, { c:"S" }, { c:"C" }, { c:"U" } ]</tt></pre>
 *    and given that the final $redact expands to this:
 *
 *
 *      <pre><tt>a sl b [ { c:"TS" }, { c:"S" }, { c:"C" }, { c:"U" } ] </tt></pre>
 * </p>
 *
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

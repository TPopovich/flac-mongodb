package com.mongodb.flac;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class SecureAggregationPipelineTest extends TestCase {

    public void testAppendMatch() throws Exception {
        final String visibilityAttributesForUser = "";
        SecureAggregationPipeline secureAggregationPipeline = new SecureAggregationPipeline(visibilityAttributesForUser);

        assertEquals(1, secureAggregationPipeline.size());
    }

    public void testAppendQuery() throws Exception {

    }

    public void testAppendLimit() throws Exception {

    }

    public void testAppendSort() throws Exception {

    }

    public void testAppendSkip() throws Exception {

    }


    @Test
    public void testPrependSecurityRedactToPipeline() throws Exception {
        // should prepend a new
        final DBObject basicDBObject = new BasicDBObject();
        final DBObject basicDBObject2 = new BasicDBObject();
        final List<DBObject> pipeline = Arrays.asList(basicDBObject, basicDBObject2);
        final String visibilityAttributesForUser = "";
        final List<DBObject> actual = new SecureAggregationPipeline(pipeline, visibilityAttributesForUser);
        org.junit.Assert.assertEquals(3, actual.size());
        org.junit.Assert.assertNotSame(basicDBObject, actual.get(0)); // element 0 should be our "src/test/java/com/mongodb/securityExpression.json" content
        org.junit.Assert.assertEquals(basicDBObject2, actual.get(1));

    }

}
package com.mongodb.flac.capco;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.mongodb.util.TestCase;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.flac.RedactExpression;
import com.mongodb.flac.RedactedDBCollection;
import com.mongodb.util.JSON;

public class CapcoRedactTest {
    
    protected static final Logger logger = LoggerFactory.getLogger(CapcoRedactTest.class);

    private DBCollection collection;

    @Before
    public void setupData() throws IOException {
        File file = new ClassPathResource("capcoDocumentMarkingsExample.json").getFile();
        String json = FileUtils.readFileToString(file);
        MongoClient mongo = new MongoClient();
        DB db = mongo.getDB(TestCase.MONGO_DBNAME_FOR_TEST_DATA);
        collection = db.getCollection(this.getClass().getSimpleName());
        collection.drop();
        collection.insert((DBObject) JSON.parse(json));
    }

    private DBObject getFirstRedactedResult(CapcoSecurityAttributes userSecurityAttributes) {
        RedactExpression redactExpression = new CapcoRedactExpression("security");
        RedactedDBCollection redactedDBCollection = new RedactedDBCollection(collection, userSecurityAttributes,
                redactExpression);
        DBObject query = new BasicDBObject("year", 2014);
        Cursor cursor = redactedDBCollection.find(query);
        DBObject result = cursor.next();
        return result;
    }

    @Test
    public void testTS_SI() {

        CapcoSecurityAttributes userSecurityAttributes = new CapcoSecurityAttributes();
        userSecurityAttributes.setClearance("TS");
        userSecurityAttributes.setSci(Arrays.asList("SI"));

        DBObject result = getFirstRedactedResult(userSecurityAttributes);
        logger.debug(result.toString());

        assertNotNull(result);
        BasicDBList subsections = (BasicDBList) result.get("subsections");
        assertNotNull(subsections);
        assertEquals(2, subsections.size());
        String subtitle1 = (String) ((DBObject) subsections.get(0)).get("subtitle");
        assertEquals("Section 1: Overview", subtitle1);
        String subtitle2 = (String) ((DBObject) subsections.get(1)).get("subtitle");
        assertEquals("Section 2: Analysis", subtitle2);
    }

    @Test
    public void testTS_SI_TK() {

        CapcoSecurityAttributes userSecurityAttributes = new CapcoSecurityAttributes();
        userSecurityAttributes.setClearance("TS");
        userSecurityAttributes.setSci(Arrays.asList("SI", "TK"));

        DBObject result = getFirstRedactedResult(userSecurityAttributes);

        assertNotNull(result);
        BasicDBList subsections = (BasicDBList) result.get("subsections");
        assertNotNull(subsections);
        assertEquals(3, subsections.size());
        String subtitle1 = (String) ((DBObject) subsections.get(0)).get("subtitle");
        assertEquals("Section 1: Overview", subtitle1);
        String subtitle2 = (String) ((DBObject) subsections.get(1)).get("subtitle");
        assertEquals("Section 2: Analysis", subtitle2);
    }

    @Test
    public void testU() {

        CapcoSecurityAttributes userSecurityAttributes = new CapcoSecurityAttributes();
        userSecurityAttributes.setClearance("U");

        DBObject result = getFirstRedactedResult(userSecurityAttributes);

        assertNotNull(result);
        BasicDBList subsections = (BasicDBList) result.get("subsections");
        assertNotNull(subsections);
        assertEquals(1, subsections.size());
        String subtitle1 = (String) ((DBObject) subsections.get(0)).get("subtitle");
        assertEquals("Section 1: Overview", subtitle1);
    }

    @Test
    public void testNoSecurityAttributes() throws IOException {

        CapcoSecurityAttributes userSecurityAttributes = new CapcoSecurityAttributes();
        DBObject result = getFirstRedactedResult(userSecurityAttributes);

        assertNotNull(result);
        BasicDBList subsections = (BasicDBList) result.get("subsections");
        assertNotNull(subsections);
        assertEquals(0, subsections.size());
    }

}

package com.mongodb.flac;

import java.io.File;
import java.io.IOException;

import com.mongodb.util.TestCase;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.flac.capco.CapcoRedactExpression;
import com.mongodb.util.JSON;

public class RedactTest {

    private CapcoRedactExpression capcoRedactExpression = new CapcoRedactExpression("sl");
    
    private DBCollection collection;

    @Before
    public void setupData() throws IOException {
        File file = new ClassPathResource("securityMarkingsExample1.json").getFile();
        String json = FileUtils.readFileToString(file);
        MongoClient mongo = new MongoClient();
        DB db = mongo.getDB(TestCase.MONGO_DBNAME_FOR_TEST_DATA);
        collection = db.getCollection(this.getClass().getSimpleName());
        collection.drop();
        collection.insert((DBObject) JSON.parse(json));
    }
    
    @Test
    public void test() throws IOException {
        
        SecurityAttributes userSecurityAttributes = new SecurityAttributes();
        userSecurityAttributes.put("security", "low");
        RedactedDBCollection redactedDBCollection = new RedactedDBCollection(collection, userSecurityAttributes, capcoRedactExpression);
        DBObject query = new BasicDBObject("year", 2014);
        Cursor cursor = redactedDBCollection.find(query);
        DBObject result = cursor.next();
        System.out.println(result);
        

    }

}

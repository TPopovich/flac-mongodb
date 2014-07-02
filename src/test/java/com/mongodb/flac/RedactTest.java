package com.mongodb.flac;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.flac.capco.CapcoRedactExpression;
import com.mongodb.util.JSON;

public class RedactTest {

    private CapcoRedactExpression capcoRedactExpression = new CapcoRedactExpression("sl");
    
    @Test
    public void test() throws IOException {
        File file = new ClassPathResource("securityMarkingsExample1.json").getFile();
        String json = FileUtils.readFileToString(file);
        
        Mongo mongo = new Mongo();

        DB db = mongo.getDB("test");
        DBCollection collection =  db.getCollection(this.getClass().getSimpleName());
        collection.insert((DBObject)JSON.parse(json));
        
        SecurityAttributes userSecurityAttributes = new SecurityAttributes();
        userSecurityAttributes.put("security", "low");
        RedactedDBCollection redactedDBCollection = new RedactedDBCollection(collection, userSecurityAttributes, capcoRedactExpression);
        DBObject query = new BasicDBObject("year", 2014);
        Cursor cursor = redactedDBCollection.find(query);
        DBObject result = cursor.next();
        System.out.println(result);
        

    }

}

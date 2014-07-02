package com.mongodb.flac;

import com.mongodb.*;
import com.mongodb.util.TestCase;
import org.bson.types.BasicBSONList;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.UnknownHostException;
import java.util.*;

import static junit.framework.Assert.assertEquals;

public class SafeDBCollectionTest extends TestCase {

    @BeforeClass
    public static void classPresetConstants() throws FileNotFoundException {
        final String filename = "src/test/java/com/mongodb/flac/securityExpression.json";
        if (!(new File(filename).exists())) {
            throw new IllegalArgumentException("securityExpression.json file not properly setup: " + filename);
        }
        new com.mongodb.flac.RedactedDBCollectionConstants().setSecurityExpression(getSecurityExpressionFromFile(filename));

    }

    private static String getSecurityExpressionFromFile(final String filenameHoldingSecurityExpression) throws FileNotFoundException {

        final String content = new Scanner(new File(filenameHoldingSecurityExpression)).useDelimiter("\\Z").next();
        return content;

    }

    private static class DbCollectionHolder {

        public DB db;
        public DBCollection collection;

        public DbCollectionHolder(DB db, DBCollection collection) {
            this.db = db;
            this.collection = collection;
        }
    }

    private DbCollectionHolder initSimpleCollection(final String collectionName) throws UnknownHostException {

        Mongo mongo = new Mongo();

        DB db = mongo.getDB("test");
        DBCollection customersCollection = db.getCollection(collectionName);
        customersCollection.drop();

        DBObject address = new BasicDBObject("city", "NYC");
        address.put("street", "Broadway");

        DBObject addresses = new BasicDBObject();

        if (false) {
            BasicBSONList bsonList = new BasicBSONList();
            bsonList.putAll(addresses);
            addresses.putAll(bsonList);
        } else {
            addresses.putAll(address);
        }

        DBObject customer = new BasicDBObject("firstname", "Tom");
        customer.put("lastname", "Smith");
        customer.put("addresses", addresses);

        customersCollection.insert(customer);

        return new DbCollectionHolder(db, customersCollection);

    }

    @Test
    public void testFind() throws Exception {

    /*
     * @param userAttributes below is a Map of attributes, e.g.  clearance="TS", sci=[ "TK", "SI", "G", "HCS" ] etc
     *                            that provide the user's SecurityAttributes.  A find() on the safeDBCollection
     *                            will honor those user attributes.
     */

        DbCollectionHolder dbCollectionHolder = initSimpleCollection("ttt_customers");

        DB db = dbCollectionHolder.db;
        DBCollection customersCollection = dbCollectionHolder.collection;

        customersCollection.find();

        DBObject address = new BasicDBObject("city", "NYC");
        address.put("street", "Broadway");


        DBCollection persons = db.getCollection("persons");
        DBCollection wrappedDBCollection = persons;

        SecurityAttributes userAttributes = new SecurityAttributes();
        userAttributes.put("c", "TS");           // clearance is stored as c
        userAttributes.put("sci", Arrays.asList("TK", "SI", "G", "HCS"));
        userAttributes.put("relto", Arrays.asList("US"));      // countries is stored as relto internally

        RedactedDBCollection safeDBCollection = new RedactedDBCollection(wrappedDBCollection, userAttributes);
        // test RedactedDBCollection

        DBObject customerQuery = new BasicDBObject("firstname", "Tom");

        safeDBCollection.find(customerQuery);

    }


}
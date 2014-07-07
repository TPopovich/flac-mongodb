package com.mongodb.util;

import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import org.junit.Before;
import org.junit.BeforeClass;

import java.net.UnknownHostException;
import java.util.List;

import static org.junit.Assume.assumeTrue;

/**
 * Shared test code to get an initial clean DB.
 */
public class TestCase {

    public static final String DEFAULT_URI = "mongodb://localhost:27017";
    public static final String MONGODB_URI_SYSTEM_PROPERTY_NAME = "org.mongodb.test.uri";
    public static final String MONGO_DBNAME_FOR_TEST_DATA = "test3";
    private static MongoClientURI mongoClientURI;

    private static MongoClient staticMongoClient;
    private static final String cleanupDB = "mongo-java-flac-test";
    protected DBCollection collection;

    ;
    @BeforeClass
    public static void testCaseBeforeClass() {
        if (staticMongoClient == null) {
            try {
                staticMongoClient = new MongoClient(getMongoClientURI());
                staticMongoClient.dropDatabase(cleanupDB);
                Runtime.getRuntime().addShutdownHook(new ShutdownHook());
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Before
    public void testCaseBefore() {
        collection = getDatabase().getCollection(getClass().getName());
        collection.drop();
    }

    static class ShutdownHook extends Thread {
        @Override
        public void run() {
            if (staticMongoClient != null) {
                staticMongoClient.dropDatabase(cleanupDB);
                staticMongoClient.close();
                staticMongoClient = null;
            }
        }
    }

    protected static MongoClient getMongoClient() {
        return staticMongoClient;
    }

    protected static DB getDatabase() {
        return staticMongoClient.getDB(cleanupDB);
    }

    public static synchronized MongoClientURI getMongoClientURI() {
        if (mongoClientURI == null) {
            String mongoURIProperty = System.getProperty(MONGODB_URI_SYSTEM_PROPERTY_NAME);
            String mongoURIString = mongoURIProperty == null || mongoURIProperty.isEmpty() ? DEFAULT_URI : mongoURIProperty;
            mongoClientURI = new MongoClientURI(mongoURIString);
        }
        return mongoClientURI;
    }
}

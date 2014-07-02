package com.mongodb.flac;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.flac.capco.CapcoRedactExpression;
import com.mongodb.flac.capco.CapcoSecurityAttributes;
import com.mongodb.util.JSON;
import com.mongodb.util.TestCase;


public class RedactedDBCollectionTest extends TestCase {
    
    private CapcoRedactExpression capcoRedactExpression = new CapcoRedactExpression("sl");

    @BeforeClass
    public static void initCollectionInitConstants() throws Exception {
        DBCollection dbCollectionSrc = getDbCollectionUsedForTesting();
        dbCollectionSrc.drop();    // drop and recreate the pristine dbCollection for testing
        seedCollectionTwoSampleRecords(dbCollectionSrc);
    }

    @Before
    public void initCollectionInitPerTest() throws Exception {
        DBCollection dbCollectionSrc = getDbCollectionUsedForTesting();
        dbCollectionSrc.drop();    // drop and recreate the pristine dbCollection for testing
        seedCollectionTwoSampleRecords(dbCollectionSrc);
    }


    /** fetch the DBCollection used for testing RedactedDBCollection */
    private static DBCollection getDbCollectionUsedForTesting() throws UnknownHostException {
        Mongo mongo = new Mongo();

        DB db = mongo.getDB("test");
        return db.getCollection("person3");
    }


    // Test find( DBObject query , DBObject keys )
    @Test
    public void testFindTwoArgBasicDBObjectForThequery() throws Exception {

        final BasicDBObject query = new BasicDBObject();
        final BasicDBObject keys = new BasicDBObject();

        final DBCollection dbCollectionSrc = getDbCollectionUsedForTesting();
        final SecurityAttributes userSecurityAttributes = new SecurityAttributes();
        final RedactedDBCollection redactedDBCollection = new RedactedDBCollection(dbCollectionSrc, userSecurityAttributes, capcoRedactExpression);
        final BasicDBObject sort = new BasicDBObject("firstName", -1);
        final Cursor dbObjects = redactedDBCollection.find(query, keys, 0, 0, 0, 0, ReadPreference.primary(), sort);

        final boolean hasNext = dbObjects.hasNext();
        assertEquals(true, hasNext);
        final String expectedRec1 = "{ \"_id\" : \"5375052930040f83a06f115a\" , \"firstName\" : \"Sheldon\" , \"lastName\" : \"Humphrey\" , \"foo\" : \"bar\"}";

        final DBObject actual = dbObjects.next();
        compareJSON(expectedRec1, actual);

        final String expectedRec2 = "{ \"_id\" : \"5375052930040f83a06f1160\" , \"firstName\" : \"Alice\" , \"lastName\" : \"Fuentes\"}";
        compareJSON(expectedRec2, dbObjects.next());

        assertEquals(false, dbObjects.hasNext());
    }

    // Test find( DBObject query , DBObject keys )
    @Test
    public void testFindTwoArgNullForThequery() throws Exception {

        final BasicDBObject query = null;
        final BasicDBObject keys = new BasicDBObject();

        final DBCollection dbCollectionSrc = getDbCollectionUsedForTesting();
        final SecurityAttributes userSecurityAttributes = new SecurityAttributes();
        final RedactedDBCollection redactedDBCollection = new RedactedDBCollection(dbCollectionSrc, userSecurityAttributes, capcoRedactExpression);
        final Cursor dbObjects = redactedDBCollection.find(query, keys);

        final boolean hasNext = dbObjects.hasNext();
        assertEquals(true, hasNext);
        final String expectedRec1 = "{ \"_id\" : \"5375052930040f83a06f115a\" , \"firstName\" : \"Sheldon\" , \"lastName\" : \"Humphrey\" , \"foo\" : \"bar\"}";

        final DBObject actual = dbObjects.next();
        compareJSON(expectedRec1, actual);

        final String expectedRec2 = "{ \"_id\" : \"5375052930040f83a06f1160\" , \"firstName\" : \"Alice\" , \"lastName\" : \"Fuentes\"}";
        compareJSON(expectedRec2, dbObjects.next());

        assertEquals(false, dbObjects.hasNext());
    }

    // Test find( DBObject query , DBObject keys )
    @Test
    public void testFindTwoArgFindQuerySheldon() throws Exception {

        final BasicDBObject query = new BasicDBObject("firstName", "Sheldon");
        final BasicDBObject keys = new BasicDBObject();

        DBCollection dbCollectionSrc = getDbCollectionUsedForTesting();
        final SecurityAttributes userSecurityAttributes = new SecurityAttributes();

        RedactedDBCollection redactedDBCollection = new RedactedDBCollection(dbCollectionSrc, userSecurityAttributes, capcoRedactExpression);
        final Cursor dbObjects = redactedDBCollection.find(query, keys);

        final boolean hasNext = dbObjects.hasNext();
        assertEquals(true, hasNext);
        final String expectedRec1 = "{ \"_id\" : \"5375052930040f83a06f115a\" , \"firstName\" : \"Sheldon\" , \"lastName\" : \"Humphrey\" , \"foo\" : \"bar\"}";

        final DBObject actual = dbObjects.next();
        compareJSON(expectedRec1, actual);

        assertEquals(false, dbObjects.hasNext());
    }

    @Test
    public void testFindTwoDocs() throws Exception {

        final BasicDBObject query = new BasicDBObject("firstName", "Sheldon");
        final BasicDBObject keys = new BasicDBObject();

        DBCollection dbCollectionSrc = getDbCollectionUsedForTesting();
        // below we use the CapcoSecurityAttributes that knows how to do our application logic of
        // c:TS also maps to c:S,  c:C, and  c:U
        //
        // we also just use .put()  rather than the  setClearance() etc higher level methods to test
        // that they work, another test will stress .setClearance() like methods
        final CapcoSecurityAttributes userSecurityAttributes = new CapcoSecurityAttributes();
        userSecurityAttributes.setClearance( "TS" );
        userSecurityAttributes.put("sci", "TK");
        // same as with:
        RedactedDBCollection redactedDBCollection = new RedactedDBCollection(dbCollectionSrc, userSecurityAttributes, capcoRedactExpression);
        Cursor dbObjectsCursor = redactedDBCollection.find(query, keys);
        DBObject orderBy = new BasicDBObject("_id", 1) ;
        assertEquals(true, dbObjectsCursor.hasNext());
        //reopen the cursor
        dbObjectsCursor = redactedDBCollection.find(query, keys);
        DBObject actual = dbObjectsCursor.next();
        final String expectedRec1 = "{ \"_id\" : \"5375052930040f83a06f115a\" , \"firstName\" : \"Sheldon\" , \"lastName\" : \"Humphrey\" , \"favorites\" : { \"sl\" : [ [ { \"c\" : \"S\"}]] , \"cartoonCharacters\" : [ \"Diablo The Raven \" , \"Rabbit\" , \"bar\"]} , \"foo\" : \"bar\"}";

        compareJSON(expectedRec1, actual);

        assertEquals(false, dbObjectsCursor.hasNext());
    }

    @Test
    public void testFindTwoDocsCapcoSetters() throws Exception {

        final BasicDBObject query = new BasicDBObject("firstName", "Sheldon");
        final BasicDBObject keys = new BasicDBObject();

        DBCollection dbCollectionSrc = getDbCollectionUsedForTesting();
        // below we use the CapcoSecurityAttributes that knows how to do our application logic of
        // c:TS also maps to c:S,  c:C, and  c:U
        final CapcoSecurityAttributes userSecurityAttributes = new CapcoSecurityAttributes();
        // same as with:
        userSecurityAttributes.setClearance("TS");
        userSecurityAttributes.setSci(Arrays.asList("TK"));
        RedactedDBCollection redactedDBCollection = new RedactedDBCollection(dbCollectionSrc, userSecurityAttributes, capcoRedactExpression);
        Cursor dbObjectsCursor = redactedDBCollection.find(query, keys);
        DBObject orderBy = new BasicDBObject("_id", 1) ;
        assertEquals(true, dbObjectsCursor.hasNext());
        //reopen the cursor
        dbObjectsCursor = redactedDBCollection.find(query, keys);
        DBObject actual = dbObjectsCursor.next();
        final String expectedRec1 = "{ \"_id\" : \"5375052930040f83a06f115a\" , \"firstName\" : \"Sheldon\" , \"lastName\" : \"Humphrey\" , \"favorites\" : { \"sl\" : [ [ { \"c\" : \"S\"}]] , \"cartoonCharacters\" : [ \"Diablo The Raven \" , \"Rabbit\" , \"bar\"]} , \"foo\" : \"bar\"}";

        compareJSON(expectedRec1, actual);

        assertEquals(false, dbObjectsCursor.hasNext());
    }

    // Test find( DBObject query , DBObject keys )
    @Test
    public void testFindTwoArgFindQuerySheldonTSClearance() throws Exception {

        final BasicDBObject query = new BasicDBObject("firstName", "Sheldon");
        final BasicDBObject keys = new BasicDBObject();

        DBCollection dbCollectionSrc = getDbCollectionUsedForTesting();
        // below we use the CapcoSecurityAttributes that knows how to do our application logic of
        // c:TS also maps to c:S,  c:C, and  c:U
        final CapcoSecurityAttributes userSecurityAttributes = new CapcoSecurityAttributes();
        userSecurityAttributes.setClearance( "TS" );
        RedactedDBCollection redactedDBCollection = new RedactedDBCollection(dbCollectionSrc, userSecurityAttributes, capcoRedactExpression);
        final Cursor dbObjects = redactedDBCollection.find(query, keys);
        DBObject orderBy = new BasicDBObject("_id", 1) ;
        final Cursor c = redactedDBCollection.find(query, keys, 0, ReadPreference.primaryPreferred(), orderBy);
        assertEquals(true, c.hasNext());
        final boolean hasNext = dbObjects.hasNext();
        assertEquals(true, hasNext);
        final String expectedRec1 = "{ \"_id\" : \"5375052930040f83a06f115a\" , \"firstName\" : \"Sheldon\" , \"lastName\" : \"Humphrey\" , \"favorites\" : { \"sl\" : [ [ { \"c\" : \"S\"}]] , \"cartoonCharacters\" : [ \"Diablo The Raven \" , \"Rabbit\" , \"bar\"]} , \"foo\" : \"bar\"}";

        final DBObject actual = dbObjects.next();
        compareJSON(expectedRec1, actual);

    }

    @Test
    public void testFindTwoArgFindSheldonProjectJustFirstName() throws Exception {

        final BasicDBObject query = new BasicDBObject("firstName", "Sheldon");
        final BasicDBObject keys = new BasicDBObject("firstName", 1);

        DBCollection dbCollectionSrc = getDbCollectionUsedForTesting();
        final SecurityAttributes userSecurityAttributes = new SecurityAttributes();
        RedactedDBCollection redactedDBCollection = new RedactedDBCollection(dbCollectionSrc, userSecurityAttributes, capcoRedactExpression);
        final Cursor dbObjects = redactedDBCollection.find(query, keys);

        final boolean hasNext = dbObjects.hasNext();
        assertEquals(true, hasNext);
        final String expectedRec1 = "{ \"_id\" : \"5375052930040f83a06f115a\" , \"firstName\" : \"Sheldon\" }";

        final DBObject actual = dbObjects.next();
        compareJSON(expectedRec1, actual);

    }

    // Test find( DBObject query , DBObject keys )
    @Test
    public void testAggregate() throws Exception {

        final BasicDBObject query = new BasicDBObject();
        final BasicDBObject keys = new BasicDBObject();

        final DBCollection dbCollectionSrc = getDbCollectionUsedForTesting();
        final SecurityAttributes userSecurityAttributes = new SecurityAttributes();
        final RedactedDBCollection redactedDBCollection = new RedactedDBCollection(dbCollectionSrc, userSecurityAttributes, capcoRedactExpression);
        List<DBObject> pipelineForAggregate = new ArrayList<DBObject>();
        pipelineForAggregate.add(new BasicDBObject("$match",
                new BasicDBObject("firstName", "Sheldon")));
        final Cursor dbObjects = redactedDBCollection.aggregate(pipelineForAggregate);

        final boolean hasNext = dbObjects.hasNext();
        assertEquals(true, hasNext);
        final String expectedRec1 = "{ \"_id\" : \"5375052930040f83a06f115a\" , \"firstName\" : \"Sheldon\" , \"lastName\" : \"Humphrey\" , \"foo\" : \"bar\"}";

        final DBObject actual = dbObjects.next();
        compareJSON(expectedRec1, actual);

        assertEquals(false, dbObjects.hasNext());
    }

    private void compareJSON(String json1, String json2)  {

        final DBObject j1 = (DBObject) JSON.parse(json1);
        final DBObject j2 = (DBObject) JSON.parse(json2);

        final HashSet<String> hashSetJ1 = new HashSet<String>(j1.keySet());
        final HashSet<String> hashSetJ2 = new HashSet<String>(j2.keySet());
        assertEquals(hashSetJ1, hashSetJ2);
        for (String k : hashSetJ1) {
            assertEquals(j1.get(k), j2.get(k));
        }
    }

    private void compareJSON(String json1, DBObject dbObject)  {

        final DBObject j1 = (DBObject) JSON.parse(json1);
        final DBObject j2 = dbObject;

        final HashSet<String> hashSetJ1 = new HashSet<String>(j1.keySet());
        final HashSet<String> hashSetJ2 = new HashSet<String>(j2.keySet());
        assertEquals(hashSetJ1, hashSetJ2);
        for (String k : hashSetJ1) {
            assertEquals(j1.get(k), j2.get(k));
        }
    }

    // Test find( DBObject query , DBObject keys )
    @Test
    public void testFindTwoArgItCount() throws Exception {
        final BasicDBObject query = null;
        final BasicDBObject keys = new BasicDBObject();

        DBCollection dbCollectionSrc = getDbCollectionUsedForTesting();
        final SecurityAttributes userSecurityAttributes = new SecurityAttributes();
        RedactedDBCollection redactedDBCollection = new RedactedDBCollection(dbCollectionSrc, userSecurityAttributes, capcoRedactExpression);
        final Cursor dbObjects = redactedDBCollection.find(query, keys);

        final boolean hasNext = dbObjects.hasNext();
        assertEquals(true, hasNext);
        assertNotNull(dbObjects.next()); // should have 2 records  , as low level docs are protected only
        assertNotNull(dbObjects.next()); // should have 2 records  , as low level docs are protected only
    }


    @Test
    public void testFindOne() throws Exception {
        final BasicDBObject query = null;
        final BasicDBObject keys = new BasicDBObject();

        DBCollection dbCollectionSrc = getDbCollectionUsedForTesting();
        final SecurityAttributes userSecurityAttributes = new SecurityAttributes();
        RedactedDBCollection redactedDBCollection = new RedactedDBCollection(dbCollectionSrc, userSecurityAttributes, capcoRedactExpression);
        final DBObject dbObject = redactedDBCollection.findOne(query, keys);

        assertNotNull(dbObject);
    }

    @Test
    public void testFindOne1() throws Exception {

    }






    // Sample data
    // ==================  record #1 =========================
    //    {
    //    	"_id" : ObjectId("5375052930040f83a06f115a"),5a"),
    //    	"firstName" : "Sheldon",
    //    	"lastName" : "Humphrey",
    //    	"ssn" : {
    //    		"sl" : [
    //    			[
    //    				{
    //    					"c" : "TS"
    //    				}
    //    			],
    //    			[
    //    				{
    //    					"sci" : "G"
    //    				}
    //    			]
    //    		],
    //    		"value" : "354-61-8555"
    //    	},
    //    	"country" : {
    //    		"sl" : [
    //    			[
    //    				{
    //    					"c" : "S"
    //    				}
    //    			],
    //    			[
    //    				{
    //    					"sci" : "HCS"
    //    				}
    //    			]
    //    		],
    //    		"value" : "IRAQ"
    //    	},
    //    	"favorites" : {
    //    		"sl" : [
    //    			[
    //    				{
    //    					"c" : "S"
    //    				}
    //    			]
    //    		],
    //    		"cartoonCharacters" : [
    //    			"Diablo The Raven ",
    //    			"Rabbit",
    //    			"bar"
    //    		]
    //    	},
    //    	"foo" : "bar"
    //    }
    // ==================  record #2 =========================
    //    {
    //    	"_id" : ObjectId("5375052930040f83a06f1160"),60"),
    //    	"firstName" : "Alice",
    //    	"lastName" : "Fuentes",
    //    	"ssn" : {
    //    		"sl" : [
    //    			[
    //    				{
    //    					"c" : "TS"
    //    				}
    //    			],
    //    			[
    //    				{
    //    					"sci" : "SI"
    //    				},
    //    				{
    //    					"sci" : "TK"
    //    				}
    //    			]
    //    		],
    //    		"value" : "409-56-5309"
    //    	},
    //    	"country" : {
    //    		"sl" : [
    //    			[
    //    				{
    //    					"c" : "TS"
    //    				}
    //    			],
    //    			[
    //    				{
    //    					"sci" : "HCS"
    //    				},
    //    				{
    //    					"sci" : "G"
    //    				}
    //    			]
    //    		],
    //    		"value" : "UNITED STATES"
    //    	},
    //    	"favorites" : {
    //    		"sl" : [
    //    			[
    //    				{
    //    					"c" : "TS"
    //    				}
    //    			],
    //    			[
    //    				{
    //    					"sci" : "SI"
    //    				},
    //    				{
    //    					"sci" : "TK"
    //    				}
    //    			]
    //    		],
    //    		"cartoonCharacters" : [
    //    			"Tantor"
    //    		]
    //    	}
    //    }
    //============================================
    private static void seedCollectionTwoSampleRecords(DBCollection dbCollectionSrc) {
        dbCollectionSrc.insert(getRecordOne(), WriteConcern.NORMAL);
        dbCollectionSrc.insert(getRecordTwo(), WriteConcern.NORMAL);
    }

    private static DBObject getRecordOne() {
           return (DBObject) JSON.parse("{ \"_id\" : \"5375052930040f83a06f115a\", \"firstName\" : \"Sheldon\", \"lastName\" : \"Humphrey\", " +
                "\"ssn\" : { \"sl\" : [ [ { \"c\" : \"TS\" } ], [ { \"sci\" : \"G\" } ] ], \"value\" : \"354-61-8555\" }, \"country\" : { \"sl\" : [ [ { \"c\" : \"S\" } ], [ { \"sci\" : \"HCS\" } ] ], \"value\" : \"IRAQ\" }, \"favorites\" : { \"sl\" : [ [ { \"c\" : \"S\" } ] ], \"cartoonCharacters\" : [ \"Diablo The Raven \", \"Rabbit\", \"bar\" ] }, \"foo\" : \"bar\" }");
    }

    private static DBObject getRecordTwo() {
         return (DBObject) JSON.parse("{ \"_id\" : \"5375052930040f83a06f1160\", \"firstName\" : \"Alice\", \"lastName\" : \"Fuentes\", " +
                "\"ssn\" : { \"sl\" : [ [ { \"c\" : \"TS\" } ], [ { \"sci\" : \"SI\" }, { \"sci\" : \"TK\" } ] ], \"value\" : \"409-56-5309\" }, \"country\" : { \"sl\" : [ [ { \"c\" : \"TS\" } ], [ { \"sci\" : \"HCS\" }, { \"sci\" : \"G\" } ] ], \"value\" : \"UNITED STATES\" }, \"favorites\" : { \"sl\" : [ [ { \"c\" : \"TS\" } ], [ { \"sci\" : \"SI\" }, { \"sci\" : \"TK\" } ] ], \"cartoonCharacters\" : [ \"Tantor\" ] } }");
    }

    /** contains a 9 line complete FLAC application!   Also proves that a user w/ TS should see more than a user w/ U */
    @Test
    public void sampleApplication() throws Exception {

        //final BasicDBObject query = new BasicDBObject("firstName", "Sheldon");    // locate docs with { firstName: "Sheldon" }

        // try Alice
        final BasicDBObject query = new BasicDBObject("firstName", "Alice");    // locate docs with { firstName: "Alice" }

        final BasicDBObject keys = new BasicDBObject();                           // get all fields/columns

        DBCollection dbCollectionSrc = getDbCollectionUsedForTesting();           // this is a normal standard Mongodb collection
        ////
        // below we use the CapcoSecurityAttributes that knows how to do our application logic of
        // c:TS also maps to c:S,  c:C, and  c:U
        ////
        // here we have a user with clearance of "TS"  and they also have sci: TK
        ////
        final CapcoSecurityAttributes userSecurityAttributes = new CapcoSecurityAttributes();
        userSecurityAttributes.setClearance("U");
        userSecurityAttributes.setSci(Arrays.asList("TK"));

        // Now we get a new secure redactedDBCollection, that will honor the above CapcoSecurityAttributes
        // and only provide fields that this specific user is allowed to access:
        RedactedDBCollection redactedDBCollection = new RedactedDBCollection(dbCollectionSrc, userSecurityAttributes, capcoRedactExpression);
        Cursor dbObjectsCursor = redactedDBCollection.find(query, keys);

        final DBObject dbObject = dbObjectsCursor.next();
        System.out.println("First matching document: (c:U/TK) :: " + dbObject);

       ////////////////////////////
       // The above is a complete FLAC application, what following code does in this test file is prove that a
       // person with TS  should see more fields.
       ////////////////////////////

        final CapcoSecurityAttributes userSecurityAttributesTS = new CapcoSecurityAttributes();
        userSecurityAttributesTS.setClearance("TS");
        userSecurityAttributesTS.setSci(Arrays.asList("TK"));
        RedactedDBCollection redactedDBCollectionTS = new RedactedDBCollection(dbCollectionSrc, userSecurityAttributes, capcoRedactExpression);
        Cursor dbObjectsCursorTS = redactedDBCollectionTS.find(query, keys);
        final DBObject dbObjectTS = dbObjectsCursorTS.next();
        System.out.println("First matching document: (c:TS/TK) :: " + dbObjectTS);

        assertNotSame(dbObjectsCursorTS, dbObjectsCursor);                        // a user w/ TS should see more than a user w/ U
    }

}
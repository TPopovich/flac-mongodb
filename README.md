FLAC for mongodb - provides a way to do field level access control in mongodb using 2.6+ that has support for redact in aggregate:

        final BasicDBObject query = new BasicDBObject("firstName", "Sheldon");
        final BasicDBObject keys = new BasicDBObject();

        DBCollection dbCollectionSrc = getDbCollectionUsedForTesting();
        // below we use the UserSecurityAttributesMapCapco that knows how to do our application logic of
        // c:TS also maps to c:S,  c:C, and  c:U
        final UserSecurityAttributesMap userSecurityAttributes = new UserSecurityAttributesMapCapco();
        userSecurityAttributes.setClearance("TS");

        RedactedDBCollection redactedDBCollection = new RedactedDBCollection(dbCollectionSrc, userSecurityAttributes);
        Cursor dbObjects = redactedDBCollection.find(query, keys);
        Cursor dbObjects = redactedDBCollection.aggregate( ... );

notice that we have a redactedDBCollection.find working on a DBCollection that is protected by FLAC access controls
and that the find is really based on aggregate using $redact to limit sub-documents as needed


This effort differs from the one given in project:
  https://github.com/TPopovich/flac-mongodb-requires-changes-to-javacoredrivers
as that one requires changes to the mongodb drivers, and that is harder to do -- any changes need to be
carefully vetted before release as it could impact many important applications.

This project's effort is the enable effective usage of FLAC in mongodb. But not design it in a way to be
as seamless in how you can use this new "DB Collection" as we did in the other project listed above.
In that other project we created a unified DBCursor to access the output as you would using a native
DBCollection from mongo.  Here we have the same find() findOne() and aggregate() methods but they
return Cursor output.  This approach does not require any modification to mongodb's java drivers.



package com.mongodb.flac;

import com.mongodb.*;
import com.mongodb.util.JSON;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * RedactedDBCollection is a class that acts like a DBCollection. It wraps a standard mongodb DBCollection.
 *
 * <p>But this class honors user specific FLAC security controls.  This allows
 * tight control on access to data, also known as Field Level Access Control. </p>
 *
 * <p> The application can then use the this class as they would use a normal DBCollection
 * for the most part.  Since the underlying find operations will be transformed into aggregation pipeline
 * there are a few minor restrictions.  However all find and aggregation run through an aggregation
 * pipeline that has a special-purpose build $redact clause that implements CAPCO like security controls.
 * </p>
 *
 * <h3>Overview:</h3>
 * This class looks large, but, it is not.  Picture a wrapped DBCollection inside.  Imagine all calls that deal
 * with a <b>find</b> or <b>aggregation</b> operation on that DBCollection getting trapped and instead converted
 * safely into a new aggregation pipeline that has as its first stage the above mentioned carefully crafted
 * $redact clause to honor CAPCO like security controls.  Then the output from mongodb will only
 * contain fields and subdocuments that are able to be given to the current user. The user still has to
 * store in the raw documents "document level controls" that look like this:<br/>
 *  "somefield" : { "sl" : ____  , "value": ___ }   and the "sl" fields control access.  See our documents
 *  that accompany this reference implementation and our test files for sample documents that are marked
 *  up with "sl" fields.
 *
 *  <h3>Implementation Details</h3>
 *  The file has every permutation of find/aggregation permitted on DBCollection, but if you only
 *  need a few variations, you could safely comment out or delete the ones your do not need.
 *
 * <p> Q: where / how does aggregation know the users ability to see certain fields?
 *     A: we have a class com.mongodb.flac.SecurityAttributes  that stores them. (Also see
 *     the CAPCO specific subclass com.mongodb.flac.capco.CapcoSecurityAttributes). The
 *     application will in general do the following, it will instantiate a SecurityAttributes
 *     class and fill it with user permissions, e.g. clearance sci etc.  Then that class
 *     is given to this class.  whatever user permissions are granted is used when we construct
 *     the mongodb $redact phrase to indicate to mongo what documents and parts of documents
 *     can be returned to this specific user.  (See the code below.)
 * </p>
 *

 * <h3>Typical usage pattern:</h3>
 * <p> As a little code will show, you can now do something like this: </p>
 * <p/>
 *
 * <pre>
 *
 *     final BasicDBObject query = new BasicDBObject("firstName", "Alice");    // locate docs with { firstName: "Alice" }
 *     final BasicDBObject keys = new BasicDBObject();                           // get all fields/columns
 *     DBCollection dbCollectionSrc =  ... ;
 *
 *     <span style="color:green">CapcoSecurityAttributes</span> userSecurityAttributes = new <span style="color:green">CapcoSecurityAttributes()</span>;
 *
 *     userSecurityAttributes.setClearance("TS");     // set the users permissions
 *     userSecurityAttributes.setSci(Arrays.asList("TK"));
 *
 *     // construct a protected interface that honors FLAC security by using a  RedactedDBCollection
 *     <span style="color:green">RedactedDBCollection</span> redactedDBCollection = new RedactedDBCollection(dbCollectionSrc, userSecurityAttributes);
 *     Cursor dbObjectsCursor = redactedDBCollection.find(query, keys);
 *
 *     final DBObject dbObject = dbObjectsCursor.next();     // the return value depends on the userSecurityAttributes
 *                                                           // and different users may see different components
 *
 * </pre>
 * <p/>
 *
 * <h3>Code Step by Step</h3>
 * <pre>
     1,2  we choose the query that we want { firstName of “Alice” } and all available fields.
     3 we get the original DBCollection, this is not protected by FLAC
     4 we construct the object needed to specify the current users permissions
     5 we continue step 4 by saying that the user has clearance of TS and has SCI of TK
     6 we construct the protected RedactedDBCollection that wraps the native unprotected DBCollection , and all operations on it will honor FLAC
     7 we display the matching records
 * </pre>
 *
 * <p/>
 * <p> See the <span style="color:green">$redact processor</span> that we created in {@link com.mongodb.flac.capco.CapcoSecurityAttributes} which is created for CAPCO
 * ( http://fas.org/sgp/othergov/intel/capco_reg.pdf ) like protection, where we have an AND-ing of OR-s so we
 * can support use cases of i.e. clearance of TS and sci of either TK or G.
 * <p/>
 * <p/>
 * </p>
 * <p> See a complete 9 line application, the core of that application your saw above,  built on FLAC by looking at some test code found in:
 * <span style="color:green">com.mongodb.flac.RedactedDBCollectionTest#sampleApplication()</span>.  Find that file in the test
 * code subdirectory.
 * </p>
 *
 * <h3> For more info about mongodb aggregation technology </h3>
 *
 * <p/>
 *  $redact  http://docs.mongodb.org/master/reference/operator/aggregation/redact/#pipe._S_redact
 *
 * <p/>
 *   aggregation  http://docs.mongodb.org/master/reference/operator/aggregation/
 *
 * @see com.mongodb.flac.capco.CapcoSecurityAttributes
 * @see SecurityAttributes
 */
public class RedactedDBCollection {

    // typedef  alias - an Aggregation Pipeline is an ArrayList of DBObject's  - this is just a name for an  ArrayList<DBObject>
    //                  destined to be the list given to aggregate as the job pipeline of operations...
    public static class SecureAggregationPipeline extends ArrayList<DBObject> {
    }

    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(RedactedDBCollection.class);

    final ReadPreference readPreferenceDefault = ReadPreference.primary();
    ReadPreference readPreference = null;

    /**
     * The current users SecurityAttributes, which is a application specific mapping of security attributes,
     * e.g. clearance  sci  citizenship   etc.
     */
    private SecurityAttributes userSecurityAttributes;
    
    private RedactExpression redactExpression;
    
    public static final BasicDBObject EMPTY_OBJECT = new BasicDBObject(); // i.e. "{}";


    /**
     * Initializes a new safe collection.
     * Constructs a wrapper over a standard MongoDB collection {@link com.mongodb.DBCollection} for safe
     * access to a collection that
     * considers the information in the document honoring the
     * user specified FLAC "sl" (field level access control - security
     * level label) field
     * before accessing documents from that collection. No operation is
     * actually performed on the database with this call,
     * we access the wrapped collection in a lazy manner.
     * <p/>
     *
     *  <p>
     * Consider a simple use case.
     * Given a DBCollection for the "persons" mongo collection
     * and a set of user's SecurityAttributes, which have meaning perhaps for a government application, e.g.
     * <pre><tt>
     *        clearance="TS"
     *        sci=[ "TK", "SI", "G", "HCS" ]
     *        etc ...
     * </tt></pre>
     * These are specified in the userSecurityAttributes map.
     * </p>
     * <p/>
     * <p> This wrapper class will internally build up a deferred aggregationPipeline that is called
     *     eventually when the user actually fetches data. And it will use the attributes stored
     * </p>
     *
     * @param wrappedDBCollection    the wrapped DB collection on which we operate
     * @param userSecurityAttributes a Map of attributes, e.g.  clearance="TS", sci=[ "TK", "SI", "G", "HCS" ] etc
     *                               that provide the user's SecurityAttributes.  A detailed list of attributes might be:
     *                               <pre><tt>
     *                                      clearance="TS"
     *                                      sci=[ "TK", "SI", "G", "HCS" ]
     *                                      citizenship=["US"]
     *                               </tt></pre>
     */
    public RedactedDBCollection(DBCollection wrappedDBCollection, SecurityAttributes userSecurityAttributes, RedactExpression redactExpression) {
        checkNotNull(wrappedDBCollection, "wrappedDBCollection can't be null");
        this.userSecurityAttributes = checkNotNull(userSecurityAttributes, "userSecurityAttributes can't be null");
        this.redactExpression = checkNotNull(redactExpression, "redactExpression can't be null");
        this._wrapped = wrappedDBCollection;
        namespace = wrappedDBCollection.getFullName();
    }


    private DBCollection _wrapped;
    private final String namespace;

    public Cursor find(DBObject query, DBObject fields, int numToSkip, int batchSize, int limit, int options,
                       ReadPreference readPref, DBDecoder decoder) {
        if (willTrace()) {
            trace("RedactedDBCollection find: " + namespace + " " + JSON.serialize(query) + " fields " + JSON.serialize(safeDref(fields, EMPTY_OBJECT)));
        }

        final SecureAggregationPipeline pipelineSecure = getSecureAggregationPipelineForUser();

        appendQueryToSecureAggregationPipeline(pipelineSecure, query);
        appendProjectToSecureAggregationPipeline(pipelineSecure, fields);
        appendSkipToSecureAggregationPipeline(pipelineSecure, numToSkip);
        appendLimitToSecureAggregationPipeline(pipelineSecure, limit);

        return _wrapped.aggregate(pipelineSecure, AggregationOptions.builder().
                batchSize(batchSize).
                outputMode(AggregationOptions.OutputMode.CURSOR).
                build(), readPref);
    }




    public Cursor find(DBObject query, DBObject fields, int numToSkip, int batchSize, int limit, int options,
                       ReadPreference readPref, DBDecoder decoder, DBEncoder encoder) {

        if (willTrace()) {
            trace("RedactedDBCollection find: " + namespace + " " + JSON.serialize(query) + " fields " + JSON.serialize(safeDref(fields, EMPTY_OBJECT)));
        }

        final SecureAggregationPipeline pipelineSecure = getSecureAggregationPipelineForUser();

        appendQueryToSecureAggregationPipeline(pipelineSecure, query);
        appendProjectToSecureAggregationPipeline(pipelineSecure, fields);
        appendSkipToSecureAggregationPipeline(pipelineSecure, numToSkip);
        appendLimitToSecureAggregationPipeline(pipelineSecure, limit);

        return _wrapped.aggregate(pipelineSecure, AggregationOptions.builder().
                batchSize(batchSize).
                outputMode(AggregationOptions.OutputMode.CURSOR).
                build(), readPref);
    }

    public Cursor find(DBObject query, DBObject fields, int numToSkip, int batchSize, int limit, int options,
                       ReadPreference readPref, DBObject orderBy) {
        if (willTrace()) {
            trace("RedactedDBCollection find: " + namespace + " " + JSON.serialize(query) + " fields " + JSON.serialize(safeDref(fields, EMPTY_OBJECT)));
        }

        final SecureAggregationPipeline pipelineSecure = getSecureAggregationPipelineForUser();

        appendQueryToSecureAggregationPipeline(pipelineSecure, query);
        appendProjectToSecureAggregationPipeline(pipelineSecure, fields);
        appendSkipToSecureAggregationPipeline(pipelineSecure, numToSkip);
        appendLimitToSecureAggregationPipeline(pipelineSecure, limit);
        appendSortToSecureAggregationPipeline(pipelineSecure, orderBy);

        return _wrapped.aggregate(pipelineSecure, AggregationOptions.builder().
                batchSize(batchSize).
                outputMode(AggregationOptions.OutputMode.CURSOR).
                build(), readPref);
    }



    public Cursor find(DBObject query, DBObject fields, int limit,
                       ReadPreference readPref, DBObject orderBy) {
        if (willTrace()) {
            trace("RedactedDBCollection find: " + namespace + " " + JSON.serialize(query) + " fields " + JSON.serialize(safeDref(fields, EMPTY_OBJECT)));
        }

        final SecureAggregationPipeline pipelineSecure = getSecureAggregationPipelineForUser();

        appendQueryToSecureAggregationPipeline(pipelineSecure, query);
        appendProjectToSecureAggregationPipeline(pipelineSecure, fields);
        appendLimitToSecureAggregationPipeline(pipelineSecure, limit);
        appendSortToSecureAggregationPipeline(pipelineSecure, orderBy);

        return _wrapped.aggregate(pipelineSecure, AggregationOptions.builder().
                outputMode(AggregationOptions.OutputMode.CURSOR).
                build(), readPref);
    }


    /**
     * Queries for an object in this collection using Secure AggregationPipeline, an implementation of FLAC (field level access control).
     *
     * @param query A document outlining the search query
     * @return an iterator over the results
     */
    public Cursor find(DBObject query) {
        return find(query, null);
    }

    /**
     * Queries for an object in this collection using Secure AggregationPipeline, an implementation of FLAC (field level access control).
     * <p>
     * An empty DBObject will match every document in the collection.
     * Regardless of fields specified, the _id fields are always returned.
     * </p>
     * <p>
     * An example that returns the "x" and "_id" fields for every document
     * in the collection that has an "x" field:
     * </p>
     * <pre>
     * {@code
     * BasicDBObject keys = new BasicDBObject();
     * keys.put("x", 1);
     *
     * DBCursor cursor = collection.find(new BasicDBObject(), keys);}
     * </pre>
     *
     * @param query  object for which to search
     *               Restrictions
     *               You cannot use $where in $match queries as part of the aggregation pipeline.
     *               To use $text in the $match stage, the $match stage has to be the first stage of the pipeline,
     *               but for security the redact will be first, so $text can not be utilized.
     * @param fields fields to return
     * @return a cursor to iterate over results
     */
    public Cursor find(DBObject query, DBObject fields) {
        if (willTrace()) {
            trace("RedactedDBCollection find: " + namespace + " " + JSON.serialize(query) + " fields " + JSON.serialize(safeDref(fields, EMPTY_OBJECT)));
        }

        final SecureAggregationPipeline pipelineSecure = getSecureAggregationPipelineForUser();

        appendQueryToSecureAggregationPipeline(pipelineSecure, query);
        appendProjectToSecureAggregationPipeline(pipelineSecure, fields);

        return _wrapped.aggregate(pipelineSecure, AggregationOptions.builder().outputMode(AggregationOptions.OutputMode.CURSOR).build(), getReadPreference());
    }


    /**
     * Queries for all objects in this collection using Secure AggregationPipeline, an implementation of FLAC (field level access control).
     *
     * @return a cursor which will iterate over every object
     * @mongodb.driver.manual tutorial/query-documents/ Query
     */
    public Cursor find() {
        return find(null, null);
    }


    /**
     * Returns a single object from this collection using Secure AggregationPipeline, an implementation of FLAC (field level access control).
     *
     * @return the object found, or {@code null} if the collection is empty
     * @throws MongoException
     * @mongodb.driver.manual tutorial/query-documents/ Query
     */
    public DBObject findOne() {
        return findOne(new BasicDBObject());
    }

    /**
     * Returns a single object from this collection matching the query using Secure AggregationPipeline, an implementation of FLAC (field level access control).
     *
     * @param o the query object
     * @return the object found, or {@code null} if no such object exists
     * @throws MongoException
     * @mongodb.driver.manual tutorial/query-documents/ Query
     */
    public DBObject findOne(DBObject o) {
        return findOne(o, null, null, getReadPreference());
    }

    /**
     * Returns a single object from this collection matching the query using Secure AggregationPipeline, an implementation of FLAC (field level access control).
     *
     * @param o      the query object
     * @param fields fields to return
     * @return the object found, or {@code null} if no such object exists
     * @throws MongoException
     * @mongodb.driver.manual tutorial/query-documents/ Query
     */
    public DBObject findOne(DBObject o, DBObject fields) {
        return findOne(o, fields, null, getReadPreference());
    }

    /**
     * Returns a single object from this collection matching the query using Secure AggregationPipeline, an implementation of FLAC (field level access control).
     *
     * @param o       the query object
     * @param fields  fields to return
     * @param orderBy fields to order by
     * @return the object found, or {@code null} if no such object exists
     * @throws MongoException
     * @mongodb.driver.manual tutorial/query-documents/ Query
     */
    public DBObject findOne(DBObject o, DBObject fields, DBObject orderBy) {
        return findOne(o, fields, orderBy, getReadPreference());
    }

    /**
     * Get a single document from collection using Secure AggregationPipeline, an implementation of FLAC (field level access control).
     *
     * @param o        the selection criteria using query operators.
     * @param fields   specifies which fields MongoDB will return from the documents in the result set.
     * @param readPref {@link ReadPreference} to be used for this operation
     * @return A document that satisfies the query specified as the argument to this method, or {@code null} if no such object exists
     * @throws MongoException
     * @mongodb.driver.manual tutorial/query-documents/ Query
     */
    public DBObject findOne(DBObject o, DBObject fields, ReadPreference readPref) {
        return findOne(o, fields, null, readPref);
    }

    /**
     * Get a single document from collection using Secure AggregationPipeline, an implementation of FLAC (field level access control).
     *
     * @param o        the selection criteria using query operators.
     * @param fields   specifies which projection MongoDB will return from the documents in the result set.
     * @param orderBy  A document whose fields specify the attributes on which to sort the result set.
     * @param readPref {@code ReadPreference} to be used for this operation
     * @return A document that satisfies the query specified as the argument to this method, or {@code null} if no such object exists
     * @throws MongoException
     * @mongodb.driver.manual tutorial/query-documents/ Query
     */
    public DBObject findOne(DBObject o, DBObject fields, DBObject orderBy, ReadPreference readPref) {
        return findOne(o, fields, orderBy, readPref, 0, MILLISECONDS);
    }

    /**
     * Get a single document from collection using Secure AggregationPipeline, an implementation of FLAC (field level access control).
     *
     * @param query       the selection criteria using query operators.
     * @param fields      specifies which projection MongoDB will return from the documents in the result set.
     * @param orderBy     A document whose fields specify the attributes on which to sort the result set.
     * @param readPref    {@code ReadPreference} to be used for this operation
     * @param maxTime     the maximum time that the server will allow this operation to execute before killing it
     * @param maxTimeUnit the unit that maxTime is specified in
     * @return A document that satisfies the query specified as the argument to this method.
     * @mongodb.driver.manual tutorial/query-documents/ Query
     * @since 2.12.0
     */
    DBObject findOne(DBObject query, DBObject fields, DBObject orderBy, ReadPreference readPref,
                     long maxTime, TimeUnit maxTimeUnit) {
        if (willTrace()) {
            trace("RedactedDBCollection findOne: " + namespace + " " + JSON.serialize(query) + " fields " + JSON.serialize(safeDref(fields, EMPTY_OBJECT)));
        }

        final SecureAggregationPipeline pipelineSecure = getSecureAggregationPipelineForUser();

        appendQueryToSecureAggregationPipeline(pipelineSecure, query);
        appendProjectToSecureAggregationPipeline(pipelineSecure, fields);
        appendSortToSecureAggregationPipeline(pipelineSecure, orderBy);

        Cursor i = _wrapped.aggregate(pipelineSecure, AggregationOptions.builder().
                outputMode(AggregationOptions.OutputMode.CURSOR).
                maxTime(maxTime, maxTimeUnit).
                build(), readPref);


        DBObject obj = (i.hasNext() ? i.next() : null);
        if (obj != null && (fields != null && fields.keySet().size() > 0)) {
            obj.markAsPartialObject();
        }
        return obj;
    }

    /**
     * Do aggregation pipeline in a secure manner, using Secure AggregationPipeline, an implementation of FLAC (field level access control).
     *
     * @param pipeline       List<DBObject> of operations for aggregation Pipeline
     * @param options
     * @param readPreference
     * @return QueryResultIterator or DBCursor (if the last part of pipeline had a $out)
     */
    public Cursor aggregate(final List<DBObject> pipeline, final AggregationOptions options,
                            final ReadPreference readPreference) {
        if (willTrace()) {
            trace("RedactedDBCollection aggregate: " + namespace);
        }

        checkNotNull(pipeline,"pipeline can not be null");
        checkNotNull(options,"options can not be null");

        final List<DBObject> pipelineSecure = getSecureAggregationPipelineForUser();
        pipelineSecure.addAll(pipeline);

        return _wrapped.aggregate(pipelineSecure, options, readPreference);

    }

    /**
     * Do aggregation pipeline in a secure manner, using Secure AggregationPipeline, an implementation of FLAC (field level access control).
     *
     * @param firstOp       requisite first operation to be performed in the aggregation pipeline
     * @param additionalOps additional operations to be performed in the aggregation pipeline
     * @return the aggregation operation's result set
     * @mongodb.driver.manual core/aggregation-pipeline/ Aggregation
     * @mongodb.server.release 2.2
     * @deprecated Use {@link com.mongodb.DBCollection#aggregate(java.util.List)} instead
     */
    @Deprecated
    public Cursor aggregate(final DBObject firstOp, final DBObject... additionalOps) {
        List<DBObject> pipeline = new ArrayList<DBObject>();
        pipeline.add(firstOp);
        Collections.addAll(pipeline, additionalOps);
        return aggregate(pipeline);
    }

    /**
     * Do aggregation pipeline in a secure manner, using Secure AggregationPipeline, an implementation of FLAC (field level access control).
     *
     * @param pipeline operations to be performed in the aggregation pipeline
     * @return the aggregation's result set
     * @mongodb.driver.manual core/aggregation-pipeline/ Aggregation
     * @mongodb.server.release 2.2
     */
    public Cursor aggregate(final List<DBObject> pipeline) {
        return aggregate(pipeline, getReadPreference());
    }

    /**
     * Do aggregation pipeline in a secure manner, using Secure AggregationPipeline, an implementation of FLAC (field level access control).
     *
     * @param pipeline       operations to be performed in the aggregation pipeline
     * @param readPreference the read preference specifying where to run the query
     * @return the aggregation's result set
     * @mongodb.driver.manual core/aggregation-pipeline/ Aggregation
     * @mongodb.server.release 2.2
     */
    public Cursor aggregate(final List<DBObject> pipeline, ReadPreference readPreference) {
        AggregationOptions options = AggregationOptions.builder()
                .outputMode(AggregationOptions.OutputMode.CURSOR)
                .build();

        return aggregate(pipeline, options, readPreference);
    }

    /**
     * Do aggregation pipeline in a secure manner, using Secure AggregationPipeline, an implementation of FLAC (field level access control).
     *
     * @param pipeline operations to be performed in the aggregation pipeline
     * @param options  options to apply to the aggregation
     * @return the aggregation operation's result set
     * @mongodb.driver.manual core/aggregation-pipeline/ Aggregation
     * @mongodb.server.release 2.2
     */
    public Cursor aggregate(final List<DBObject> pipeline, AggregationOptions options) {
        return aggregate(pipeline, options, getReadPreference());
    }


    /**
     * get the Secure Aggregation Pipeline for the user.
     *
     * @return new pipeline {@link java.util.ArrayList} with SecurityRedact phase on the front.
     */
    protected SecureAggregationPipeline getSecureAggregationPipelineForUser() {
        return getSecureAggregationPipelineForUserWorker(userSecurityAttributes);
    }

    protected SecureAggregationPipeline getSecureAggregationPipelineForUserWorker(SecurityAttributes userSecurityAttributes) {
        SecureAggregationPipeline redactPipeline = new SecureAggregationPipeline();
        redactPipeline = prependSecurityRedactToPipelineWorker(redactPipeline, userSecurityAttributes);
        return redactPipeline;
    }

    /**
     * prepend the SecurityRedact Phrase To Pipeline and return a new redactPipeline and also
     *
     * @param redactPipeline              pipeline that we need to modify to prepend the $redact phase onto
     * @param userSecurityAttributes      visibility Attributes For User suitable for FLAC $redact operation
     * @return a new  List<DBObject> that has a $redact operation on the front
     */
    private SecureAggregationPipeline prependSecurityRedactToPipelineWorker(final ArrayList<DBObject> redactPipeline, final SecurityAttributes userSecurityAttributes) {
        final SecureAggregationPipeline redactPipelineNew = new SecureAggregationPipeline();
        final DBObject redactCommandForPipeline = getRedactCommand(userSecurityAttributes);

        redactPipelineNew.add(redactCommandForPipeline);       // make sure that the $redact is the first thing in the aggregate pipeline
        redactPipelineNew.addAll(redactPipeline);
        return redactPipelineNew;
    }

    /**
     * build the "$redact" mongodb command based on specified userSecurityAttributes
     */
    private DBObject getRedactCommand(SecurityAttributes userSecurityAttributes) {
        
        //String userSecurityExpression = String.format(com.mongodb.flac.RedactedDBCollectionConstants.getSecurityExpression(), visibilityAttributesForUser);
        String userSecurityExpression = redactExpression.getRedactExpression(userSecurityAttributes);
        logger.debug("**************** find/aggregate() userSecurityExpression: " + userSecurityExpression);
        DBObject redactCommand = (DBObject) JSON.parse(userSecurityExpression);
        return new BasicDBObject("$redact", redactCommand);
    }


    /////
    //  Following are all the operations that can be added to a MongoDB aggregation pipeline that are supported:
    //  appendClauseToSecureAggregationPipeline() can add any clause, but we have named all the key clauses below,
    //  like  Query = "$match", etc.
    /////
    private void appendLimitToSecureAggregationPipeline(SecureAggregationPipeline pipelineSecure, int limit) {
        if (limit != 0) appendClauseToSecureAggregationPipeline(pipelineSecure, "$limit", limit);
    }

    private void appendSkipToSecureAggregationPipeline(SecureAggregationPipeline pipelineSecure, int numToSkip) {
        if (numToSkip != 0) appendClauseToSecureAggregationPipeline(pipelineSecure, "$skip", numToSkip);
    }

    private void appendProjectToSecureAggregationPipeline(SecureAggregationPipeline pipelineSecure, DBObject fields) {
        if (fields != null) if (dbObjectHasData(fields)) appendClauseToSecureAggregationPipeline(pipelineSecure, "$project", fields);
    }

    private void appendQueryToSecureAggregationPipeline(SecureAggregationPipeline pipelineSecure, DBObject query) {
        if (query != null) if (dbObjectHasData(query)) appendClauseToSecureAggregationPipeline(pipelineSecure, "$match", query);
    }

    private void appendSortToSecureAggregationPipeline(SecureAggregationPipeline pipelineSecure, DBObject orderBy) {
        if (orderBy != null) if (dbObjectHasData(orderBy)) appendClauseToSecureAggregationPipeline(pipelineSecure, "$sort", orderBy);
    }

    private void appendClauseToSecureAggregationPipeline(SecureAggregationPipeline pipelineSecure, final String clauseKey, Object criteria) {
        if (criteria != null) {
            DBObject match = new BasicDBObject(clauseKey, criteria);
            pipelineSecure.add(match);
        }
    }

    private boolean dbObjectHasData(DBObject dbObject) {
        if (dbObject != null) {
            if (dbObject instanceof BasicDBObject) {

                if (((BasicDBObject) dbObject).size() > 0) {
                    return true;
                }

            } else {
                return true;
            }
        }
        return false;
    }


    /**
     * get the provided or default ReadPreference (currently ReadPreference.primary() ).
     *
     * @return
     */
    public ReadPreference getReadPreference() {
        return (readPreference == null) ? readPreferenceDefault : readPreference;
    }

    /**
     * set the default ReadPreference
     *
     * @param readPreference the ReadPreference
     */
    public void setReadPreference(ReadPreference readPreference) {
        this.readPreference = readPreference;
    }


    // Util methods

    private Object safeDref(DBObject fields, DBObject def) {
        if (fields == null) return def;
        return fields;
    }

    private static final Logger TRACE_LOGGER = Logger.getLogger("com.mongodb.flac.TRACE");
    private static final Level TRACE_LEVEL = Boolean.getBoolean("DB.TRACE") ? Level.INFO : Level.FINEST;

    private boolean willTrace() {
        return TRACE_LOGGER.isLoggable(TRACE_LEVEL);
    }

    private void trace(String s) {
        TRACE_LOGGER.log(TRACE_LEVEL, s);
    }



    /**
     * Verify that an object referenced passed as a parameter is not null and return that object also.
     *
     * @param reference an object reference to verify
     * @param errorMessage the exception message to use if the null check fails
     * @return the non-null same object,  that was validated
     * @throws NullPointerException if {@code reference} is null
     */
    public static <T> T checkNotNull(T reference, String errorMessage) {
        if (reference == null) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        }
        return reference;
    }

}

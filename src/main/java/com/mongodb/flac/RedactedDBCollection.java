package com.mongodb.flac;

import com.mongodb.*;
import com.mongodb.flac.UserSecurityAttributesMap;
import com.mongodb.util.JSON;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
import static com.mongodb.QueryResultIterator.chooseBatchSize;
import static com.mongodb.WriteCommandResultHelper.getBulkWriteException;
import static com.mongodb.WriteCommandResultHelper.getBulkWriteResult;
import static com.mongodb.WriteCommandResultHelper.hasError;
import static com.mongodb.WriteRequest.Type.*;
import static com.mongodb.WriteRequest.Type.INSERT;
import static com.mongodb.WriteRequest.Type.REPLACE;
*/
import static java.lang.String.format;
import com.google.common.base.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.bson.util.Assertions.isTrue;
import static org.bson.util.Assertions.notNull;

/**
 * RedactedDBCollection is a class that acts like a DBCollection. It wraps a standard mongodb DBCollection.
 * But this class honors user specific FLAC security controls.  This allows
 * tight control on access to data, also known as Field Level Access Control.
 * <p/>
 *
 * <p> The application can then use the DBCollection as they would use a normal DBCollection
 * for the most part.  Since the underlying find operations will be transformed into aggregation pipeline
 * there are a few minor restrictions.
 * </p>
 *
 * <p> As a little code will show, you can now do something like this: </p>
 * <p/>
 * <p> <pre><tt><code>
 *
 *     DBCollection dbCollectionSrc =  ... ;
 *
 *     UserSecurityAttributesMapCapco userSecurityAttributes = new UserSecurityAttributesMapCapco();
 *
 *     userSecurityAttributes.setClearance("U");     // set the users permissions
 *
 *
 *     // construct a protected interface that honors FLAC security by using a  RedactedDBCollection
 *     RedactedDBCollection redactedDBCollection = new RedactedDBCollection(dbCollectionSrc, userSecurityAttributes);
 *     Cursor dbObjectsCursor = redactedDBCollection.find(query, keys);
 *
 *     final DBObject dbObject = dbObjectsCursor.next();     // the return value depends on the userSecurityAttributes
 *                                                           // and different users may see different components
 *
 *
 * </code></tt></pre>
 * </p>
 *
 * <p> See the $redact processor that we created in {@link } which is created for CAPCO
 *     ( http://fas.org/sgp/othergov/intel/capco_reg.pdf ) like protection, where we have an AND-ing of OR-s so we
 *     can support use cases of i.e. clearance of TS and sci of either TK or G.
 *
 *
 * </p>
 * <p> See a complete 9 line application built on FLAC by looking at some test code found in:
 *     com.mongodb.flac.RedactedDBCollectionTest#sampleApplication(). See that file in the test
 *     code subdirectory.
 * </p>
 */
@SuppressWarnings("deprecation")
public class RedactedDBCollection {

    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(RedactedDBCollection.class);

    final ReadPreference readPreferenceDefault = ReadPreference.primary();
    ReadPreference readPreference = null;


    /**
     * The current users SecurityAttributes, which is a application specific mapping of security decls.
     */
    private UserSecurityAttributesMap userSecurityAttributes;
    public static final BasicDBObject EMPTY_OBJECT = new BasicDBObject(); // i.e. "{}";


    /**
     * Initializes a new safe collection.
     * Constructs a wrapper over a standard MongoDB collection {@link com.mongodb.DBCollection} for safe
     * access to a collection that
     * considers the information in the document honoring the
     * user specified FLAC "sl" (field level access control - security
     * level) field
     * before accessing documents from that collection. No operation is
     * actually performed on the database with this call,
     * we access it in a lazy manner.
     * <p/>
     * <p>
     * This is similar to:
     * <tt> RedactedDBCollection.fromCollection( db.getCollection("persons") , Map userSecurityAttributes ); </tt>
     * but is used to create the wrapper directly instead of using a builder pattern.
     * <p/>
     * Consider a simple use case. See the {@link com.mongodb.flac.docs.SampleApplicationDescription} docs that are also included in the kit for more information.
     * Given a DBCollection for the "persons" mongo collection
     * and a set of UserSecurityAttributes, which have meaning perhaps for a government application, e.g.
     * <pre><tt>
     *        clearance="TS"
     *        sci=[ "TK", "SI", "G", "HCS" ]
     *        countries=["US"]
     * </tt></pre>
     * might set user attributes.  These are specified in the userSecurityAttributes map.
     * <p/>
     * </p>
     * <p> This wrapper class will internally build up a deferred aggregationPipeline that is called
     * eventually when the user actually fetches data.
     * </p>
     *
     * @param wrappedDBCollection    the wrapped DB collection on which we operate
     * @param userSecurityAttributes a Map of attributes, e.g.  clearance="TS", sci=[ "TK", "SI", "G", "HCS" ] etc
     *                               that provide the UserSecurityAttributes.  A detailed list of attributes might be:
     *                               <pre><tt>
     *                                                                                               clearance="TS"
     *                                                                                               sci=[ "TK", "SI", "G", "HCS" ]
     *                                                                                               countries=["US"]
     *                                                                                        </tt></pre>
     */
    public RedactedDBCollection(DBCollection wrappedDBCollection, UserSecurityAttributesMap userSecurityAttributes) {
        Preconditions.checkNotNull(wrappedDBCollection, "wrappedDBCollection can't be null");
        this.userSecurityAttributes = Preconditions.checkNotNull(userSecurityAttributes, "userSecurityAttributes can't be null");

        this._wrapped = wrappedDBCollection;
        namespace = wrappedDBCollection.getFullName();
    }

    /**
     * Builder to construct a wrapper for safe access a standard MongoDB collection {@link com.mongodb.DBCollection} that
     * considers the information in the document honoring the
     * user specified FLAC "sl" (field level access control - security
     * level) field
     * before accessing documents from that collection. No operation is
     * actually performed on the database with this call,
     * we access it in a lazy manner.
     * <p/>
     * <p/>
     * <p>
     * RedactedDBCollection.fromCollection( db.getCollection("persons") , Map userSecurityAttributes );
     * above is a simple use case.  Given a DBCollection for the "persons" mongo collection
     * and a set of UserSecurityAttributes, e.g.
     * <pre><tt>
     *        clearance="TS"
     *        sci=[ "TK", "SI", "G", "HCS" ]
     *        countries=["US"]
     * </tt></pre>
     * <p/>
     * </p>
     *
     * @param wrappedDBCollection    the wrapped DB collection on which we operate
     * @param userSecurityAttributes a Map of attributes, e.g.  clearance="TS", sci=[ "TK", "SI", "G", "HCS" ] etc
     *                               that provide the UserSecurityAttributes.  A detailed list of attributes might be:
     *                               <pre><tt>
     *                                                                                               clearance="TS"
     *                                                                                               sci=[ "TK", "SI", "G", "HCS" ]
     *                                                                                               countries=["US"]
     *                                                                                        </tt></pre>
     */
    public static RedactedDBCollection fromCollection(DBCollection wrappedDBCollection, UserSecurityAttributesMap userSecurityAttributes) {
        return new RedactedDBCollection(wrappedDBCollection, userSecurityAttributes);
    }

    private DBCollection _wrapped;
    private UserSecurityAttributesMap _userSecurityString;
    private final String namespace;

    public Cursor find(DBObject query, DBObject fields, int numToSkip, int batchSize, int limit, int options,
                       ReadPreference readPref, DBDecoder decoder) {
        if (willTrace()) {
            trace("RedactedDBCollection find: " + namespace + " " + JSON.serialize(query) + " fields " + JSON.serialize(safeDref(fields, EMPTY_OBJECT)));
        }

        final SecureAggregationPipeline pipelineSecure = getSecureAggregationPipelineForUser();

        if (query != null) {
            pipelineSecure.appendQuery(query);
        }
        if (fields != null) {
            pipelineSecure.appendMatch(fields);
        }
        if (numToSkip != 0) {
            pipelineSecure.appendSkip(numToSkip);
        }
        if (limit != 0) {
            pipelineSecure.appendLimit(limit);
        }

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

        if (query != null) {
            pipelineSecure.appendQuery(query);
        }
        if (fields != null) {
            pipelineSecure.appendMatch(fields);
        }
        if (numToSkip != 0) {
            pipelineSecure.appendSkip(numToSkip);
        }
        if (limit != 0) {
            pipelineSecure.appendLimit(limit);
        }

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

        if (query != null) {
            pipelineSecure.appendQuery(query);
        }
        if (fields != null) {
            pipelineSecure.appendMatch(fields);
        }
        if (numToSkip != 0) {
            pipelineSecure.appendSkip(numToSkip);
        }
        if (limit != 0) {
            pipelineSecure.appendLimit(limit);
        }
        if (orderBy != null) {
            pipelineSecure.appendSort(orderBy);
        }

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

        if (query != null) {
            pipelineSecure.appendQuery(query);
        }
        if (fields != null) {
            pipelineSecure.appendMatch(fields);
        }
        if (limit != 0) {
            pipelineSecure.appendLimit(limit);
        }
        if (orderBy != null) {
            pipelineSecure.appendSort(orderBy);
        }

        return _wrapped.aggregate(pipelineSecure, AggregationOptions.builder().
                outputMode(AggregationOptions.OutputMode.CURSOR).
                build(), readPref);
    }


    /**
     * Queries for an object in this collection.
     *
     * @param query A document outlining the search query
     * @return an iterator over the results
     * @mongodb.driver.manual tutorial/query-documents/ Query
     */
    public Cursor find(DBObject query) {
        return find(query, null);
    }

    /**
     * Queries for an object in this collection.
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
     * @mongodb.driver.manual tutorial/query-documents/ Query
     */
    public Cursor find(DBObject query, DBObject fields) {
        if (willTrace()) {
            trace("RedactedDBCollection find: " + namespace + " " + JSON.serialize(query) + " fields " + JSON.serialize(safeDref(fields, EMPTY_OBJECT)));
        }

        final SecureAggregationPipeline pipelineSecure = getSecureAggregationPipelineForUser();

        if (query != null) {
            pipelineSecure.appendQuery(query);
        }
        if (fields != null) {
            pipelineSecure.appendMatch(fields);
        }

        return _wrapped.aggregate(pipelineSecure, AggregationOptions.builder().outputMode(AggregationOptions.OutputMode.CURSOR).build(), getReadPreference());
    }


    /**
     * Queries for all objects in this collection.
     *
     * @return a cursor which will iterate over every object
     * @mongodb.driver.manual tutorial/query-documents/ Query
     */
    public Cursor find() {
        return find(null, null);
    }


    /**
     * Returns a single object from this collection.
     *
     * @return the object found, or {@code null} if the collection is empty
     * @throws MongoException
     * @mongodb.driver.manual tutorial/query-documents/ Query
     */
    public DBObject findOne() {
        return findOne(new BasicDBObject());
    }

    /**
     * Returns a single object from this collection matching the query.
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
     * Returns a single object from this collection matching the query.
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
     * Returns a single object from this collection matching the query.
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
     * Get a single document from collection.
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
     * Get a single document from collection.
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
     * Get a single document from collection.
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

        if (query != null) {
            pipelineSecure.appendQuery(query);
        }
        if (fields != null) {
            pipelineSecure.appendMatch(fields);
        }
        if (orderBy != null) {
            pipelineSecure.appendSort(orderBy);
        }

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
     * Do aggregation pipeline in a secure manner.
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

        if (options == null) {
            throw new IllegalArgumentException("options can not be null");
        }
        if (pipeline == null) {
            throw new IllegalArgumentException("pipeline can not be null");
        }
        if (options == null) {
            throw new IllegalArgumentException("options can not be null");
        }

        final List<DBObject> pipelineSecure = getSecureAggregationPipelineForUser();
        pipelineSecure.addAll(pipeline);

        return _wrapped.aggregate(pipelineSecure, options, readPreference);

    }

    /**
     * Method implements aggregation framework.
     *
     * @param firstOp       requisite first operation to be performed in the aggregation pipeline
     * @param additionalOps additional operations to be performed in the aggregation pipeline
     * @return the aggregation operation's result set
     * @mongodb.driver.manual core/aggregation-pipeline/ Aggregation
     * @mongodb.server.release 2.2
     * @deprecated Use {@link com.mongodb.DBCollection#aggregate(java.util.List)} instead
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public Cursor aggregate(final DBObject firstOp, final DBObject... additionalOps) {
        List<DBObject> pipeline = new ArrayList<DBObject>();
        pipeline.add(firstOp);
        Collections.addAll(pipeline, additionalOps);
        return aggregate(pipeline);
    }

    /**
     * Method implements aggregation framework.
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
     * Method implements aggregation framework.
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
     * Method implements aggregation framework.
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
     * @return new pipeline {@link com.mongodb.flac.SecureAggregationPipeline} with SecurityRedact phase on the front.
     */
    protected SecureAggregationPipeline getSecureAggregationPipelineForUser() {
        String visibilityAttributesForUser = this.userSecurityAttributes.encodeFlacSecurityAttributes();
        return getSecureAggregationPipelineForUserWorker(visibilityAttributesForUser);
    }

    protected static SecureAggregationPipeline getSecureAggregationPipelineForUserWorker(String visibilityAttributesForUser) {
        final SecureAggregationPipeline redactPipeline = new SecureAggregationPipeline(visibilityAttributesForUser);

        return redactPipeline;
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

    /////////////////////
    /////  The following methods are convenience methods to help use RedactedDBCollection  as closely to a
    /////  DBCollection as possible, and pass thru operations to the internal _wrapped  DBCollection
    /////////////////////

    /**
     * Insert a document into a collection. If the collection does not exists on the server, then it will be created. If the new document
     * does not contain an '_id' field, it will be added.
     *
     * @param o       {@code DBObject} to be inserted
     * @param concern {@code WriteConcern} to be used during operation
     * @return the result of the operation
     * @throws MongoException if the operation fails
     * @dochub insert Insert
     */
    public WriteResult insert(DBObject o, WriteConcern concern) {
        return _wrapped.insert(o, concern);
    }

    /**
     * Insert documents into a collection. If the collection does not exists on the server, then it will be created. If the new document
     * does not contain an '_id' field, it will be added.
     *
     * @param list    a list of {@code DBObject}'s to be inserted
     * @param concern {@code WriteConcern} to be used during operation
     * @param encoder {@code DBEncoder} to use to serialise the documents
     * @return the result of the operation
     * @throws MongoException if the operation fails
     * @mongodb.driver.manual tutorial/insert-documents/ Insert
     */
    public WriteResult insert(List<DBObject> list, WriteConcern concern, DBEncoder encoder) {
        return _wrapped.insert(list, concern, encoder);
    }


    /**
     * Remove documents from a collection.
     *
     * @param criteria the deletion criteria using query operators. Omit the query parameter or pass an empty document to delete all
     *                 documents in the collection.
     * @param concern  {@code WriteConcern} to be used during operation
     * @return the result of the operation
     * @throws MongoException
     * @mongodb.driver.manual tutorial/remove-documents/ Remove
     */
    public WriteResult remove(DBObject criteria, WriteConcern concern) {
        return _wrapped.remove(criteria, concern);
    }

    /**
     * Remove documents from a collection.
     *
     * @param criteria the deletion criteria using query operators. Omit the query parameter or pass an empty document to delete all
     *                 documents in the collection.
     * @param concern  {@code WriteConcern} to be used during operation
     * @param encoder  {@code DBEncoder} to be used
     * @return the result of the operation
     * @throws MongoException
     * @mongodb.driver.manual tutorial/remove-documents/ Remove
     */
    public WriteResult remove(DBObject criteria, WriteConcern concern, DBEncoder encoder) {
        return _wrapped.remove(criteria, concern, encoder);
    }

    /**
     * Remove documents from a collection. Calls {@link DBCollection#remove(com.mongodb.DBObject, com.mongodb.WriteConcern)} with the
     * default WriteConcern
     *
     * @param criteria the deletion criteria using query operators. Omit the query parameter or pass an empty document to delete all documents in
     *                 the collection.
     * @return the result of the operation
     * @throws MongoException
     * @mongodb.driver.manual tutorial/remove-documents/ Remove
     */
    public WriteResult remove(DBObject criteria) {
        return _wrapped.remove(criteria);
    }

    /**
     * Modify an existing document or documents in collection. By default the method updates a single document. The query parameter employs
     * the same query selectors, as used in {@link DBCollection#find(DBObject)}.
     *
     * @param query   the selection criteria for the update
     * @param o       the modifications to apply
     * @param upsert  when true, inserts a document if no document matches the update query criteria
     * @param multi   when true, updates all documents in the collection that match the update query criteria, otherwise only updates one
     * @param concern {@code WriteConcern} to be used during operation
     * @param encoder the DBEncoder to use
     * @return the result of the operation
     * @throws MongoException
     * @mongodb.driver.manual tutorial/modify-documents/ Modify
     */
    public WriteResult update(DBObject query, DBObject o, boolean upsert, boolean multi, WriteConcern concern,
                              DBEncoder encoder) {
        return _wrapped.update(query, o, upsert, multi, concern, encoder);
    }


    public void drop() {
        _wrapped.drop();
    }


    public void createIndex(final DBObject keys, final DBObject options, DBEncoder encoder) {
        _wrapped.createIndex(keys, options, encoder);
    }


    // Util methods

    private Object safeDref(DBObject fields, DBObject def) {
        if (fields == null) return def;
        return fields;
    }

    private Object safeDref(String fields, String def) {
        if (fields == null) return def;
        return fields;
    }

    private Object safeDref(Number fields, Number def) {
        if (fields == null) return def;
        return fields;
    }


    private static final Logger TRACE_LOGGER = Logger.getLogger("com.mongodb.TRACE");
    private static final Level TRACE_LEVEL = Boolean.getBoolean("DB.TRACE") ? Level.INFO : Level.FINEST;

    private boolean willTrace() {
        return TRACE_LOGGER.isLoggable(TRACE_LEVEL);
    }

    private void trace(String s) {
        TRACE_LOGGER.log(TRACE_LEVEL, s);
    }

    private Logger getLogger() {
        return TRACE_LOGGER;
    }


}

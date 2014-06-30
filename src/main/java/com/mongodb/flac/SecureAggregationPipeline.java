package com.mongodb.flac;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Secure Aggregation Pipeline that will hold commands for a mongodb aggregation operation.
 * It will always have a $redact operator at the beginning that will keep information secure.
 *
 * <p> This is essentially a List<DBObject> but it also knows many of the Mongodb aggregation operations.</p>
 */
class SecureAggregationPipeline extends ArrayList<DBObject> {

    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(SecureAggregationPipeline.class);



    public SecureAggregationPipeline(String visibilityAttributesForUser) {
        super();
        prependSecurityRedactToPipelineWorker(visibilityAttributesForUser);
    }

    public SecureAggregationPipeline(Collection<? extends DBObject> dbObjects, String visibilityAttributesForUser) {
        super();

        prependSecurityRedactToPipelineWorker(visibilityAttributesForUser);
        this.addAll(dbObjects);
    }



    public void appendMatch(DBObject keys) {
        if (dbObjectHasData(keys)) appendClause("$project", keys);
    }

    public void appendQuery(DBObject criteria) {
        if (dbObjectHasData(criteria)) appendClause("$match", criteria);
    }


    public void appendLimit(Object criteria) {
        if (dbObjectHasData(criteria)) appendClause("$limit", criteria);
    }

    public void appendSort(Object orderBy) {
        if (dbObjectHasData(orderBy)) appendClause("$sort", orderBy);
    }

    public void appendSkip(Object skipBy) {
        if (dbObjectHasData(skipBy)) appendClause("$skip", skipBy);
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

    private boolean dbObjectHasData(Object object) {
        if (object != null) {
            return true;
        }
        return false;
    }

    private void appendClause(final String clauseKey, Object criteria) {

        if (criteria != null) {
            DBObject match = new BasicDBObject(clauseKey, criteria);
            this.add(match);
        }
    }


    /**
     * prepend the SecurityRedact Phrase To Pipeline
     * @param visibilityAttributesForUser    visibility Attributes For User suitable for FLAC $redact operation
     * @return
     */
    protected List<DBObject> prependSecurityRedactToPipelineWorker(String visibilityAttributesForUser) {
        final DBObject redactCommandForPipeline = getRedactCommand(visibilityAttributesForUser);

        this.add(redactCommandForPipeline);
        return this;
    }

//    /**
//     * Add "$redact" mongodb command incantation to aggregate pipeline, note that we build a MATCH element
//     * since that is where the $redact is nested, and append that to the pipeline.
//     *
//     * @param pipeline the pipeline that will form the basis of the aggregate operation
//     * @param criteria the match criteria desired by user, if none pass in NULL
//     * @param redact   the redact clause
//     */
//    private void addRedactionMatchToPipeline(List<DBObject> pipeline, DBObject criteria, DBObject redact) {
//        pipeline.add(redact);         // redact is always added first
//        if (criteria != null) {
//            DBObject match = new BasicDBObject("$match", criteria);
//            pipeline.add(match);
//        }
//    }
//
//    /**
//     * Add "limit" for multi-object results to mongodb command incantation to pipeline
//     */
//    private void addLimitToPipeline(int recordLimit, List<DBObject> pipeline) {
//        if (recordLimit <= 0) return;        // TODO: should we also define a default page size for 0?
//
//        DBObject limit = new BasicDBObject("$limit", recordLimit);
//
//        pipeline.add(limit);
//    }

    /**
     * build the "$redact" mongodb command based on current FLAC user visibilityAttributesForUser setting
     */
    private static DBObject getRedactCommand(String visibilityAttributesForUser) {
        if (visibilityAttributesForUser == null || visibilityAttributesForUser.trim().length() == 0) {
            visibilityAttributesForUser = "[ ]";
        }
        String userSecurityExpression = String.format(com.mongodb.flac.RedactedDBCollectionConstants.getSecurityExpression(), visibilityAttributesForUser);
        logger.debug("**************** find() userSecurityExpression: " + userSecurityExpression);
        DBObject redactCommand = (DBObject) JSON.parse(userSecurityExpression);
        return new BasicDBObject("$redact", redactCommand);
    }

}

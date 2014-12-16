/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package dk.statsbiblioteket.netark.dvenabler.wrapper;

import org.apache.commons.logging.Log;

public class ProgressTracker {
    private final String field;
    private final Log log;
    private final int maxDoc;
    private final int logEvery;

    private long requests;
    private long firstDoc;

    public ProgressTracker(String field, Log log, int maxDoc) {
        this.field = field;
        this.log = log;
        this.maxDoc = maxDoc;
        logEvery = maxDoc == 0 ? Integer.MAX_VALUE : Math.max(1000, maxDoc / 10);
    }

    public void ping(int docID) {
        requests++;
        if (docID == 0) {
            log.debug("Getting wrapped DocValue for field " + field + " in doc " + docID + "/" + maxDoc);
            firstDoc = System.nanoTime();
            requests = 1;
        } else if (docID % logEvery == 0 || docID == maxDoc-1) {
            long ms = (System.nanoTime()-firstDoc)/1000000;
            log.debug(String.format(
                    "Getting wrapped DocValue for field %s in doc %d/%d. " +
                    "Time since doc 0=%dms, requests=%d, speed %.2f docs/ms",
                    field, docID, maxDoc,
                    ms, requests, ms == 0 ? 0 : 1.0 * requests / ms));
        }
    }
}

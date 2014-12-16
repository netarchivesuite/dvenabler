/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.netark.dvenabler.wrapper;

import dk.statsbiblioteket.netark.dvenabler.DVConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.*;

/**
 * Memory-intensive transformer: All values are stored as String in (gasp) a Set and a List.
 */
// TODO: Remove the 2^31 limit on unique values
public class SortedSetDocValuesWrapper extends SortedSetDocValues {
    private static Log log = LogFactory.getLog(SortedSetDocValuesWrapper.class);

    private final AtomicReader reader;
    private final DVConfig field;
    private final Set<String> FIELDS; // Contains {@link #field} and nothing else
    // TODO: Store this in a BytesRefArray instead. This requires custom binary search
    private final List<BytesRef> values;

    private int docID = -1;
    private int ordinalIndex = -1;
    private long[] ordinals = new long[10];
    private int ordinalsCount = -1;
    private final ProgressTracker tracker;

    public SortedSetDocValuesWrapper(AtomicReader reader, DVConfig field) throws IOException {
        this.reader = reader;
        this.field = field;
        FIELDS = new HashSet<>(Arrays.asList(field.getName()));
        log.info("Creating map for SortedSetDocValues for field '" + field + "'");
        long startTime = System.nanoTime();
        values = fill();
        tracker = new ProgressTracker(field.getName(), log, reader.maxDoc());
        log.info("Finished creating SortedSetDocValues with " + values.size() + " unique values for " + reader.maxDoc()
                 + " docs for field '" + field + "' in " + ((System.nanoTime()-startTime)/1000000/1000) + "ms");
    }

    private List<BytesRef> fill() throws IOException {
        // TODO: Is this sort the same as the default BytesRef-based sort for DocValues?
        final SortedSet<BytesRef> values = new TreeSet<>();
        for (int docID = 0 ; docID < reader.maxDoc() ; docID++) {
            String[] stored = reader.document(docID, FIELDS).getValues(field.getName());
            if (stored == null) {
                continue;
            }
            for (String value: stored) {
                values.add(new BytesRef(value));
            }
        }
        return new ArrayList<>(values);
    }

    @Override
    public long nextOrd() {
        return ordinalIndex == ordinalsCount ? NO_MORE_ORDS : ordinals[ordinalIndex++];
    }

    @Override
    public void setDocument(int docID) {
        tracker.ping(docID);

        this.docID = docID;
        ordinalIndex = 0;
        ordinalsCount = 0;
        try {
            Document doc = reader.document(docID, FIELDS);
            String[] vals = doc.getValues(field.getName());
            if (vals == null) {
                return;
            }
            if (vals.length > ordinals.length) {
                ordinals = new long[vals.length];
            }
            for (String val: vals) {
                ordinals[ordinalsCount++] = getOrd(val);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException accessing field " + field, e);
        }
    }

    @Override
    public void lookupOrd(long ord, BytesRef result) {
        result.copyBytes(values.get((int) ord));
    }

    public long getOrd(String value) {
        int ord = Collections.binarySearch(values, new BytesRef(value));
        if (ord < 0) {
            throw new IllegalStateException(
                    "The ord for value '" + value + "' for docID " + docID + " in field '" + field
                    + "' could not be located but should always be present");
        }
        return ord;
    }

    @Override
    public long getValueCount() {
        return values.size();
    }
}

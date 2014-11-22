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
package dk.statsbiblioteket.netark.dvenabler;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.*;

/**
 * Memory-intensive transformer: All values are stored as String in (gasp) a Set and a List.
 */
public class SortedDocValuesWrapper extends SortedDocValues {
    private static Log log = LogFactory.getLog(SortedDocValuesWrapper.class);

    private final AtomicReader reader;
    private final String field;
    private final Set<String> FIELDS; // Contains {@link #field} and nothing else
    // TODO: Store this in a BytesRefArray instead. This requires custom binary search
    private final List<String> values;

    public SortedDocValuesWrapper(AtomicReader reader, String field) throws IOException {
        this.reader = reader;
        this.field = field;
        FIELDS = new HashSet<String>(Arrays.asList(field));
        log.info("Creating map for SortedDocValues for field '" + field + "'");
        long startTime = System.nanoTime();
        values = fill();
        log.info("Finished creating SortedDocValues with " + values.size() + " unique values for field '" + field
                 + "' in " + ((System.nanoTime()-startTime)/1000000/1000) + "ms");
    }

    private List<String> fill() throws IOException {
        final SortedSet<String> values = new TreeSet<String>();
        for (int docID = 0 ; docID < reader.maxDoc() ; docID++) {
            String value = reader.document(docID, FIELDS).get(field);
            if (value != null) {
                values.add(value);
            }
        }
        return new ArrayList<String>(values);
    }

    @Override
    public void get(int docID, BytesRef result) {
        try {
            String value = reader.document(docID, FIELDS).get(field);
            if (value == null) {
                // TODO: What is the best action when there is no value??
                throw new RuntimeException("Unable to get stored value for docID=" + docID + ", field=" + field);
            }
            result.copyChars(value);
        } catch (IOException e) {
            throw new RuntimeException("Unable to lookup docID=" + docID + ", field=" + field, e);
        }
    }

    @Override
    public int getOrd(int docID) {
        try {
            String value = reader.document(docID, FIELDS).get(field);
            if (value == null) {
                return -1;
            }
            int ord = Collections.binarySearch(values, value);
            if (ord < 0) {
                throw new IllegalStateException(
                        "The ord for value '" + value + "' for docID " + docID + " in field '" + field
                        + "' could not be located but should always be present");
            }
            return ord;
        } catch (IOException e) {
            throw new RuntimeException("Unable to lookup docID=" + docID + ", field=" + field, e);
        }
    }

    @Override
    public void lookupOrd(int ord, BytesRef result) {
        // TODO: Add check for -1
        result.copyChars(values.get(ord));
    }

    @Override
    public int getValueCount() {
        return values.size();
    }
}

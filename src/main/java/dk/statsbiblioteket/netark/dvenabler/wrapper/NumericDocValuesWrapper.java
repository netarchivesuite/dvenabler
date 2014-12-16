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
import org.apache.lucene.index.*;

import java.io.IOException;
import java.util.*;

/**
 * Simple indirection wrapper. Only downside is speed.
 */
public class NumericDocValuesWrapper extends NumericDocValues {
    private static Log log = LogFactory.getLog(NumericDocValuesWrapper.class);

    private final AtomicReader reader;
    private final long logEvery;
    private final DVConfig dvConfig;
    private final Set<String> FIELDS; // Contains {@link #field} and nothing else
    private final ProgressTracker tracker;

    public NumericDocValuesWrapper(AtomicReader reader, DVConfig dvConfig) throws IOException {
        this.reader = reader;
        logEvery = reader.maxDoc() == 0 ? Long.MAX_VALUE : reader.maxDoc() / 10;
        this.dvConfig = dvConfig;
        FIELDS = new HashSet<>(Arrays.asList(dvConfig.getName()));
        tracker = new ProgressTracker(dvConfig.getName(), log, reader.maxDoc());
    }

    @Override
    public long get(int docID) {
        tracker.ping(docID);
        try {
            IndexableField iField = reader.document(docID, FIELDS).getField(dvConfig.getName());
            if (iField == null) {
                log.warn("No stored value for field '" + dvConfig.getName() + "' in doc " + docID
                         + ". Returning -1");
                // This should have been handled by {@link DVAtomicReader#getDocsWithField}
                return -1;
            }
            Number number = iField.numericValue();
            if (number == null) {
                throw new RuntimeException(
                        "No numeric value '" + iField.stringValue() + "' for field '" + dvConfig.getName()
                         + "' in doc " + docID + ". This looks like a non-numeric field!");
            }
            // TODO: Determine correct method to call from field info
            switch (dvConfig.getNumericType()) {
                case LONG: return number.longValue();
                case INT: return number.intValue();
                case DOUBLE: return Double.doubleToLongBits(number.doubleValue());
                case FLOAT: return Float.floatToIntBits(number.longValue());
                default: throw new IllegalStateException(
                        "Unknown NumericType " + dvConfig.getNumericType() + " for field " + dvConfig.getName());
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to get field '" + dvConfig.getName() + "' from docID " + docID, e);
        }
    }

}

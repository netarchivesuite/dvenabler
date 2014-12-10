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
    private final DVConfig dvConfig;
    private final Set<String> FIELDS; // Contains {@link #field} and nothing else

    public NumericDocValuesWrapper(AtomicReader reader, DVConfig dvConfig)
            throws IOException {
        this.reader = reader;
        this.dvConfig = dvConfig;
        FIELDS = new HashSet<String>(Arrays.asList(dvConfig.getName()));
    }

    @Override
    public long get(int docID) {
        try {
            IndexableField iField = reader.document(docID, FIELDS).getField(dvConfig.getName());
            if (iField == null) {
                log.warn("No stored value for field '" + dvConfig.getName() + "' in doc " + docID
                         + ". Returning -1");
                // This should have been handled by {@link DVAtomicReader#getDocsWithField}
                return -1;
            }
            // TODO: Determine correct method to call from field info
            switch (dvConfig.getNumericType()) {
                case LONG: return iField.numericValue().longValue();
                case INT: return iField.numericValue().intValue();
                case DOUBLE: return Double.doubleToLongBits(iField.numericValue().doubleValue());
                case FLOAT: return Float.floatToIntBits(iField.numericValue().longValue());
                default: throw new IllegalStateException(
                        "Unknown NumericType " + dvConfig.getNumericType()
                        + " for field " + dvConfig.getName());
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to get field '" + dvConfig.getName() + "' from docID " + docID, e);
        }
    }

}

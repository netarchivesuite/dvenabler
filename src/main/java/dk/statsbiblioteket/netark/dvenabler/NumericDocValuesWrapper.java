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
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.*;

/**
 * Simple indirection wrapper. Only downside is speed.
 */
public class NumericDocValuesWrapper extends NumericDocValues {
    private static Log log = LogFactory.getLog(NumericDocValuesWrapper.class);

    private final AtomicReader reader;
    private final FieldInfo fieldInfo;
    private final Set<String> FIELDS; // Contains {@link #field} and nothing else

    public NumericDocValuesWrapper(AtomicReader reader, FieldInfo fieldInfo) throws IOException {
        this.reader = reader;
        this.fieldInfo = fieldInfo;
        FIELDS = new HashSet<String>(Arrays.asList(fieldInfo.name));
    }

    @Override
    public long get(int docID) {
        try {
            IndexableField iField = reader.document(docID, FIELDS).getField(fieldInfo.name);
            if (iField == null) {
                log.trace("No stored value for field '" + fieldInfo.name + "' in doc " + docID + ". Returning -1");
                // TODO: Default DV-value on missing stored-value.
                return -1;
            }
            // TODO: Determine correct method to call from field info, instead of always returning long
            return iField.numericValue().longValue();
        } catch (IOException e) {
            throw new RuntimeException("Unable to get field '" + fieldInfo.name + "' from docID " + docID, e);
        }
    }
}

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

import com.sun.swing.internal.plaf.metal.resources.metal_sv;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.xml.FilterBuilderFactory;
import org.apache.lucene.store.FilterDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefArray;
import org.eclipse.jetty.deploy.bindings.OrderedGroupBinding;

import java.io.IOException;
import java.util.*;

/**
 *
 */
public class TransformingDirectoryReader extends FilterDirectoryReader {
    private static Log log = LogFactory.getLog(TransformingDirectoryReader.class);

    public TransformingDirectoryReader(DirectoryReader in) {
        super(in, new TransformingAtomicReaderWrapper());
        log.info("Constructed TransformingDirectoryReader with default wrapper");
    }

    public TransformingDirectoryReader(DirectoryReader in, SubReaderWrapper wrapper) {
        super(in, wrapper);
        log.info("Constructed TransformingDirectoryReader with wrapper " + wrapper.getClass().getSimpleName());
    }

    @Override
    protected DirectoryReader doWrapDirectoryReader(DirectoryReader in) {
        log.info("Wrapping DirectoryReader");
        return new TransformingDirectoryReader(in, new TransformingAtomicReaderWrapper());
    }

    public static class TransformingAtomicReaderWrapper extends SubReaderWrapper {
        @Override
        public AtomicReader wrap(AtomicReader reader) {
            log.debug("Wrapping Atomic");
            return new TransformingAtomicReader(reader);
        }
    }

    public static class TransformingAtomicReader extends FilterAtomicReader {
        public TransformingAtomicReader(AtomicReader in) {
            super(in);
            log.info("Wrapped AtomicReader");
        }

        @Override
        public NumericDocValues getNumericDocValues(String field) throws IOException {
            NumericDocValues dv = super.getNumericDocValues(field);
            log.info("getNumericDocValues called for field '" + field + "'. Has DV: " + (dv != null));
            return dv;
        }

        @Override
        public BinaryDocValues getBinaryDocValues(String field) throws IOException {
            BinaryDocValues dv = super.getBinaryDocValues(field);
            log.info("getBinaryDocValues called for field '" + field + "'. Has DV: " + (dv != null));
            return dv;
        }

        @Override
        public SortedDocValues getSortedDocValues(String field) throws IOException {
            SortedDocValues dv = super.getSortedDocValues(field);
            if (dv != null) {
                log.info("getSortedDocValues called for field '" + field + "'. DV already present, returning directly");
                return dv;
            }

            return new SortedDocValuesWrapper(this, field);
        }

        @Override
        public SortedSetDocValues getSortedSetDocValues(String field) throws IOException {
            SortedSetDocValues dv = super.getSortedSetDocValues(field);
            log.info("getSortedSetDocValues called for field '" + field + "'. Has DV: " + (dv != null));
            return dv;
        }

        @Override
        protected void doClose() throws IOException {
            log.info("close called");
            super.doClose();
        }
    }

    /**
     * Memory-intensive transformer: All values are stored as String in (gasp) a Set and a List.
     */
    public static class SortedDocValuesWrapper extends SortedDocValues {
        private final AtomicReader reader;
        private final String field;
        private final Set<String> FIELDS;
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
            result.copyChars(values.get(ord));
        }

        @Override
        public int getValueCount() {
            return values.size();
        }
    }
}

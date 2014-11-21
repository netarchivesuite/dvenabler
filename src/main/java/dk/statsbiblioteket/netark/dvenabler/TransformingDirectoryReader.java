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
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.xml.FilterBuilderFactory;
import org.apache.lucene.store.FilterDirectory;

import java.io.IOException;

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
            log.info("getSortedDocValues called for field '" + field + "'. Has DV: " + (dv != null));
            return dv;
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

}

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

import java.util.*;

/**
 * Wraps the standard DirectoryReader from Lucene and simulates DocValues for selected fields by extracting them from stored fields.
 * Note that the extraction can be very memory-heavy, primarily for String fields.
 */
public class DVDirectoryReader extends FilterDirectoryReader {
    private static Log log = LogFactory.getLog(DVDirectoryReader.class);
    private final Set<String> dvFields;

  /**
     * Create a DocValues-simulating reader.
     * @param in            the Lucene DirectoryReader to wrap.
     * @param dvFields  the fields to simulate DocValues for.
     */
    public DVDirectoryReader(DirectoryReader in, Set<String> dvFields) {
        super(in, new TransformingAtomicReaderWrapper(dvFields));
        this.dvFields = dvFields;
        log.info("Constructed DVDirectoryReader with DocValues wrapper");
    }

    @Override
    protected DirectoryReader doWrapDirectoryReader(DirectoryReader in) {
        log.info("Wrapping DirectoryReader");
        return new DVDirectoryReader(in, dvFields);
    }

    public static class TransformingAtomicReaderWrapper extends SubReaderWrapper {
        private final Set<String> dvFields;

        public TransformingAtomicReaderWrapper(Set<String> dvFields) {
            this.dvFields = dvFields;
        }

        @Override
        public AtomicReader wrap(AtomicReader reader) {
            log.debug("Wrapping Atomic");
            return new DVAtomicReader(reader, dvFields);
        }
    }
}

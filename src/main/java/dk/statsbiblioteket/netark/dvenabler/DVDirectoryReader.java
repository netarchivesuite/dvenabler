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
    private final Set<DVConfig> dvConfigs;

    /**
     * Creates an adjusting reader; removing or/and adding DocValues for the specified fields.
     * @param innerReader the reader to wrap.
     * @param dvConfigs the fields to adjust DocValues for.
     */
    public DVDirectoryReader(DirectoryReader innerReader, Set<DVConfig> dvConfigs) {
        super(innerReader, new TransformingAtomicReaderWrapper(dvConfigs));
        this.dvConfigs = dvConfigs;
        log.info("Constructed DVDirectoryReader with " + dvConfigs + " DocValue field adjustments");
    }

    @Override
    protected DirectoryReader doWrapDirectoryReader(DirectoryReader in) {
        log.info("Wrapping DirectoryReader with " + dvConfigs + " field adjustments");
        return new DVDirectoryReader(in, dvConfigs);
    }

    public static class TransformingAtomicReaderWrapper extends SubReaderWrapper {
        private final Set<DVConfig> dvConfigs;

        public TransformingAtomicReaderWrapper(Set<DVConfig> dvConfigs) {
            this.dvConfigs = dvConfigs;
        }

        @Override
        public AtomicReader wrap(AtomicReader reader) {
            log.debug("Wrapping Atomic");
            return new DVAtomicReader(reader, dvConfigs);
        }
    }
}

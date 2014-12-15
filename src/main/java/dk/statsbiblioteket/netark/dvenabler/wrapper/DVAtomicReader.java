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
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.OpenBitSet;

import java.io.IOException;
import java.util.*;

/**
 *
 */
public class DVAtomicReader extends FilterAtomicReader {
    private static Log log = LogFactory.getLog(DVAtomicReader.class);

    private final Map<String, DVConfig> dvConfigs;
    private final HashMap<String, Bits> dvContent = new HashMap<String, Bits>();

    @Override
    public FieldInfos getFieldInfos() {
        log.info("Wrapped getFieldInfos called");
        FieldInfos original = super.getFieldInfos();
        FieldInfo[] modified = new FieldInfo[original.size()];
        int index = 0;
        for (FieldInfo oInfo: original) {
            modified[index++] = dvConfigs.containsKey(oInfo.name) ?
                    dvConfigs.get(oInfo.name).getFieldInfo() : oInfo;
        }
                /*FieldInfo mInfo = new FieldInfo(
                        oInfo.name, oInfo.isIndexed(), oInfo.number, oInfo.hasVectors(),
                        oInfo.omitsNorms(), oInfo.hasPayloads(), oInfo.getIndexOptions(),
                        mDocValuesType, oInfo.getNormType(), oInfo.attributes());        */
        return new FieldInfos(modified);
    }

    private FieldType.NumericType inferNumericType(String field) {
        try {
            Terms terms = fields().terms(field);
            if (terms == null) {
                return null;
            }
            TermsEnum termsEnum = terms.iterator(null);
            BytesRef val;
            if ((val = termsEnum.next()) == null) {
                return null;
            }
            //System.out.println("Got value for field " + field + ": " + val);
        } catch (IOException e) {
            log.warn("IOException while trying to infer NumericType for field " + field, e);
        }
        return null;
    }

    /**
     * Creates an adjusting reader; removing or/and adding DocValues for the specified fields.
     * @param innerReader the reader to wrap.
     * @param dvConfigs a list of fields to adjust.
     *                 Fields in the innerReader not specified in dvConfigs are passed unmodified.
     */
    public DVAtomicReader(AtomicReader innerReader, Set<DVConfig> dvConfigs) {
        super(innerReader);
        this.dvConfigs = new HashMap<String, DVConfig>(dvConfigs.size());
        for (DVConfig dvConfig: dvConfigs) {
            this.dvConfigs.put(dvConfig.getName(), dvConfig);
        }
        log.info("Wrapped AtomicReader with " + dvConfigs.size() + " field adjustments");
    }

    // Should have been named docsWithDocValueEntriesForField
    // Creates a bitmap of the documents that has stored values and should have DocValues
    @Override
    public synchronized Bits getDocsWithField(final String field) throws IOException {
        if (!dvConfigs.containsKey(field)) {
            return super.getDocsWithField(field);
        }

        if (!dvContent.containsKey(field)) {
            log.info("Resolving docsWithField(" + field + ")");
            DVConfig dvConfig = dvConfigs.get(field);
            if (!dvConfig.hasDocValues()) {
                dvContent.put(field, null);
            } else {
                OpenBitSet hasContent = new OpenBitSet(maxDoc());
                final Set<String> FIELDS = new HashSet<String>(Arrays.asList(field));
                for (int docID = 0 ; docID < maxDoc() ; docID++) {
                     if (document(docID, FIELDS).getField(field) != null) {
                         hasContent.fastSet(docID);
                     }
                }
                dvContent.put(field, hasContent);
            }
        }
        return dvContent.get(field);
    }

    @Override
    public NumericDocValues getNumericDocValues(String field) throws IOException {
        log.debug("getNumericDocValues(" + field + ") called");
        if (!dvConfigs.containsKey(field)) {
            return super.getNumericDocValues(field);
        } else if (!dvConfigs.get(field).hasDocValues()) {
            return null;
        }
        NumericDocValues dv = super.getNumericDocValues(field);
        if (dv != null) {
            log.info("getNumericDocValues called for field '" + field + "'. DV already present, returning directly");
            return dv;
        }
        log.info("getNumericDocValues called for field '" + field + "' with no DV. Constructing from stored");
        // TODO: Infer whether this is long, int, double or float
        return new NumericDocValuesWrapper(this, dvConfigs.get(field));
    }

    @Override
    public BinaryDocValues getBinaryDocValues(String field) throws IOException {
        log.debug("getBinaryDocValues(" + field + ") called");
        if (!dvConfigs.containsKey(field)) {
            return super.getBinaryDocValues(field);
        } else if (!dvConfigs.get(field).hasDocValues()) {
            return null;
        }
        // TODO: Implement this
        BinaryDocValues dv = super.getBinaryDocValues(field);
        if (dv != null) {
            log.info("getBinaryDocValues called for field '" + field + "'. DV already present, returning directly");
            return dv;
        }
        log.info("getBinaryDocValues called for field '" + field + "' with no DV. Constructing from stored");
        // TODO: Implement this
        return dv;
    }

    @Override
    public SortedDocValues getSortedDocValues(String field) throws IOException {
        log.debug("getSortedDocValues(" + field + ") called");
        if (!dvConfigs.containsKey(field)) {
            return super.getSortedDocValues(field);
        } else if (!dvConfigs.get(field).hasDocValues()) {
            return null;
        }
        SortedDocValues dv = super.getSortedDocValues(field);
        if (dv != null) {
            log.info("getSortedDocValues called for field '" + field + "'. DV already present, returning directly");
            return dv;
        }
        log.info("getSortedDocValues called for field '" + field + "' with no DV. Constructing from stored");
        return new SortedDocValuesWrapper(this, dvConfigs.get(field));
    }

    @Override
    public SortedSetDocValues getSortedSetDocValues(String field) throws IOException {
        log.debug("getSortedSetDocValues(" + field + ") called");
        if (!dvConfigs.containsKey(field)) {
            return super.getSortedSetDocValues(field);
        } else if (!dvConfigs.get(field).hasDocValues()) {
            return null;
        }
        SortedSetDocValues dv = super.getSortedSetDocValues(field);
        if (dv != null) {
            log.info("getSortedSetDocValues called for field '" + field + "'. DV already present, returning directly");
            return dv;
        }
        log.info("getSortedSetDocValues called for field '" + field + "' with no DV. Constructing from stored");
        return new SortedSetDocValuesWrapper(this, dvConfigs.get(field));
    }

    @Override
    protected void doClose() throws IOException {
        log.info("close called");
        super.doClose();
    }
}

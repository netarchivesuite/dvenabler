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
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;

import java.io.IOException;
import java.util.Set;

/**
 *
 */
public class DVAtomicReader extends FilterAtomicReader {
    private static Log log = LogFactory.getLog(DVAtomicReader.class);

    private final Set<String> dvFields;

    @Override
    public FieldInfos getFieldInfos() {
        log.info("Wrapped getFieldInfos called");
        FieldInfos original = super.getFieldInfos();
        FieldInfo[] modified = new FieldInfo[original.size()];
        int index = 0;
        for (FieldInfo oInfo: original) {
            if (dvFields.contains(oInfo.name)) {
                // TODO: Infer the type or get it from arguments
                // http://www.gossamer-threads.com/lists/lucene/java-user/182641
                FieldType.NumericType numType = inferNumericType(oInfo.name);
                if (numType == null) {
                    // Single or multi-value String
                } else {
                    // TODO: Construct DocValuesType
                }
                FieldInfo.DocValuesType mDocValuesType = oInfo.getDocValuesType();
                //
                FieldInfo mInfo = new FieldInfo(
                        oInfo.name, oInfo.isIndexed(), oInfo.number, oInfo.hasVectors(),
                        oInfo.omitsNorms(), oInfo.hasPayloads(), oInfo.getIndexOptions(),
                        mDocValuesType, oInfo.getNormType(), oInfo.attributes());
                modified[index++] = mInfo;
            } else {
                modified[index++] = oInfo;
            }
        }
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
            System.out.println("Got value for field " + field + ": " + val);
        } catch (IOException e) {
            log.warn("IOException whily trying to infer NumericType for field " + field, e);
        }
        return null;
    }

    @Override
    public Fields fields() throws IOException {
        return super.fields();    // TODO: Implement this
    }

    public DVAtomicReader(AtomicReader in, Set<String> dvFields) {
        super(in);
        this.dvFields = dvFields;
        log.info("Wrapped AtomicReader");
    }

    @Override
    public NumericDocValues getNumericDocValues(String field) throws IOException {
        if (!dvFields.contains(field)) {
            return super.getNumericDocValues(field);
        }
        NumericDocValues dv = super.getNumericDocValues(field);
        log.info("getNumericDocValues called for field '" + field + "'. Has DV: " + (dv != null));
        return dv;
    }

    @Override
    public BinaryDocValues getBinaryDocValues(String field) throws IOException {
        if (!dvFields.contains(field)) {
            return super.getBinaryDocValues(field);
        }
        BinaryDocValues dv = super.getBinaryDocValues(field);
        log.info("getBinaryDocValues called for field '" + field + "'. Has DV: " + (dv != null));
        return dv;
    }

    @Override
    public SortedDocValues getSortedDocValues(String field) throws IOException {
        if (!dvFields.contains(field)) {
            return super.getSortedDocValues(field);
        }
        SortedDocValues dv = super.getSortedDocValues(field);
        if (dv != null) {
            log.info("getSortedDocValues called for field '" + field + "'. DV already present, returning directly");
            return dv;
        }

        return new SortedDocValuesWrapper(this, field);
    }

    @Override
    public SortedSetDocValues getSortedSetDocValues(String field) throws IOException {
        if (!dvFields.contains(field)) {
            return super.getSortedSetDocValues(field);
        }
        SortedSetDocValues dv = super.getSortedSetDocValues(field);
        log.info("getSortedSetDocValues called for field '" + field + "'. Has DV: " + (dv != null));
        return dv;
    }

    // TODO: Override getFieldInfos to adjust info for the DV-wrapping fields
    // TODO: Override getFields to mirror getFieldInfos adjustment of meta data

    @Override
    protected void doClose() throws IOException {
        log.info("close called");
        super.doClose();
    }
}

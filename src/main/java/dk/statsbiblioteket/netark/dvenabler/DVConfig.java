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

import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.FieldInfo;

/**
 * As FieldInfo does not contain enough information for the wrapper to work, we use this class to describe
 * what should happen.
 */
public class DVConfig implements Comparable<DVConfig>{
    
    private FieldInfo fieldInfo;
    private FieldType.NumericType numericType; // Only relevant when {@link FieldInfo#getDocValuesType} == NUMERIC

    public DVConfig(FieldInfo fieldInfo, FieldType.NumericType numericType) {
        this.fieldInfo = fieldInfo;
        this.numericType = numericType;
    }

    public FieldInfo getFieldInfo() {
        return fieldInfo;
    }

    public void setFieldInfo(FieldInfo fieldInfo) {
        this.fieldInfo = fieldInfo;
    }

    public FieldType.NumericType getNumericType() {
        return numericType;
    }

    public void setNumericType(FieldType.NumericType numericType) {
        this.numericType = numericType;
    }

    public String getName() {
        return fieldInfo.name;
    }

    /**
     * Convenience method for adjusting DocValues.
     * @param docValuesType if null, docValues are disabled for this field.
     * @return the adjusted DVConfig, which is also the current DVConfig.
     */
    public DVConfig set(FieldInfo.DocValuesType docValuesType) {
        fieldInfo = new FieldInfo(
                fieldInfo.name, fieldInfo.isIndexed(), fieldInfo.number, fieldInfo.hasVectors(),
                fieldInfo.omitsNorms(), fieldInfo.hasPayloads(), fieldInfo.getIndexOptions(),
                docValuesType, fieldInfo.getNormType(), fieldInfo.attributes());
        return this;
    }
    /**
     * Convenience method for adjusting DocValues as well as the sub-option NumericType.
     * @param docValuesType if null, docValues are disabled for this field.
     * @param numericType if {@code docValuesType == NUMERIC}, this must be non-null.
     * @return the adjusted DVConfig, which is also the current DVConfig.
     */
    public DVConfig set(FieldInfo.DocValuesType docValuesType, FieldType.NumericType numericType) {
        if (FieldInfo.DocValuesType.NUMERIC == docValuesType && numericType == null) {
            throw new IllegalArgumentException("The docValuesType is NUMERIC but numericType==null");
        }
        set(docValuesType);
        setNumericType(numericType);
        return this;
    }

    public boolean hasDocValues() {
        return fieldInfo.hasDocValues();
    }

    public int compareTo(DVConfig other) {                
        return this.fieldInfo.name.compareTo(other.fieldInfo.name);
    }   

}

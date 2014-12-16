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

import dk.statsbiblioteket.netark.dvenabler.wrapper.DVDirectoryReader;
import junit.framework.TestCase;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DVReaderTest extends TestCase {
    private static Log log = LogFactory.getLog(DVReaderTest.class);

    private static final String ID = "id";
    private static final String DV = "dv";
    private static final String DV_CONTENT = "dvcontent";
    private static final String SEARCH = "search";
    private static final String SEARCH_CONTENT = "searchcontent";
    private static final String SINGLE = "singlestring";
    private static final String SINGLE_CONTENT = "plainstoreS";
    private static final String MULTI = "multistring";
    private static final String MULTI_CONTENT_1 = "plainstoreM1";
    private static final String MULTI_CONTENT_2 = "plainstoreM2";
    private static final String DOUBLE = "double";
    private static final double DOUBLE_CONTENT = 12.13;
    private static final String FLOAT = "float";
    private static final float FLOAT_CONTENT = 18.5f;
    private static final long LONG_CONTENT = 87L;
    private static final String LONG = "long";

    private static final Version LUCENE_VERSION = Version.LUCENE_48;

    public void testCreateAndReadPlainIndex() throws IOException, ParseException {
        log.info("testCreateAndReadPlainIndex started");
        final File INDEX = generateIndex();
        try {
            assertIndexValues(INDEX, false);
        } finally {
            delete(INDEX);
        }
    }

    public void testCreateAndReadWrappedIndex() throws IOException, ParseException {
        log.info("testCreateAndReadPlainIndex started");
        final File INDEX = generateIndex();

        try {
            Directory directory = MMapDirectory.open(INDEX);
            IndexReader reader = new DVDirectoryReader(
                    DirectoryReader.open(directory), createDVFieldDescriptions(INDEX));
            IndexSearcher searcher = new IndexSearcher(reader);

            assertIndexValues(reader, searcher, true);
        } finally {
            delete(INDEX);
        }
    }

    public void testDVEnableIndex() throws IOException, ParseException {
        log.info("testDVEnableIndex started");

        final File INDEX_SRC = generateIndex();
        final File INDEX_DEST = new File("target/testindex.deletefreely2");
        try {
            IndexUtils.convert(INDEX_SRC, INDEX_DEST, createDVFieldDescriptions(INDEX_SRC));
            assertIndexValues(INDEX_SRC, false);
            assertIndexValues(INDEX_DEST, true);
        } finally {
            delete(INDEX_SRC);
            delete(INDEX_DEST);
        }
    }

    public void testLargerDVEnableIndex() throws IOException {
        final int DOCS = 1000;

        log.info("testLargerDVEnableIndex started");

        final File INDEX_SRC = generateIndex(DOCS);
        final File INDEX_DEST = new File("target/testindex.deletefreely.dest");
        try {
            IndexUtils.convert(INDEX_SRC, INDEX_DEST, createDVFieldDescriptions(INDEX_SRC));
            IndexReader readerSrc = DirectoryReader.open(MMapDirectory.open(INDEX_SRC));
            IndexReader readerDest = DirectoryReader.open(MMapDirectory.open(INDEX_DEST));

            long multiCount = 0;
            long singleCount = 0;
            long longCount = 0;
            long doubleCount = 0;
            for (int docID = 0 ; docID < DOCS ; docID++) {
                {
                    String[] multisSrc = readerSrc.document(docID).getValues(MULTI);
                    if (multisSrc != null) {
                        List<String> dvs = getSortedSetDocValues(readerDest, docID, MULTI);
                        Arrays.sort(multisSrc);
                        Collections.sort(dvs);
                        assertEquals("There should be as many DV as stored for field " + MULTI,
                                     multisSrc.length, dvs.size());
                        for (int i = 0; i < multisSrc.length; i++) {
                            assertEquals("Value " + i + " for field " + MULTI + " should be equal",
                                         multisSrc[i], dvs.get(i));
                            multiCount++;
                        }
                    }
                }
                {
                    String singleSrc = readerSrc.document(docID).get(SINGLE);
                    if (singleSrc != null) {
                        String dv = getSortedDocValue(readerDest, docID, SINGLE);
                        assertEquals("The DV for field " + SINGLE + " should match the stored value",
                                     singleSrc, dv);
                        singleCount++;
                    }
                }
                {
                    IndexableField fieldSrc = readerSrc.document(docID).getField(LONG);
                    if (fieldSrc != null) {
                        long longSrc = fieldSrc.numericValue().longValue();
                        long dv = getLongDocValue(readerDest, docID, LONG);
                        assertEquals("The DV for field " + LONG + " should match the stored value",
                                     longSrc, dv);
                        longCount++;
                    }
                }
                {
                    IndexableField fieldSrc = readerSrc.document(docID).getField(DOUBLE);
                    if (fieldSrc != null) {
                        double doubleSrc = fieldSrc.numericValue().doubleValue();
                        double dv = getDoubleDocValue(readerDest, docID, DOUBLE);
                        assertEquals("The DV for field " + DOUBLE + " should match the stored value",
                                     doubleSrc, dv);
                        doubleCount++;
                    }
                }
            }
            assertTrue("There should be at least 1 value for field " + MULTI + " in a document", multiCount > 0);
            assertTrue("There should be at least 1 value for field " + SINGLE + " in a document", singleCount > 0);
            assertTrue("There should be at least 1 value for field " + LONG + " in a document", longCount > 0);
            assertTrue("There should be at least 1 value for field " + DOUBLE + " in a document", doubleCount > 0);
            readerSrc.close();
            readerDest.close();
        } finally {
            delete(INDEX_SRC);
            delete(INDEX_DEST);
        }
    }

    private Set<DVConfig> createDVFieldDescriptions(File index) throws IOException {
        List<DVConfig> baseConfigs = IndexUtils.getDVConfigs(index);
        List<DVConfig> dvConfigs = new ArrayList<DVConfig>();
        for (DVConfig baseConfig: baseConfigs) {
            if (SINGLE.equals(baseConfig.getName())) {
                dvConfigs.add(baseConfig.set(FieldInfo.DocValuesType.SORTED));
            } else if (MULTI.equals(baseConfig.getName())) {
                dvConfigs.add(baseConfig.set(FieldInfo.DocValuesType.SORTED_SET));
            } else if (DOUBLE.equals(baseConfig.getName())) {
                dvConfigs.add(baseConfig.set(FieldInfo.DocValuesType.NUMERIC, FieldType.NumericType.DOUBLE));
            } else if (FLOAT.equals(baseConfig.getName())) {
                dvConfigs.add(baseConfig.set(FieldInfo.DocValuesType.NUMERIC, FieldType.NumericType.FLOAT));
            } else if (LONG.equals(baseConfig.getName())) {
                dvConfigs.add(baseConfig.set(FieldInfo.DocValuesType.NUMERIC, FieldType.NumericType.LONG));
            }
        }
        return new HashSet<DVConfig>(dvConfigs);
    }

    public static void assertIndexValues(File index, boolean dvExpected) throws IOException, ParseException {
        IndexReader reader = DirectoryReader.open(MMapDirectory.open(index));
        IndexSearcher searcher = new IndexSearcher(reader);
        try {
            assertIndexValues(reader, searcher, dvExpected);
        } finally {
            reader.close();
        }
    }

    private static void assertIndexValues(IndexReader reader, boolean dvExpected) throws IOException, ParseException {
        IndexSearcher searcher = new IndexSearcher(reader);
        assertIndexValues(reader, searcher, dvExpected);
    }

    private static void assertIndexValues(IndexReader reader, IndexSearcher searcher, boolean dvExpected)
            throws ParseException, IOException {

        final String M = "dvExpected=" + dvExpected + ". ";
        Analyzer analyzer = new StandardAnalyzer(LUCENE_VERSION);
        QueryParser queryParser = new QueryParser(LUCENE_VERSION, SEARCH, analyzer);
        Query query = queryParser.parse(SEARCH_CONTENT);
        TopDocs topDocs = searcher.search(query, 10);
        assertEquals(M + "Search for 'somecontent' should give the right number of results", 1, topDocs.totalHits);
        Document doc = reader.document(topDocs.scoreDocs[0].doc);

        assertEquals(M + "The stored value for the document should be correct", SINGLE_CONTENT, doc.get(SINGLE));
        assertEquals(M + "The stored long value for the document should be correct",
                     Long.toString(LONG_CONTENT), doc.get(LONG));
        for (String value: Arrays.asList(MULTI_CONTENT_1, MULTI_CONTENT_2)) {
            assertTrue("The value " + value + " should be stored in field " + MULTI,
                       Arrays.asList(doc.getValues(MULTI)).contains(value));
        }

/*        assertEquals(M + "The stored double value for the document should be correct",
                     Double.toString(DOUBLE_CONTENT), doc.get(DOUBLE));
        assertEquals(M + "The stored float value for the document should be correct",
                     Double.toString(FLOAT_CONTENT), doc.get(FLOAT));*/

        String dv = getSortedDocValue(reader, topDocs.scoreDocs[0].doc, DV);
        assertEquals("The plain DocValues content for the document should be correct", DV_CONTENT, dv);

        // SINGLE (single value String)
        try {
            String nonexistingDV = getSortedDocValue(reader, topDocs.scoreDocs[0].doc, SINGLE);
            if (!dvExpected) {
                fail(M + "Requesting the DocValue from the non-DV field " + SINGLE
                     + " should have failed but returned " + nonexistingDV);
            }
            assertEquals("Requesting DV from a stored field should work due to the wrapper",
                         SINGLE_CONTENT, nonexistingDV);
        } catch (Exception e) {
            if (dvExpected) {
                fail(M + "There should have been a DV-value for field " + SINGLE);
            }
        }

        // LONG
        try {
            long nonexistingDV = getLongDocValue(reader, topDocs.scoreDocs[0].doc, LONG);
            if (!dvExpected) {
                fail(M + "Requesting the DocValue from the non-DV field " + LONG
                     + " should have failed but returned " + nonexistingDV);
            }
            assertEquals("Requesting DV from a stored field should work due to the wrapper",
                         LONG_CONTENT, nonexistingDV);
        } catch (Exception e) {
            if (dvExpected) {
                fail(M + "There should have been a DV-value for field " + LONG);
            }
        }

        // MULTI (multi value String)
        try {
            List<String> dvs = getSortedSetDocValues(reader, topDocs.scoreDocs[0].doc, MULTI);
            if (!dvExpected) {
                fail(M + "Requesting the SortedSet DocValues from the non-DV field " + SINGLE
                     + " should have failed but returned " + dvs);
            }
            assertEquals("The number of returned DVs for field " + MULTI + " should match",
                         2, dvs.size());
            for (String value: Arrays.asList(MULTI_CONTENT_1, MULTI_CONTENT_2)) {
                assertTrue("The value " + value + " should be DocValued in field " + MULTI,
                           dvs.contains(value));
            }
        } catch (Exception e) {
            if (dvExpected) {
                fail(M + "There should have been a DV-value for field " + MULTI);
            }
        }

    }

    private static List<String> getSortedSetDocValues(IndexReader reader, int docID, String field) throws IOException {
        if (!reader.getContext().isTopLevel) {
            throw new IllegalStateException("Expected the reader to be topLevel");
        }
        for (AtomicReaderContext atom: reader.getContext().leaves()) {
            if (atom.docBase <= docID && atom.docBase + atom.reader().maxDoc() > docID) {
                return getSortedSetDocValues(atom, docID, field);
            }
        }
        throw new IllegalArgumentException("The docID " + docID + " exceeded the index size");
    }
    private static List<String> getSortedSetDocValues(
            AtomicReaderContext atomContext, int docID, String field) throws IOException {
        SortedSetDocValues dvs = atomContext.reader().getSortedSetDocValues(field);
        if (dvs == null) {
            throw new IllegalStateException("No SortedSetDocValues for field '" + field + "'");
        }
        dvs.setDocument(docID);
        List<String> values = new ArrayList<String>();
        BytesRef result = new BytesRef();
        long ord;
        while ((ord = dvs.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS) {
            dvs.lookupOrd(ord, result);
            values.add(result.utf8ToString());
        }
        return values;
    }

    private static String getSortedDocValue(IndexReader reader, int docID, String field) throws IOException {
        if (!reader.getContext().isTopLevel) {
            throw new IllegalStateException("Expected the reader to be topLevel");
        }
        for (AtomicReaderContext atom: reader.getContext().leaves()) {
            if (atom.docBase <= docID && atom.docBase + atom.reader().maxDoc() > docID) {
                return getSortedDocValue(atom, docID, field);
            }
        }
        throw new IllegalArgumentException("The docID " + docID + " exceeded the index size");
    }
    private static String getSortedDocValue(
            AtomicReaderContext atomContext, int docID, String field) throws IOException {
        SortedDocValues dvs = atomContext.reader().getSortedDocValues(field);
        if (dvs == null) {
            throw new IllegalStateException("No SortedDocValues for field '" + field + "'");
        }
        BytesRef result = new BytesRef();
        dvs.get(docID-atomContext.docBase, result);
        return result.utf8ToString();
    }

    private double getDoubleDocValue(IndexReader reader, int docID, String field) throws IOException {
        if (!reader.getContext().isTopLevel) {
            throw new IllegalStateException("Expected the reader to be topLevel");
        }
        for (AtomicReaderContext atom: reader.getContext().leaves()) {
            if (atom.docBase <= docID && atom.docBase + atom.reader().maxDoc() > docID) {
                return getDoubleDocValue(atom, docID, field);
            }
        }
        throw new IllegalArgumentException("The docID " + docID + " exceeded the index size");
    }
    private static Double getDoubleDocValue(AtomicReaderContext atomContext, int docID, String field)
            throws IOException {
        NumericDocValues dvs = atomContext.reader().getNumericDocValues(field);
        if (dvs == null) {
            throw new IllegalStateException("No NumericDocValues for field '" + field + "'");
        }
        return NumericUtils.sortableLongToDouble(dvs.get(docID));
    }

    private static long getLongDocValue(IndexReader reader, int docID, String field) throws IOException {
        if (!reader.getContext().isTopLevel) {
            throw new IllegalStateException("Expected the reader to be topLevel");
        }
        for (AtomicReaderContext atom: reader.getContext().leaves()) {
            if (atom.docBase <= docID && atom.docBase + atom.reader().maxDoc() > docID) {
                return getLongDocValue(atom, docID, field);
            }
        }
        throw new IllegalArgumentException("The docID " + docID + " exceeded the index size");
    }
    private static Long getLongDocValue(AtomicReaderContext atomContext, int docID, String field) throws IOException {
        NumericDocValues dvs = atomContext.reader().getNumericDocValues(field);
        if (dvs == null) {
            throw new IllegalStateException("No NumericDocValues for field '" + field + "'");
        }
        return dvs.get(docID);
    }

    // Semi.random index with stored fields only: multi, single and long. Some values missing from some documents
    private static File generateIndex(int documents) throws IOException {
        final File INDEX = new File("target/testindex.deletefreely." + documents);
        final long seed = new Random().nextLong();
        Random random = new Random(seed);
        log.info("Testing with random seed" + seed);
        Analyzer analyzer = new StandardAnalyzer(LUCENE_VERSION);

        final FieldType SINGLE_F = new FieldType();
        SINGLE_F.setIndexed(true);
        SINGLE_F.setStored(true);

        final FieldType MULTI_F = new FieldType();
        MULTI_F.setIndexed(true);
        MULTI_F.setStored(true);

        final FieldType SEARCH_F = new FieldType();
        SEARCH_F.setIndexed(true);

        final FieldType LONG_F = new FieldType();
        LONG_F.setIndexed(true);
        LONG_F.setStored(true);
        LONG_F.setNumericType(FieldType.NumericType.LONG);

        final FieldType DOUBLE_F = new FieldType();
        DOUBLE_F.setIndexed(true);
        DOUBLE_F.setStored(true);
        DOUBLE_F.setNumericType(FieldType.NumericType.DOUBLE);

        IndexWriter indexWriter = new IndexWriter(
                MMapDirectory.open(INDEX), new IndexWriterConfig(LUCENE_VERSION, analyzer));
        for (int docID = 0 ; docID < documents ; docID++) {
            Document document = new Document();
            document.add(new Field(ID, Integer.toString(docID), SINGLE_F));
            document.add(new Field(SEARCH, SEARCH_CONTENT + "_" + docID, SEARCH_F));
            if (random.nextInt(5) > 0) {
                document.add(new Field(SINGLE, SINGLE_CONTENT + "_r" + random.nextInt(), SINGLE_F));
            }
            if (random.nextInt(5) > 0) {
                document.add(new Field(MULTI, MULTI_CONTENT_1 + "_" + docID, MULTI_F));
                if (random.nextInt(3) > 0) {
                    document.add(new Field(MULTI, MULTI_CONTENT_2 + "_random" + random.nextInt(5), MULTI_F));
                }
            }
            if (random.nextInt(5) > 0) {
                document.add(new LongField(LONG, random.nextLong(), LONG_F));
            }
            if (random.nextInt(5) > 0) {
                document.add(new DoubleField(DOUBLE, random.nextDouble(), DOUBLE_F));
            }
            indexWriter.addDocument(document);
            if (docID == documents / 3) {
                indexWriter.commit(); // Ensure multi-segment
            }
        }
        indexWriter.commit();
        indexWriter.close();
        return INDEX;
    }

    public static File generateIndex() throws IOException {
        final File INDEX = new File("target/testindex.deletefreely");
        Analyzer analyzer = new StandardAnalyzer(LUCENE_VERSION);

        final FieldType SINGLE_F = new FieldType();
        SINGLE_F.setIndexed(true);
        SINGLE_F.setStored(true);

        final FieldType MULTI_F = new FieldType();
        MULTI_F.setIndexed(true);
        MULTI_F.setStored(true);

        final FieldType SEARCH_F = new FieldType();
        SEARCH_F.setIndexed(true);

        final FieldType LONG_F = new FieldType();
        LONG_F.setIndexed(true);
        LONG_F.setStored(true);
        LONG_F.setNumericType(FieldType.NumericType.LONG);

/*        final FieldType DOUBLE_F = new FieldType();
        DOUBLE_F.setIndexed(true);
        DOUBLE_F.setStored(true);
        DOUBLE_F.setNumericType(FieldType.NumericType.DOUBLE);

        final FieldType FLOAT_F = new FieldType();
        FLOAT_F.setIndexed(true);
        FLOAT_F.setStored(true);
        FLOAT_F.setNumericType(FieldType.NumericType.FLOAT);
  */

/*        final FieldType STR_DV = new FieldType();
        STR_DV.setIndexed(true);
        STR_DV.setStored(true);
        STR_DV.setDocValueType(FieldInfo.DocValuesType.SORTED);*/


        IndexWriter indexWriter = new IndexWriter(MMapDirectory.open(INDEX),
                                                  new IndexWriterConfig(LUCENE_VERSION, analyzer));
        {
            Document document = new Document();
            document.add(new Field(ID, "1", MULTI_F));
            document.add(new Field(SEARCH, SEARCH_CONTENT, SEARCH_F));
            document.add(new Field(SINGLE, SINGLE_CONTENT, MULTI_F));
            document.add(new Field(MULTI, MULTI_CONTENT_1, MULTI_F));
            document.add(new Field(MULTI, MULTI_CONTENT_2, MULTI_F));
            document.add(new LongField(LONG, LONG_CONTENT, LONG_F));
//            document.add(new DoubleField(DOUBLE, DOUBLE_CONTENT, DOUBLE_F));
//            document.add(new FloatField(FLOAT, FLOAT_CONTENT, FLOAT_F));
            document.add(new SortedDocValuesField(DV, new BytesRef(DV_CONTENT)));
            indexWriter.addDocument(document);
        }
        indexWriter.commit();
        indexWriter.close();
        return INDEX;
    }

    public static void delete(File path) {
        File[] subs = path.listFiles();
        if (subs != null) {
            for (File subPath: subs) {
                delete(subPath);
            }
        }
        if (!path.delete()) {
            log.warn("Unable to delete '" + path + "'");
        }
    }
}

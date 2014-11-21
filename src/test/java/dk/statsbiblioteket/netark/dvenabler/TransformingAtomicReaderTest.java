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

import junit.framework.TestCase;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;

public class TransformingAtomicReaderTest extends TestCase {
    private static Log log = LogFactory.getLog(TransformingAtomicReaderTest.class);

    private static final String ID = "id";
    private static final String DV = "dv";
    private static final String DV_CONTENT = "dvcontent";
    private static final String SEARCH = "search";
    private static final String SEARCH_CONTENT = "searchcontent";
    private static final String STORED = "stored";
    public static final String STORED_CONTENT = "plainstore";

    public void testCreateAndReadPlainIndex() throws IOException, ParseException {
        log.info("testCreateAndReadPlainIndex started");
        final File INDEX = generateIndex();
        Directory directory = MMapDirectory.open(INDEX);
        IndexReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);

        try {
            Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_48);
            QueryParser queryParser = new QueryParser(Version.LUCENE_48, SEARCH, analyzer);
            Query query = queryParser.parse(SEARCH_CONTENT);
            TopDocs topDocs = searcher.search(query, 10);
            assertEquals("Search for 'somecontent' should give the right number of results", 1, topDocs.totalHits);
            Document doc = reader.document(topDocs.scoreDocs[0].doc);
            assertEquals("The stored value for the document should be correct", STORED_CONTENT, doc.get(STORED));
            String dv = getSortedDocValue(reader, topDocs.scoreDocs[0].doc, DV);
            assertEquals("The DocValues content for the document should be correct", DV_CONTENT, dv);
            try {
                String nonexistingDV = getSortedDocValue(reader, topDocs.scoreDocs[0].doc, STORED);
                fail("Requesting the DocValue from the non-DV field " + STORED + " should have failed but returned "
                     + nonexistingDV);
            } catch (Exception e) {
                log.debug("Requesting non-existing DV gave an error as expected");
            }
        } finally {
            reader.close();
            delete(INDEX);
        }
    }

    public void testCreateAndReadWrappedIndex() throws IOException, ParseException {
        log.info("testCreateAndReadPlainIndex started");
        final File INDEX = generateIndex();
        Directory directory = MMapDirectory.open(INDEX);
        IndexReader reader = new TransformingDirectoryReader(DirectoryReader.open(directory));
        IndexSearcher searcher = new IndexSearcher(reader);

        try {
            Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_48);
            QueryParser queryParser = new QueryParser(Version.LUCENE_48, SEARCH, analyzer);
            Query query = queryParser.parse(SEARCH_CONTENT);
            TopDocs topDocs = searcher.search(query, 10);
            assertEquals("Search for 'somecontent' should give the right number of results", 1, topDocs.totalHits);
            Document doc = reader.document(topDocs.scoreDocs[0].doc);
            assertEquals("The stored value for the document should be correct", STORED_CONTENT, doc.get(STORED));
            String dv = getSortedDocValue(reader, topDocs.scoreDocs[0].doc, DV);
            assertEquals("The DocValues content for the document should be correct", DV_CONTENT, dv);
            String nonexistingDV = getSortedDocValue(reader, topDocs.scoreDocs[0].doc, STORED);
            assertEquals("Requesting DV from a stored field should work due to the wrapper",
                         STORED_CONTENT, nonexistingDV);
        } finally {
            reader.close();
            delete(INDEX);
        }
    }

    private String getSortedDocValue(IndexReader reader, int docID, String field) throws IOException {
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

    private String getSortedDocValue(AtomicReaderContext atomContext, int docID, String field) throws IOException {
        SortedDocValues dvs = atomContext.reader().getSortedDocValues(field);
        if (dvs == null) {
            throw new IllegalStateException("No SortedDocValues for field '" + field + "'");
        }
        BytesRef result = new BytesRef();
        dvs.get(docID-atomContext.docBase, result);
        return result.utf8ToString();
    }

    public File generateIndex() throws IOException {
        final File INDEX = new File("testindex.deletefreely");
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_48);

        final FieldType STORED_F = new FieldType();
        STORED_F.setIndexed(true);
        STORED_F.setStored(true);

        final FieldType SEARCH_F = new FieldType();
        SEARCH_F.setIndexed(true);

/*        final FieldType STR_DV = new FieldType();
        STR_DV.setIndexed(true);
        STR_DV.setStored(true);
        STR_DV.setDocValueType(FieldInfo.DocValuesType.SORTED);*/


        IndexWriter indexWriter = new IndexWriter(MMapDirectory.open(INDEX),
                                                  new IndexWriterConfig(Version.LUCENE_48, analyzer));
        {
            Document document = new Document();
            document.add(new Field(ID, "1", STORED_F));
            document.add(new Field(SEARCH, SEARCH_CONTENT, SEARCH_F));
            document.add(new Field(STORED, STORED_CONTENT, STORED_F));
            document.add(new SortedDocValuesField(DV, new BytesRef(DV_CONTENT)));
            indexWriter.addDocument(document);
        }
        indexWriter.commit();
        indexWriter.close();
        return INDEX;
    }

    private void delete(File path) {
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

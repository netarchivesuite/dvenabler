package dk.statsbiblioteket.netark.dvenabler;

import java.io.File;
import java.io.IOException;
import java.util.*;

import dk.statsbiblioteket.netark.dvenabler.wrapper.DVDirectoryReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.store.MMapDirectory;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

public class IndexUtils {
    private static Log log = LogFactory.getLog(IndexUtils.class);

    // Last version with DocValuesFormat="disk"
    private static final Version LUCENE_VERSION = Version.LUCENE_48;
    private static final long M = 1000000;

    /**
     * Transform the index at source to the destination, adjusting DocValues for the given adjustFields underway.
     * @param source       the location of an existing index.
     * @param destination  where the adjusted index should be stored.
     * @param dvConfigs the fields to adjust. Use {@link #getDVConfigs(java.io.File)} to obtain the
     *                         original setup.
     */
    public static void convert(
            File source, File destination, Collection<DVConfig> dvConfigs) throws IOException {
        log.info("Converting index at " + source + " to " + destination + " with " + dvConfigs.size()
                 + " DocValues adjustment fields");
        final long startTime = System.nanoTime();

        DirectoryReader inner = DirectoryReader.open(MMapDirectory.open(source));
        final long afterInner = System.nanoTime();
        log.info("Opened standard reader(" + source + ") in " + (afterInner-startTime)/M + "ms");

        IndexReader dvReader = new DVDirectoryReader(inner, new HashSet<>(dvConfigs));
        final long afterWrapper = System.nanoTime();
        log.info("Opened DVWrapper(" + source + ") in " + (afterWrapper-afterInner)/M + "ms");

        Analyzer analyzer = new StandardAnalyzer(LUCENE_VERSION);
        IndexWriter writer = new IndexWriter(
                MMapDirectory.open(destination), new IndexWriterConfig(LUCENE_VERSION, analyzer));
        final long afterWriterCreation = System.nanoTime();
        log.info("Created writer(" + destination + ") in " + (afterWriterCreation-afterWrapper)/M + "ms");

        writer.addIndexes(dvReader);
        final long afterConversion = System.nanoTime();
        log.info("Converted index(" + destination + ") in " + (afterConversion-afterWriterCreation)/M + "ms");

        writer.commit();
        final long afterCommit = System.nanoTime();
        log.info("Finished commit(" + destination + ") in " + (afterCommit - afterCommit) / M + "ms");

        // No need for optimize as the addIndexes + commit ensures transformation
        writer.close();
        dvReader.close();
        log.info("All done. Total time " + (System.nanoTime() - startTime) / M + "ms");
    }

    /**
     * Extracts field information from the Lucene index at the given location.
     * </p><p>
     * Important: The {@link DVConfig#numericType} is always set to LONG as the auto-detection code
     * has not been implemented.
     * @param indexLocation where the index is stored.
     * @return a complete list of DocValues-relevant information on the fields contained in the index.
     * @throws IOException if the information could not be extracted.
     */
    public static List<DVConfig> getDVConfigs(File indexLocation) throws IOException {
        try (IndexReader reader = DirectoryReader.open(MMapDirectory.open(indexLocation))) {
            Map<String, DVConfig> dvConfigs = new HashMap<>();
            for (AtomicReaderContext context : reader.leaves()) {
                for (FieldInfo fieldInfo : context.reader().getFieldInfos()) {
                    if (dvConfigs.containsKey(fieldInfo.name)) {
                        continue;
                    }
                    String first = getFirst(context.reader(), fieldInfo.name);
                    dvConfigs.put(fieldInfo.name, new DVConfig(
                            fieldInfo,
                            fieldInfo.hasDocValues() && fieldInfo.getDocValuesType() == FieldInfo.DocValuesType.NUMERIC
                            ? FieldType.NumericType.LONG : null,
                            first));
                }
            }
            List<DVConfig> configs = new ArrayList<>(dvConfigs.values());
            Collections.sort(configs);
            return configs;
        }
    }

    private static String getFirst(AtomicReader reader, String field) throws IOException {
        Terms terms = reader.fields().terms(field);
        if (terms == null) {
            return "<No terms>";
        }
        BytesRef first = terms.iterator(null).next();
        return first == null ? "N/A" : first.utf8ToString();
    }

}

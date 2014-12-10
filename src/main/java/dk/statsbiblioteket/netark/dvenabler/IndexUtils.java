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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import dk.statsbiblioteket.netark.dvenabler.gui.SchemaField;
import org.apache.lucene.util.Version;

public class IndexUtils {
    private static Log log = LogFactory.getLog(IndexUtils.class);

    // Last version with DocValuesFormat="disk"
    private static final Version LUCENE_VERSION = Version.LUCENE_48;

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

        IndexReader dvReader = new DVDirectoryReader(
                DirectoryReader.open(MMapDirectory.open(source)), new HashSet<DVConfig>(dvConfigs));
        Analyzer analyzer = new StandardAnalyzer(LUCENE_VERSION);
        IndexWriter writer = new IndexWriter(
                MMapDirectory.open(destination), new IndexWriterConfig(LUCENE_VERSION, analyzer));

        writer.addIndexes(dvReader);
        writer.commit();
        // No need for optimize as the addIndexes + commit ensures transformation
        writer.close();
        dvReader.close();
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
        IndexReader reader = DirectoryReader.open(MMapDirectory.open(indexLocation));
        try {
            Map<String, DVConfig> dvConfigs = new HashMap<String, DVConfig>();
            for (AtomicReaderContext context : reader.leaves()) {
                for (FieldInfo fieldInfo : context.reader().getFieldInfos()) {
                    dvConfigs.put(fieldInfo.name, new DVConfig(fieldInfo, FieldType.NumericType.LONG));
                }
            }
            return new ArrayList<DVConfig>(dvConfigs.values());
        } finally {
            reader.close();
        }
    }

}

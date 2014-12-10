package dk.statsbiblioteket.netark.dvenabler;

import java.io.File;
import java.io.IOException;
import java.util.*;

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

    public static ArrayList<String> getAllFieldsFromIndex(String indexLocation) throws Exception{

        Directory directory = MMapDirectory.open(new File(indexLocation));
        IndexReader reader = DirectoryReader.open(directory);

        ArrayList<String> schemaFieldList= new ArrayList<String>();
        List<AtomicReaderContext> leaves = reader.leaves();
        for (AtomicReaderContext context : leaves) {
            AtomicReader atomicReader = context.reader();
            Fields fields = atomicReader.fields();
            for (String fieldName : fields) {
                System.out.println(fieldName);
                schemaFieldList.add(fieldName);
            }

        }
        return schemaFieldList;
    }


    public static  ArrayList<SchemaField> getAllFieldsInfoFromIndex(String indexLocation) throws Exception{

        Directory directory = MMapDirectory.open(new File(indexLocation));
        IndexReader reader = DirectoryReader.open(directory);

        ArrayList<SchemaField>  fieldInfoList= new  ArrayList<SchemaField> ();
        List<AtomicReaderContext> leaves = reader.leaves();
        for (AtomicReaderContext context : leaves) {
            AtomicReader atomicReader = context.reader();
            FieldInfos fieldInfos = atomicReader.getFieldInfos();
            for (FieldInfo fieldInfo : fieldInfos) {
                SchemaField f = new SchemaField();
                f.setName(fieldInfo.name);
                f.setStored(false); // kan ikke få info
                f.setType("(mangler type)");

                fieldInfoList.add(f);
            }
        }
        return fieldInfoList;
    }



    //TODO, implement. Maybe reuse the field objects from Lucene
    public static ArrayList<SchemaField> getFields(String indexLocation){

        ArrayList<SchemaField> fieldsList = new ArrayList<SchemaField>();

        SchemaField f1 = new SchemaField();
        f1.setDocVal(false);
        f1.setStored(true);
        f1.setName("field_name1");
        f1.setType("String");
        fieldsList.add(f1);

        SchemaField f2 = new SchemaField();
        f2.setDocVal(false);
        f2.setStored(false);
        f2.setName("field_name2");
        f2.setType("String");
        fieldsList.add(f2);

        SchemaField f3 = new SchemaField();
        f3.setDocVal(true);
        f3.setStored(true);
        f3.setName("field_name3");
        f3.setType("tint");
        fieldsList.add(f3);

        return fieldsList;

    }

}

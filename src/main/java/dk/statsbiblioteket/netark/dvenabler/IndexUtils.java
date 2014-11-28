package dk.statsbiblioteket.netark.dvenabler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

public class IndexUtils {


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
}

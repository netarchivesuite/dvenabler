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

import dk.statsbiblioteket.netark.dvenabler.gui.SchemaField;

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

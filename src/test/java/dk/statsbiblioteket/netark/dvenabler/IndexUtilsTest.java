package dk.statsbiblioteket.netark.dvenabler;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IndexUtilsTest extends TestCase {
  
    private static Log log = LogFactory.getLog(IndexUtilsTest.class);
        
    public void testReadFieldsFromIndex() throws Exception {
        
        IndexUtils.getAllFieldsFromIndex("/home/teg/Desktop/solr-4.7.0/example/solr/collection1/data/index");
        
        
        
    }
    
    
}

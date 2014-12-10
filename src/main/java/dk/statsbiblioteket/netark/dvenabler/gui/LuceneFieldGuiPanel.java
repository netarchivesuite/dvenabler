package dk.statsbiblioteket.netark.dvenabler.gui;

import java.awt.FlowLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import dk.statsbiblioteket.netark.dvenabler.DVConfig;

public class LuceneFieldGuiPanel extends JPanel{
 
    
    private static final long serialVersionUID = 1L;
    private JLabel fieldName;
    private JComboBox<DocValuesTypeGUI> docValTypesList;
     
    private DVConfig luceneField;
        
    public LuceneFieldGuiPanel(DVConfig luceneField){            
        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        setLayout(flowLayout);
          this.luceneField = luceneField;
        
            //setBorder(BorderFactory.createEmptyBorder(0,10,10,10));            
            docValTypesList= new JComboBox<DocValuesTypeGUI>(DocValuesTypeGUI.values());                                           
            docValTypesList.setSelectedIndex(0);
            fieldName = new JLabel(luceneField.getFieldInfo().name);                      
            if (luceneField.hasDocValues()){          
               docValTypesList.setSelectedIndex(1); //user has to pick the correct one
            }
            else{
                                
            }
            
            add(fieldName);                   
            add(docValTypesList);                  
    }
     
    public static enum DocValuesTypeGUI {       
        NO_DOCVAL,     
        NUMERIC_INT,       
        NUMERIC_LONG,
        NUMERIC_FLOAT,
        NUMERIC_DOUBLE,        
        BINARY,       
        SORTED_SINGLE_VALUE_STRING,       
        SORTED_SET_MULTIVALUE_STRING
      }
    
    
}

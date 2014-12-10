package dk.statsbiblioteket.netark.dvenabler.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;

import dk.statsbiblioteket.netark.dvenabler.DVConfig;
import dk.statsbiblioteket.netark.dvenabler.IndexUtils;


public class DvEnablerGui extends JFrame  {

    private static final long serialVersionUID = 1L;

    private ArrayList<JCheckBox> fieldsCheckBoxList;  //For checkbox to pick doc values fields. 	

    private static DvEnablerGui main; 

    //Menu
    JMenuBar  menuBar;
    JMenu     jMenu_about;
    JMenuItem menuItem_info;

    //Gui
    JButton fileButton = new JButton("Index");
    JButton buildButton = new JButton("Rebuild index");
    JLabel indexFileLabel = new JLabel("Index file:", JLabel.LEFT);
    JFileChooser chooser;
    JScrollPane checkBoxScrollPane;

    public static void main(String args[]) throws Exception {
        main = new DvEnablerGui();
        main.init();
        main.pack();
        main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        main.setVisible(true);
        main.setResizable(true);
    }


    public  void init() throws Exception{
        createMenu();
        createGui();
    }

    public void createGui() throws Exception {

        int row = 0;
        getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST; 
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = row; // row not finished
        gbc.gridheight = 1;
        gbc.gridwidth = 1;                          
        fileButton=  new JButton("Index");;
        fileButton.addActionListener(new IndexFolderActionListener());
        getContentPane().add(fileButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = row++;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;                          
        buildButton.setEnabled(false);
        buildButton.addActionListener(new IndexBuilderActionListener());
        getContentPane().add(buildButton, gbc);        

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridheight = 1;
        gbc.gridwidth = 2;                          
        getContentPane().add(indexFileLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridheight = 1;
        gbc.gridwidth = 2;                          

        Box box = Box.createVerticalBox(); //Empty before index is selected
        checkBoxScrollPane = new JScrollPane(box);
        checkBoxScrollPane.setPreferredSize(new Dimension(600, 300));
        getContentPane().add(checkBoxScrollPane, gbc);
    }


    public void createMenu(){
        menuBar = new JMenuBar();
        menuBar.setBorder(new BevelBorder(BevelBorder.RAISED));
        jMenu_about = new JMenu("About");
        menuItem_info = new JMenuItem("Info");
        jMenu_about.add(menuItem_info);
        menuItem_info.addActionListener(new HelpEvent());
        menuBar.add(jMenu_about);
        setJMenuBar(menuBar);
        setTitle("DocValue index build tool");
    }

    class HelpEvent implements ActionListener{

        public void actionPerformed(ActionEvent e){
            JOptionPane.showMessageDialog(main, "Another IT-WEB product.");
        }
    }


    public void showError(Throwable e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
    }


    class IndexFolderActionListener implements ActionListener{

        public void actionPerformed(ActionEvent e) {

            chooser = new JFileChooser(); 
            chooser.setCurrentDirectory(new java.io.File("."));
            chooser.setDialogTitle("Select index data folder.");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            //
            // disable the "All files" option.
            //
            chooser.setAcceptAllFileFilterUsed(false);

            int returnVal = chooser.showOpenDialog(null); 

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                String filePath= file.getAbsolutePath();
                indexFileLabel.setText("Index file:"+filePath);
                buildButton.setEnabled(true);
                System.out.println("selected file:"+filePath);

                createJCheckBoxes(filePath);
            }
        }

        private void createJCheckBoxes(String indexFolder){

            try{

                fieldsCheckBoxList  = new ArrayList<JCheckBox>();
                List<DVConfig> fieldInfoList = IndexUtils.getDVConfigs(new File(indexFolder));
                Collections.sort(fieldInfoList);

                Box box = Box.createVerticalBox();
                for (DVConfig current: fieldInfoList){				
                    LuceneFieldGuiPanel luceneFieldGui = new LuceneFieldGuiPanel(current); 			  

                    box.add(luceneFieldGui);
                }

                checkBoxScrollPane.add(box);      
                checkBoxScrollPane.setViewportView(box);    
                checkBoxScrollPane.repaint();

            }
            catch(Throwable ex){
                showError(ex);
            }

        }		
    }


    class IndexBuilderActionListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {        

            try{

                chooser.setCurrentDirectory(new java.io.File("."));
                chooser.setDialogTitle("Select index rebuild data folder.");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                //
                // disable the "All files" option.
                //
                chooser.setAcceptAllFileFilterUsed(false);

                int returnVal = chooser.showOpenDialog(null); 

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    String rebuildIndexfilePath= file.getAbsolutePath();

                    //Need an index method to call
                    for ( JCheckBox current : fieldsCheckBoxList){ //Find which fields has been marked for DocVal
                        if (current.isSelected()){
                            System.out.println(current.getText() +  " is marked for DocVal");
                        }
                    }

                }

            }
            catch(Exception ex){
                showError(ex);
            }
        }

    }

}

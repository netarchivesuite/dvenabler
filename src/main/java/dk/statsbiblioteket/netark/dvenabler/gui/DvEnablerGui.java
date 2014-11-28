package dk.statsbiblioteket.netark.dvenabler.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.border.BevelBorder;

public class DvEnablerGui extends JFrame  {
  
    private static final long serialVersionUID = 1L;

    private static DvEnablerGui main; 

    //Menu
    JMenuBar  menuBar;
    JMenu     jMenu_about;
    JMenuItem menuItem_info;

    //Gui
    JButton fileButton = new JButton("Index");
    JFileChooser chooser;
    String choosertitle;
    
    public static void main(String args[]) throws Exception {
        main = new DvEnablerGui();
   
        main.fileButton=  new JButton("Index");;
        main.fileButton.addActionListener(main. new IndexFolderActionListener());
        
        main.init();
        main.pack();
        main.setVisible(true);
        // main.setResizable(false);
    }
    
    
    public  void init() throws Exception{
        
        createMenu();
        createGui();
    }

    public void createGui() throws Exception {

        getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST; 
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 5, 5, 5);

        
        //Row start        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;                          
        getContentPane().add(fileButton, gbc);
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
    }

    class HelpEvent implements ActionListener{

        public void actionPerformed(ActionEvent e){
            JOptionPane.showMessageDialog(main, "Another IT-WEB product.");
        }
    }
    
    
    public void showError(Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
    }

    
    class IndexFolderActionListener implements ActionListener{
    
    public void actionPerformed(ActionEvent e) {
        int result;
            
        chooser = new JFileChooser(); 
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle(choosertitle);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        //
        // disable the "All files" option.
        //
        chooser.setAcceptAllFileFilterUsed(false);
    
        int returnVal = chooser.showOpenDialog(null); 
        
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            System.out.println("selected file:"+file.getAbsolutePath());
     
        }
        
    }
    
    }
    
}

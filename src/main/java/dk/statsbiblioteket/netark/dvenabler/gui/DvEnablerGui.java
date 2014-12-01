package dk.statsbiblioteket.netark.dvenabler.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

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


public class DvEnablerGui extends JFrame  {

	private static final long serialVersionUID = 1L;

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
		fileButton=  new JButton("Index");;
		fileButton.addActionListener(new IndexFolderActionListener());
		getContentPane().add(fileButton, gbc);

		//Row start        
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;                          
		buildButton.setEnabled(false);
		fileButton.addActionListener(new IndexBuilderActionListener());
		getContentPane().add(buildButton, gbc);        

		//Row start        
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridheight = 1;
		gbc.gridwidth = 2;                          
		getContentPane().add(indexFileLabel, gbc);


		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridheight = 1;
		gbc.gridwidth = 2;                          

		JCheckBox a = new JCheckBox("A");
		JCheckBox b = new JCheckBox("B");
		JCheckBox c = new JCheckBox("C");
		JCheckBox d = new JCheckBox("D");
		JCheckBox e = new JCheckBox("E");


		Box box = Box.createVerticalBox();
		box.add(a);
		box.add(b);
		box.add(c);
		box.add(d);               
		box.add(e);
		JScrollPane pane = new JScrollPane(box);
		pane.setPreferredSize(new Dimension(400, 300));
		getContentPane().add(pane, gbc);



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


	public void showError(Exception e) {
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
				indexFileLabel.setText("Index file:"+file.getAbsolutePath());
				buildButton.setEnabled(true);
				System.out.println("selected file:"+file.getAbsolutePath());


			}

		}

	}

	
	class IndexBuilderActionListener implements ActionListener{

		public void actionPerformed(ActionEvent e) {
           System.out.println("Todo build index");

		}

	}

	
}

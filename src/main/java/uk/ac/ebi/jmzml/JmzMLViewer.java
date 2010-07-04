/**
 * Created by IntelliJ IDEA.
 * User: martlenn
 * Date: 20-Jul-2009
 * Time: 15:50:30
 */
package uk.ac.ebi.jmzml;

import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.SkyKrupp;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;
import uk.ac.ebi.jmzml.gui.MzMLTab;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;
import java.awt.event.*;
import java.awt.*;
import uk.ac.ebi.jmzml.gui.HelpWindow;
import uk.ac.ebi.jmzml.gui.ProgressDialog;
import uk.ac.ebi.jmzml.gui.ProgressDialogParent;
/*
 * CVS information:
 *
 * $Revision$
 * $Date$
 */

/**
 * This class provides a basic viewer for jmzML spectra.
 *
 * @author Lennart Martens
 * @version $Id$
 */
public class JmzMLViewer extends JFrame implements ProgressDialogParent {

    /**
     * A reference to the last mzML file opened in the viewer. Defaults to
     * the user's home directory.
     */
    private String pathToLastOpenedFile = "user.home";

    /**
     * A small dialog containing a progress bar.
     */
    ProgressDialog progressDialog;

    private ArrayList<MzMLUnmarshaller> iUnmarshallers = null;
    private ArrayList<String> iFilenames = null;
    private JTabbedPane jtpMain = null;

    public JmzMLViewer() {
        this(null);
    }

    public JmzMLViewer(ArrayList<File> aMzMLFiles) {
        super("jmzML Viewer");
        this.initDisplay();
        if(aMzMLFiles == null) {
            this.loadMzmlFile(false);
        } else {
            iUnmarshallers = new ArrayList<MzMLUnmarshaller>();
            iFilenames = new ArrayList<String>();
            for (Iterator lFileIterator = aMzMLFiles.iterator(); lFileIterator.hasNext();) {
                File lFile = (File) lFileIterator.next();
                addUnmarshaller(new MzMLUnmarshaller(lFile), lFile.getName());
            }
        }
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                terminate(0);
            }
        });
    }

    public static void main(String[] args) {
        ArrayList<File> mzMLFiles = new ArrayList<File>();
        if(args != null) {
            for (int i = 0; i < args.length; i++) {
                String inFile = args[i];
                File inputFile = new File(args[i]);
                if(!inputFile.exists()) {
                    System.err.println("\n\n  ** The mzML file you specified ('" + inFile + "') does not exist!\n     Skipping file...\n");
                }
                if(inputFile.isDirectory()) {
                    System.err.println("\n\n  ** The mzML file you specified ('" + inFile + "') is a folder, not a file!\n     Skipping file...\n");
                }
                mzMLFiles.add(inputFile);
                System.out.println("Using file "+inputFile.getName());
            }
        }

        // Set the Java Look and Feel
        try {
            PlasticLookAndFeel.setPlasticTheme(new SkyKrupp());
            UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        JmzMLViewer jmzMLViewer = null;
        if(mzMLFiles == null || mzMLFiles.isEmpty()) {
            jmzMLViewer = new JmzMLViewer();
        } else {
            jmzMLViewer = new JmzMLViewer(mzMLFiles);
        }
        int width = Toolkit.getDefaultToolkit().getScreenSize().width;
        int height = Toolkit.getDefaultToolkit().getScreenSize().height;
        jmzMLViewer.setBounds(50,50, width-250, height-250);

        jmzMLViewer.setLocationRelativeTo(null);
        jmzMLViewer.setVisible(true);
    }

    private void initDisplay() {
        // Add menubar.
        this.addMenuBar();
        // View consists of multiple tabs.
        jtpMain = new JTabbedPane(JTabbedPane.TOP);

        this.getContentPane().add(jtpMain, BorderLayout.CENTER);
    }

    private void addMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem openItem = new JMenuItem("Open...", KeyEvent.VK_O);
        openItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, 
                java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        openItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadMzmlFile(true);
            }
        });

        JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_E);
        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                terminate(0);
            }
        });
        
        fileMenu.add(openItem);
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem aboutItem = new JMenuItem("About", KeyEvent.VK_A);
        aboutItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openHelpDialog("/helpfiles/AboutJmzML.html");
            }
        });
        JMenuItem helpItem = new JMenuItem("Help", KeyEvent.VK_H);
        helpItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        helpItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openHelpDialog("/helpfiles/JmzMLViewer.html");
            }
        });

        helpMenu.add(helpItem);
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);

        this.getContentPane().add(menuBar, BorderLayout.NORTH);
    }

    /**
     * Open help dialog
     */
    private void openHelpDialog(String urlAsString){
        new HelpWindow(this, getClass().getResource(urlAsString));
    }

    /**
     * This method loads an mzML file from disk.
     *
     * @param addTab    boolean to indicate whether the loaded file
     *                  should be added to the existing tabs, or
     *                  whether it should replace all existing tabs.
     */
    private void loadMzmlFile(boolean addTab) {
        JFileChooser jfc = new JFileChooser(pathToLastOpenedFile);
        jfc.setDialogTitle("Select mzML file to view...");
        jfc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                boolean result = false;
                if(f.isDirectory() || f.getName().toLowerCase().endsWith(".mzml") || f.getName().toLowerCase().endsWith(".xml")) {
                    result = true;
                }
                return result;
            }

            @Override
            public String getDescription() {
                return ".mzml or .xml";
            }
        });
        jfc.setDialogType(JFileChooser.OPEN_DIALOG);
        int result = jfc.showOpenDialog(this);
        if(result == JFileChooser.CANCEL_OPTION) {
            if(addTab) {
                return;
            } else {
                terminate(0);
            }
        } else {
            final File selected = jfc.getSelectedFile();
            pathToLastOpenedFile = selected.getAbsolutePath();
            if(!addTab) {
                iUnmarshallers = new ArrayList<MzMLUnmarshaller>();
                iFilenames = new ArrayList<String>();
            }
            // @TODO Add validation code!

            progressDialog = new ProgressDialog(this, this, true);
            progressDialog.setIntermidiate(true);

            new Thread(new Runnable() {

                    @Override
                    public void run() {
                        progressDialog.setIntermidiate(true);
                        progressDialog.setTitle("Opening mzML File. Please Wait...");
                        progressDialog.setVisible(true);
                    }
                }, "ProgressDialog").start();

            new Thread("SearchThread") {

                @Override
                public void run() {
                    addUnmarshaller(new MzMLUnmarshaller(selected), selected.getName());
                    progressDialog.setVisible(false);
                    progressDialog.dispose();
                }
            }.start();
        }
    }

    private void addUnmarshaller(MzMLUnmarshaller aUnmarshaller, String aFilename) {
        iUnmarshallers.add(aUnmarshaller);
        iFilenames.add(aFilename);
        jtpMain.addTab(aFilename, new MzMLTab(this, aUnmarshaller));
    }

    public void seriousProblem(String aMessage, String aTitle) {
        JOptionPane.showMessageDialog(this, new String[] {aMessage, "", "Quitting application"}, aTitle, JOptionPane.ERROR_MESSAGE);
        terminate(1);
    }

    private void terminate(int aStatus) {
        this.setVisible(false);
        this.dispose();
        System.exit(aStatus);
    }

    /**
     * Cancels the opening of a jmzML file and closes the tool.
     */
    public void cancelProgress() {
        terminate(0);
    }
}

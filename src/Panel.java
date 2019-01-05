import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import org.apache.commons.io.FileUtils;

/**
 * Created by Tyler Brient
 */
public class Panel extends JPanel implements ActionListener {

    private int whatToDraw = -1; //This corresponds to the index of the thing to draw in the JComboBox, and is used to indicate paintComponent to draw the graphs. -1 is nothing.

    //ArrayLists for Values
    //All
    private ArrayList<int[]> AlldiffCounts = new ArrayList<int[]>(); //0 is the All Int Array, everything >0 is another functions int array.
    private ArrayList<String> AllfunctionsIn = new ArrayList<String>(); //0 is "All", after that, it is a list of functions names corresponding to AlldiffCounts
    //Only UI Events
    private ArrayList<int[]> UIdiffCounts = new ArrayList<>();
    private ArrayList<String> UIfunctionsIn = new ArrayList<>();
    //Exclude UI Events
    private ArrayList<int[]> excludeUIdiffCounts = new ArrayList<>();
    private ArrayList<String> excludeUIfunctionsIn = new ArrayList<>();
    //Main
    private ArrayList<int[]> MaindiffCounts = new ArrayList<>();
    private ArrayList<String> MainfunctionsIn = new ArrayList<>();
    //Greater than 10
    private long[] timeSplits;
    private int[] timeSplitCounts;

    private JComboBox dropDown;
    private MyComboBoxRenderer comboBoxRenderer = new MyComboBoxRenderer();
    private JFrame f; //The JFrame for the Loading Bar
    private JFrame List10; //The Extra JFrame that lists the 10+ second functions
    private JFrame timeHistogram;
    private JProgressBar progressBar; //The Loading Bar
    private TitledBorder border; //The Border around the Loading Bar
    private int unzipCounter = 0; //This counts the number of Zip Files to display the correct amount on the loading bar
    private Rectangle graphRect; //The Rectangle to tell where the screenshot should be taken
    private JButton screenshotButton = new JButton("Take a Screenshot");
    private int selectedFuncIndex; //This is the index of the selected function on the dropdown.
    private JList list;
    private DefaultListModel listModel = new DefaultListModel();
    private JScrollPane scroll;
    private ArrayList<Integer> AllTenPlusIndexes = new ArrayList<Integer>();
    private ArrayList<Integer> UITenPlusIndexes = new ArrayList<Integer>();
    private ArrayList<Integer> NotUITenPlusIndexes = new ArrayList<Integer>();
    private Graph graph = new Graph();

    private ArrayList<String> whitelistedFunctions = new ArrayList<>();
    private static final String WhitelistLoc = "whitelist.txt";
    private static final String UnzipLoc = "UnzippedFileDestination/Unzipped";
    private static final String allString = "All";
    private static final String onlyUIString = "Only UI Events";
    private static final String excludeUIString = "Exclude UI Events";

    private JRadioButton allButton = new JRadioButton(allString);
    private JRadioButton onlyUIEvent = new JRadioButton(onlyUIString);
    private JRadioButton excludeUIEvent = new JRadioButton(excludeUIString);
    private ButtonGroup UIButtonGroup = new ButtonGroup();

    private ArrayList<String> MSLines = new ArrayList<String>(); //The arraylist of all of the lines in all of the files that contain "milliseconds"


    public Panel(){
        //Set Action Commands JRadioButtons
        allButton.setSelected(true);
        allButton.setActionCommand(allString);
        onlyUIEvent.setActionCommand(onlyUIString);
        excludeUIEvent.setActionCommand(excludeUIString);
        //Group JRadio Buttons
        UIButtonGroup.add(allButton);
        UIButtonGroup.add(onlyUIEvent);
        UIButtonGroup.add(excludeUIEvent);
        //Initialize Action Listeners for Radio Buttons
        allButton.addActionListener(this);
        onlyUIEvent.addActionListener(this);
        excludeUIEvent.addActionListener(this);

//        graphRect = new Rectangle(0, 115, 800,675); //Bounds for the screenshot

        //Loading Screen Code:
        f = new JFrame("Parsing Files, Please Wait...");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container content = f.getContentPane();
        progressBar = new JProgressBar();
        progressBar.setValue(0); //Value of Progress Bar starts at Zero
        progressBar.setStringPainted(true);
        border = BorderFactory.createTitledBorder("Parsing Files, Please Wait...");
        progressBar.setBorder(border);
        content.add(progressBar, BorderLayout.NORTH);
        f.setSize(300, 100);

        List10 = new JFrame(">10 Second Functions");
        List10.setBounds(855, 0, 300, 500);
        List10.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        timeHistogram = new JFrame(">10 Second Functions Frequency");
        timeHistogram.setBounds(0, 0, 855, 800); //(x, y, w, h)
        timeHistogram.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        AllfunctionsIn.add(0, "All"); //Makes the first entry in AllfunctionsIn "All"
        AlldiffCounts.add(new int[9]); //0 is < 0.5, 1 is 0.5-1, 2 is 1-1.5, 3 is 1.5-2, 4 is 2-3, 5 is 3-4, 6 is 4-5, 7 is 5-10, 8 is >10

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel"); //Set the look and feel to a windows type look and feel.
        } catch (Exception e) {
            e.printStackTrace();
        }
        //File Chooser Code:
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Zip Log Files", "zip"));
        fileChooser.setMultiSelectionEnabled(true); //Allows for multiple zip files to be selected.
        fileChooser.showOpenDialog(this);
        final File[] selectedFiles = fileChooser.getSelectedFiles();

        makeDirectories();

        whitelistedFunctions.addAll(readWhiteList());

        for (int i = 0; i < selectedFiles.length; i++) {
            MSLines.addAll(unzip(selectedFiles[i].getAbsolutePath(), selectedFiles.length)); //Adds all Lines containing "milliseconds" to MSLines. Unzip returns an ArrayList of Strings
        }

        generateTimeChunks();

        //Screenshot Button Code
//        screenshotButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent event) {
//                try {
//                    Robot robot = new Robot();
//                    BufferedImage image = robot.createScreenCapture(graphRect); //Capture the screen
//                    String midname = selectedFiles[0].toString().split("\\\\")[4];
//
//                    File outputfile = new File("Output/" + midname + "/HistogramOf" + AllfunctionsIn.get(selectedFuncIndex) + ".jpg"); //File will always be in the Output Folder, under the folder of the time period, called HistogramOf(FunctionName).jpg
//                    outputfile.getParentFile().mkdirs(); //Make sure that the file folder is there, and if not, create it.
//                    ImageIO.write(image, "jpg", outputfile); //Write the bufferedImage
//
//                } catch (Exception e) {
//                    System.out.println("Screenshot capturing failed");
//                }
//            }
//        });
        makeArrayLists();
        sortAlphabetically();
        sortSlowest();
        String[] final10List = new String[AllTenPlusIndexes.size()+2];
        final10List[0] = "# of Instances | Name";
        for (int i = 0; i < AllTenPlusIndexes.size(); i++) {
            final10List[i+1] =  AlldiffCounts.get(AllTenPlusIndexes.get(i))[8] + ": " + AllfunctionsIn.get(AllTenPlusIndexes.get(i));
        }
        MaindiffCounts.addAll(AlldiffCounts);
        MainfunctionsIn.addAll(AllfunctionsIn);
        setColors();
        printSlowest();
        buildUIEventArrayLists();


        //printFunctions();

        for(String ListEntry: final10List) {
            listModel.addElement(ListEntry);
        }
        list = new JList(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);


        scroll = new JScrollPane(list);
        List10.add(scroll);
        f.dispose(); //removes the window that the loading bar is in.
        dropDown = new JComboBox(MainfunctionsIn.toArray()); //Creates dropdown with the AllfunctionsIn arrayList
        dropDown.setMaximumRowCount(20); //Makes it so you can see 20 items before using a scroll bar.
        dropDown.setRenderer(comboBoxRenderer); //Makes the renderer a custom renderer, allowing the use of custom colors.

        //Grid Layout for Radio Buttons
        JPanel radioPanel = new JPanel(new GridLayout(0,1));
        radioPanel.add(allButton);
        radioPanel.add(excludeUIEvent);
        radioPanel.add(onlyUIEvent);
        add(radioPanel, BorderLayout.LINE_START);



        add(dropDown);
//        add(screenshotButton);
        whatToDraw = 0; //Initially draw the "All" graph
        graph.calculateGraph(whatToDraw, MaindiffCounts, MSLines);
        dropDown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                JComboBox combo = (JComboBox) event.getSource();
                selectedFuncIndex = combo.getSelectedIndex(); //gets the selected index once clicked
                whatToDraw = selectedFuncIndex; //Sets "whatToDraw" to the index, so it will draw the selected graph.
                if (whatToDraw != -1) {
                    graph.calculateGraph(whatToDraw, MaindiffCounts, MSLines);
                }
                repaint();
            }
        });
        List10.setVisible(true);
    }

    private Date formatLogDate(String dateString) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); //FORMAT: 2017/08/10 08:04:09
        try {
            return formatter.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void generateTimeChunks() {
        //Get the Lines to Determine the start/end Time and Dates
        String firstLine = MSLines.get(0);
        String lastLine = MSLines.get(MSLines.size()-1);

        //Parse the two lines by splitting them at "|" and taking the third piece of the spit.
        firstLine = firstLine.split(" \\| ")[2];
        lastLine = lastLine.split(" \\| ")[2];

        System.out.println("First: " + firstLine);
        System.out.println("Last: " + lastLine);
        Date parsedFirstDate = formatLogDate(firstLine);
        Date parsedSecondDate = formatLogDate(lastLine);
        System.out.println("First: " + parsedFirstDate);
        System.out.println("Last: " + parsedSecondDate);
        System.out.println("First: " + parsedFirstDate.getTime());
        System.out.println("Last: " + parsedSecondDate.getTime());
        long differenceInMS = parsedSecondDate.getTime() - parsedFirstDate.getTime();
        System.out.println("Difference: " + differenceInMS);
        long differenceInMins = differenceInMS / (60 * 1000) % 60;
        final long minsInMS15 = 15 * 60 * 1000;
        timeSplits = new long[(int)differenceInMins % 15];
        timeSplitCounts = new int[timeSplits.length];
        long firstDateTime = parsedFirstDate.getTime();
        for (int i = 0; i < timeSplits.length; i++) {
            timeSplits[i] = firstDateTime + minsInMS15 * i;
        }

    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        if (whatToDraw != -1) {
            graph.draw(g2, MaindiffCounts);
        }
    }

    public void printFunctions() {
        for (int i = 0; i < AllfunctionsIn.size(); i++) {
            System.out.println(AllfunctionsIn.get(i));
        }
    }

    public void makeDirectories(){
        File outputDir = new File(UnzipLoc);
        try {
            outputDir.mkdirs();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("unable to make unzipped file directory");
        }
        File whitelistTxt = new File(WhitelistLoc);

        try {
            whitelistTxt.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("unable to make whitelist.txt");
        }
    }

    public void buildUIEventArrayLists(){
        UIfunctionsIn.add("All UIEvents");
        UIdiffCounts.add(new int[9]);
        excludeUIfunctionsIn.add("All except UIEvents");
        excludeUIdiffCounts.add(new int[9]);
        for (int i = 1; i < AllfunctionsIn.size(); i++) {
            if (AllfunctionsIn.get(i).contains("UIEvent")) {
                UIfunctionsIn.add(AllfunctionsIn.get(i));
                UIdiffCounts.add(AlldiffCounts.get(i));
            } else {
                excludeUIfunctionsIn.add(AllfunctionsIn.get(i));
                excludeUIdiffCounts.add(AlldiffCounts.get(i));
            }
        }
        for (int i = 1; i < UIdiffCounts.size(); i++) {
            for (int j = 0; j < 9; j++) {
                UIdiffCounts.get(0)[j] = UIdiffCounts.get(0)[j] + UIdiffCounts.get(i)[j];
            }
        }
        for (int i = 1; i < excludeUIdiffCounts.size(); i++) {
            for (int j = 0; j < 9; j++) {
                excludeUIdiffCounts.get(0)[j] = excludeUIdiffCounts.get(0)[j] + excludeUIdiffCounts.get(i)[j];
            }
        }

    }


    public ArrayList<String> GetLines(String Location) {
        int i;
        FileInputStream fin;
        ArrayList<String> finalLines = new ArrayList<String>();

        try {
            fin = new FileInputStream(Location); //Selects the location where the unzipped files would be.
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            return null;
        }
        int wordCount = 0;

        try {
            do {
                finalLines.add(""); //set the first one to "" to avoid an index out of bounds exception
                finalLines.set(wordCount, "");
                i = fin.read(); //Read the first character
                while (i != -1 && i != 10) { //makes sure that there isn't a line break or an end of document
                    finalLines.set(wordCount, finalLines.get(wordCount) + Character.toString((char) i)); //Adds this character to the selected index of the arrayList
                    i = fin.read();
                }
                if (!finalLines.get(wordCount).contains("milliseconds")) { //Checks to see if the string doesn't contain word count. If it doesn't, remove it from the array.
                    finalLines.remove(wordCount);
                    wordCount--; //Make index go back down to avoid skipping an index
                }
                wordCount++; //go to the next index in the arrayList.

            } while (i != -1); //-1 is end of document.
        } catch (IOException e) {
            System.out.println("Error reading file");
        }
        try {
            fin.close();
        } catch (IOException e) {
            System.out.println("Error Closing File");
        }
        return finalLines; //Returns the array of lines containing Milliseconds
    }

    public ArrayList<String> readWhiteList() {
        int i;
        FileInputStream fin;
        ArrayList<String> finalLines = new ArrayList<String>();

        try {
            fin = new FileInputStream(WhitelistLoc); //Selects the location where the unzipped files would be.
        } catch (FileNotFoundException e) {
            System.out.println("Whitelist not found");
            return null;
        }
        int wordCount = 0;

        try {
            do {
                finalLines.add(""); //set the first one to "" to avoid an index out of bounds exception
                finalLines.set(wordCount, "");
                i = fin.read(); //Read the first character
                while (i != -1 && i != 10) { //makes sure that there isn't a line break or an end of document
                    if (i != 13) {
                        finalLines.set(wordCount, finalLines.get(wordCount) + Character.toString((char) i)); //Adds this character to the selected index of the arrayList
                    }
                    i = fin.read();
                }
                wordCount++; //go to the next index in the arrayList.

            } while (i != -1); //-1 is end of document.
        } catch (IOException e) {
            System.out.println("Error reading file");
        }
        try {
            fin.close();
        } catch (IOException e) {
            System.out.println("Error Closing File");
        }
        return finalLines; //Returns the array of lines from whitelist
    }




    public ArrayList<String> unzip(String Source, int fileNum){


        unzipCounter++;
        if (fileNum > 1) {
            border.setTitle("Parsing Zip Number " + unzipCounter + "..."); //Progress bar title for multiple zips.
            progressBar.setValue(0);
        }
        f.setVisible(true);


        this.deleteFiles(); //Deletes all the files in the unzippedFileDestination folder.

        String source = Source;

        String destination = UnzipLoc;

        ArrayList<String> finalArrayList = new ArrayList<String>();



        try { //Unzips the files using zip4j
            ZipFile zipFile = new ZipFile(source);
            zipFile.extractAll(destination);
        } catch (ZipException e) {
            e.printStackTrace();
        }

        File[] filesInFolder = new File(UnzipLoc).listFiles();
        for (int i = 0; i < filesInFolder.length; i++) {
            System.out.print(filesInFolder[i] + ", ");
        }
        System.out.println("]");
        Arrays.sort(filesInFolder, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                int n1 = getNumber(o1.getName());
                int n2 = getNumber(o2.getName());
                return n2 - n1;
            }

            private int getNumber(String name) {
                String[] number = name.split("\\.");
                if (number.length > 2) {
                    return Integer.parseInt(number[2]);
                } else {
                    return 0;
                }
            }
        });
        for (int i = 0; i < filesInFolder.length; i++) {
            System.out.print(filesInFolder[i] + ", ");
        }
        System.out.println("]");
        int numberOfFiles = filesInFolder.length;

        filesInFolder = checkDirectoryForFolder(numberOfFiles, filesInFolder);
        //System.out.println(filesInFolder.length);
        numberOfFiles =  filesInFolder.length;


        for (int i = 0; i < filesInFolder.length; i++) { //could be a foreach loop, but they are ugly...
            border.setTitle("Parsing Log Number " + (i + 1) + "/" + filesInFolder.length + "..."); //Progress bar title for multiple zips.
            finalArrayList.addAll(GetLines(filesInFolder[i].getAbsolutePath()));
            progressBar.setValue((int) (((i + 1) / ((double) (numberOfFiles))) * 100)); //Progress bar stuff, makes the progress bar jump every time a file finishes
            System.out.println((i+1) + " Text File(s) Done");
            System.out.println(filesInFolder[i].getName() + " Finished");
            repaint();
        }

        return finalArrayList;
    }

    public File[] checkDirectoryForFolder(int numberOfFiles, File[] filesInFolder){
        if (filesInFolder[0].isDirectory()) {
            numberOfFiles = new File(filesInFolder[0].getAbsolutePath()).listFiles().length;
            filesInFolder = new File(filesInFolder[0].getAbsolutePath()).listFiles();
            return checkDirectoryForFolder(numberOfFiles, filesInFolder);
        } else {
            return filesInFolder;
        }

    }

    //This function deletes all of the files in the unzipped directory.
    //This is necessary because if there are files that aren't overwritten, it will cause those old files to be read.
    public void deleteFiles(){
        try {
            FileUtils.cleanDirectory(new File("UnzippedFileDestination/Unzipped"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }



    public boolean isFuncWithWhitelist(String function){
        for (int i = 0; i < whitelistedFunctions.size(); i++) {
            if (function.equalsIgnoreCase(whitelistedFunctions.get(i))) {
                return true;
            }
        }
        return false;
    }

    public void checkAndAdd(int number, String function, String date){ //This function takes in the number of milliseconds, previously parsed, and the
        if (!isFuncWithWhitelist(function)) {
            if (number < 500) {                               //function name. Then it checks the number of milliseconds to see what category it
                AlldiffCounts.get(0)[0] = AlldiffCounts.get(0)[0] + 1;//falls into. After it knows that, it adds one instane to the "All" category, and to
                addFunction(0, function);                     //Its repective function.
            } else if (number < 1000) {
                AlldiffCounts.get(0)[1] = AlldiffCounts.get(0)[1] + 1;
                addFunction(1, function);
            } else if (number < 1500) {
                AlldiffCounts.get(0)[2] = AlldiffCounts.get(0)[2] + 1;
                addFunction(2, function);
            } else if (number < 2000) {
                AlldiffCounts.get(0)[3] = AlldiffCounts.get(0)[3] + 1;
                addFunction(3, function);
            } else if (number < 3000) {
                AlldiffCounts.get(0)[4] = AlldiffCounts.get(0)[4] + 1;
                addFunction(4, function);
            } else if (number < 4000) {
                AlldiffCounts.get(0)[5] = AlldiffCounts.get(0)[5] + 1;
                addFunction(5, function);
            } else if (number < 5000) {
                AlldiffCounts.get(0)[6] = AlldiffCounts.get(0)[6] + 1;
                addFunction(6, function);
            } else if (number < 10000) {
                AlldiffCounts.get(0)[7] = AlldiffCounts.get(0)[7] + 1;
                addFunction(7, function);
            } else {
                AlldiffCounts.get(0)[8] = AlldiffCounts.get(0)[8] + 1;
                addFunction(8, function);
                addFunctionToTimeHistogram(date);
            }
        }
    }

    private void addFunctionToTimeHistogram(String dateString) {
        Date formattedDate = formatLogDate(dateString);
        long formattedDateTime = formattedDate.getTime();
        for (int i = 0; i < timeSplits.length - 1; i++) {
            if (formattedDateTime >= timeSplits[i] && formattedDateTime <= timeSplits[i+1]) {
                timeSplitCounts[i] = timeSplitCounts[i]+1;
                break;
            }
        }
    }

    public int checkFunction(String Function){ //Return the Index and add the function into the different ArrayLists
        for (int i = 1; i < AllfunctionsIn.size();i++) { //Checks if the function is already in the ArrayList
            if (Function.equalsIgnoreCase(AllfunctionsIn.get(i))) {
                return i;
            }
        }
        AllfunctionsIn.add(Function);
        AlldiffCounts.add(new int[9]);
        return AllfunctionsIn.size()-1;  //Returns the most recently added index.
    }

    public void addFunction(int num, String function){ //After getting the index, add one instance to that index.
        int index = checkFunction(function);
        AlldiffCounts.get(index)[num] = AlldiffCounts.get(index)[num]+1;
        if(num == 8) {
            if(!AllTenPlusIndexes.contains(index)) {
                AllTenPlusIndexes.add(index);
            }
        }

    }

    public void makeArrayLists(){ //This function parses the lines into millisecond integers, and function strings.
        for (int i = 0; i < MSLines.size(); i++) {
            int number;
            System.out.println(MSLines.get(i));
            try {

                    String[] numberBefore = MSLines.get(i).split("took | milliseconds"); //Create milliseconds number
                    number = Integer.parseInt(numberBefore[1]); //parse the number string as an int
                    String[] function;
                    if (MSLines.get(i).contains(" - completed")) {
                        function = MSLines.get(i).split(": | - completed"); //Create the function string[]
                    } else if (MSLines.get(i).contains(" completed took")){
                        function = MSLines.get(i).split(": | completed"); //Create the function string[]
                    } else {
                        function = MSLines.get(i).split(": | Completed"); //Create the function string[]
                    }
                    String[] date = MSLines.get(i).split("    \\| | \\| ");
                    for (int j = 0; j < date.length; j++) {
                        System.out.println(j + ": " + date[j]);
                    }
                    if (function.length > 2) {
                        checkAndAdd(number, function[1], date[2]);
                    }
                } catch(Exception e) {
                    //System.out.println(e);
                    //e.printStackTrace();
                }
            }
        }

        public void printSlowest() { // Prints the one with the most 10+ seconds and sets it's color to black.
            int highestIndex = 1;

            for (int i = 2; i < MaindiffCounts.size(); i++) {
                if (MaindiffCounts.get(i)[8] > MaindiffCounts.get(highestIndex)[8]) {
                    highestIndex = i;
                }
            }
            if (MaindiffCounts.get(highestIndex)[8] != 0) {
                comboBoxRenderer.worst(highestIndex); //Sets the color of the item with the most number of 10+ instances to black
            }
        }

        public void setColors(){ //determine the ratio of short times, to medium times, to long times, and set the color in the dropdown appropriately.
            for (int i = 0; i < MaindiffCounts.size(); i++) {
                int firstNum = MaindiffCounts.get(i)[0] + MaindiffCounts.get(i)[1] + MaindiffCounts.get(i)[2]; //short times are the first three times
                int secondNum = MaindiffCounts.get(i)[3] + MaindiffCounts.get(i)[4] + MaindiffCounts.get(i)[5]; //Medium times are the middle three times
                int thirdNum = MaindiffCounts.get(i)[6] + MaindiffCounts.get(i)[7] + MaindiffCounts.get(i)[8]; //Long times are the last three times\

                if (firstNum > secondNum && firstNum >= thirdNum) {
                    comboBoxRenderer.GreenColor();
                } else if (secondNum >= firstNum && secondNum >= thirdNum) { //Second number is most common, all >=
                    comboBoxRenderer.YellowColor();
                } else {
                    comboBoxRenderer.RedColor();
                }
            }
        }

        public void sortAlphabetically(){ //Sorts both arrayLists alphabetically for ease of viewing.
            for (int i = 1; i < AllfunctionsIn.size(); i++) {
                for (int j = i + 1; j < AllfunctionsIn.size(); j++) {
                    if (AllfunctionsIn.get(i).compareTo(AllfunctionsIn.get(j))>0) {
                        String temp = AllfunctionsIn.get(i);
                        AllfunctionsIn.set(i, AllfunctionsIn.get(j));
                        AllfunctionsIn.set(j, temp);
                        int[] temp2 = AlldiffCounts.get(i);
                        AlldiffCounts.set(i, AlldiffCounts.get(j));
                        AlldiffCounts.set(j, temp2);
                        for (int k = 0; k < AllTenPlusIndexes.size(); k++) {
                            if (AllTenPlusIndexes.get(k) == i) {
                                AllTenPlusIndexes.set(k, j);
                            } else if (AllTenPlusIndexes.get(k) == j) {
                                AllTenPlusIndexes.set(k, i);
                            }
                        }
                    }
                }
            }
        }

        public void sortSlowest(){ //Sorts the arrayList of Indexes with instances of 10+ seconds by most number of 10 plus indexes.
            for (int i = 0; i < AllTenPlusIndexes.size(); i++) {
                for (int j = i + 1; j < AllTenPlusIndexes.size(); j++) {
                    int FirstNumber = AlldiffCounts.get(AllTenPlusIndexes.get(i))[8];
                    int SecondNumber = AlldiffCounts.get(AllTenPlusIndexes.get(j))[8];
                    if (SecondNumber > FirstNumber) {
                        int temp = AllTenPlusIndexes.get(i);
                        AllTenPlusIndexes.set(i, AllTenPlusIndexes.get(j));
                        AllTenPlusIndexes.set(j, temp);
                    }

                }
            }
            for (int i = 0; i < AllTenPlusIndexes.size(); i++) {
                if (AllfunctionsIn.get(AllTenPlusIndexes.get(i)).contains("UIEvent")) {
                    UITenPlusIndexes.add(AllTenPlusIndexes.get(i));
                } else {
                    NotUITenPlusIndexes.add(AllTenPlusIndexes.get(i));
                }
            }

        }

        public void printArrayList(ArrayList arrayList){
            System.out.print("{");
            for (int i = 0; i < arrayList.size(); i++) {
                System.out.print(arrayList.get(i) + ", ");
            }
            System.out.println("}");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String[] final10List = new String[AllTenPlusIndexes.size()+2];
            final10List[0] = "# of Instances | Name";

            MaindiffCounts.clear();
            MainfunctionsIn.clear();
            //All button pressed down
            if (e.getActionCommand().equals(allString)) {
                MaindiffCounts.addAll(AlldiffCounts);
                MainfunctionsIn.addAll(AllfunctionsIn);
                for (int i = 0; i < AllTenPlusIndexes.size(); i++) {
                    final10List[i+1] =  AlldiffCounts.get(AllTenPlusIndexes.get(i))[8] + ": " + AllfunctionsIn.get(AllTenPlusIndexes.get(i));
                }
            } else if (e.getActionCommand().equals(onlyUIString)) { //only UIEvent Button pressed down
                MaindiffCounts.addAll(UIdiffCounts);
                MainfunctionsIn.addAll(UIfunctionsIn);
                for (int i = 0; i < UITenPlusIndexes.size(); i++) {
                    final10List[i+1] =  AlldiffCounts.get(UITenPlusIndexes.get(i))[8] + ": " + AllfunctionsIn.get(UITenPlusIndexes.get(i));
                }
            } else if (e.getActionCommand().equals(excludeUIString)) { //exclude UIEvent button pressed down
                MaindiffCounts.addAll(excludeUIdiffCounts);
                MainfunctionsIn.addAll(excludeUIfunctionsIn);
                for (int i = 0; i < NotUITenPlusIndexes.size(); i++) {
                    final10List[i+1] =  AlldiffCounts.get(NotUITenPlusIndexes.get(i))[8] + ": " + AllfunctionsIn.get(NotUITenPlusIndexes.get(i));
                }
            }

            dropDown.removeAllItems();
            for(String func: MainfunctionsIn){
                dropDown.addItem(func);
            }

            listModel.clear();
            for(String ListEntry: final10List) {
                listModel.addElement(ListEntry);
            }

            graph.calculateGraph(whatToDraw, MaindiffCounts, MSLines);

            comboBoxRenderer.clearColors();
            setColors();
            printSlowest();
            repaint();
        }

    }

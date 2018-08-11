import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

/**
 * Created by Tyler on 8/2/2016.
 */
public class Graph {

    private int topNum;
    private int index;
    private String finalString;
    private int increment;

    public Graph() {
        topNum = 0;
        index = 0;
        finalString = null;
        increment = 0;
    }

    public void calculateGraph(int Index, ArrayList<int[]> MaindiffCounts, ArrayList<String> MSLines) {
        System.out.println(index);
        this.index = Index;
        int highestIndex = 0;
        for (int i = 1; i < 9; i++) { //Finds the most number of instances in a category and sets it to highestIndex.
//            System.out.println("index: " + index);
            if (MaindiffCounts.get(index)[i] > MaindiffCounts.get(index)[highestIndex]){
                highestIndex = i;
            }
        }
        topNum = MaindiffCounts.get(index)[highestIndex];
        topNum = (int)(topNum + topNum/10.0);
        topNum = (int)(Math.log10(topNum)+1); //Gets the closest 10^x to highestIndex.
        topNum = (int)(Math.pow(10, topNum));
        //up to this point, you are close to the highestIndex, but not really.
        if (topNum > 10) {
            topNum = changeHeight(topNum, MaindiffCounts.get(index)[highestIndex]); //divides by 2 until the most number of instances times 2 is >= topNum.
            if (topNum % 10 != 0) { //Round to the nearest multiple of 10
                topNum += (10 - topNum%10);
            }
        } else {
            topNum = 10; //if it isn't > 10, set topNum to 10.
        }
        increment = topNum/10; //determine the increment...

        //Get the Lines to Determine the start/end Time and Dates
        String firstLine = MSLines.get(0);
        String lastLine = MSLines.get(MSLines.size()-1);

        //Parse the two lines by splitting them at "|" and taking the third piece of the spit.
        firstLine = firstLine.split(" \\| ")[2];
        lastLine = lastLine.split(" \\| ")[2];

        finalString = firstLine + " - " + lastLine ; //Create the string to be displayed at the top.
    }

    public int changeHeight(int CurrentHeight, int HighestBar){ //This is a recursive method that shrinks the height of the graph until it is
        if (HighestBar*2 >= CurrentHeight) {                    //at most the highest number of instances times 2.
            return CurrentHeight;
        } else {
            return changeHeight(CurrentHeight/2, HighestBar);
        }
    }

    public void draw(Graphics2D g2, ArrayList<int[]> MaindiffCounts){ //all the drawing code is performed here.
        g2.setColor(Color.BLACK);
        g2.drawLine(69, 150, 69, 650); //Vertical base line
        g2.drawLine(69, 650, 780, 650); //Horizontal base line
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        FontMetrics fontMetrics = g2.getFontMetrics();
        for (int i = 1; i < 11; i++) {
            g2.drawLine(69, 650-(50*i), 780, 650-(50*i)); //Draw all of the lines going horizontally across next to the # of instnaces.
        }
        g2.setColor(Color.BLACK);
        //Draw all of the categories at the bottom of the graph representing the number of seconds.
        g2.drawString("< 0.5", 98, 675);
        g2.drawString("0.5-1", 177, 675);
        g2.drawString("1-1.5", 253, 675);
        g2.drawString("1.5-2", 335, 675);
        g2.drawString("2-3", 417, 675);
        g2.drawString("3-4", 497, 675);
        g2.drawString("4-5", 577, 675);
        g2.drawString("5-10", 655, 675);
        g2.drawString("> 10", 730, 675);

        //Draw all of the strings to the side representing the number of instances.
        for (int i = 0; i <= 10; i++) {
            g2.drawString(String.valueOf(increment*i), 63-fontMetrics.stringWidth(String.valueOf(increment*i)), 655-(50*i));
        }


        int sum = 0;
        for (int i = 0; i < MaindiffCounts.get(index).length; i++) {
            sum += MaindiffCounts.get(index)[i];
        }

        //Draws the Bars along with the numbers above the bars that represent the number of instances.
        for (int i = 0; i < MaindiffCounts.get(index).length; i++) {
            if (i%2 == 0) {
                g2.setColor(new Color(255, 160, 28));
            } else {
                g2.setColor(new Color(112, 32, 197));
            }
            g2.fillRect(100 + (79 * i), (int)(652 - (500*((double)(MaindiffCounts.get(index)[i])/topNum))), 30, (int)(500*((double)(MaindiffCounts.get(index)[i])/topNum)));
            g2.setColor(Color.BLACK);

            String percentage = String.valueOf((MaindiffCounts.get(index)[i]*1.0)/sum*100);
            String percentageChop = percentage.substring(0, Math.min(percentage.length(), 4)) + "%";
            g2.drawString(percentageChop, 115+(79*i) - ((fontMetrics.stringWidth(percentageChop))/2), (int)(640-(500*((double)(MaindiffCounts.get(index)[i])/topNum))-15)); //TODO: Throw in percent here

            g2.drawString(String.valueOf(MaindiffCounts.get(index)[i]), 115+(79*i) - ((fontMetrics.stringWidth(String.valueOf(MaindiffCounts.get(index)[i])))/2), (int)(640-(500*((double)(MaindiffCounts.get(index)[i])/topNum)))); //TODO: Throw in percent here
        }

        //Draw all of the main categories as well as the time section title.
        g2.setFont(new Font("Arial", Font.BOLD, 15));

        AffineTransform orig = g2.getTransform();
        g2.rotate(-Math.PI / 2);
        g2.drawString("Number of Instances", -475, 20);
        g2.setTransform(orig);

        g2.setColor(Color.BLACK);

        g2.drawString("Seconds", 400, 700);

        g2.drawString(finalString, 275, 125);
    }


}

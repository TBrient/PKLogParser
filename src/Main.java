import javax.swing.*;

/**
 * A program designed to take in a log Zip file, unzip it to its components and
 * parse all of the lines in said components. Then take the information gained from the parse
 * and create various histograms that then can be screenshotted.
 *
 * Created by Tyler Brient
 */
public class Main {
    public static void main(String[] args) {
        Panel p = new Panel();

        JFrame window = new JFrame("Parse Logs");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setBounds(0, 0, 855, 800); //(x, y, w, h)
        window.add(p);
        window.setVisible(true);
    }



}

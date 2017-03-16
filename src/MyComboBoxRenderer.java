
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class MyComboBoxRenderer extends JLabel implements ListCellRenderer { //The custom combobox renderer which allows me to set the color of the text and the background color per item.

    private ArrayList<Color> colors = new ArrayList<Color>();

    public MyComboBoxRenderer() {
        setOpaque(true);
        setForeground(Color.BLACK);
    }

    public void GreenColor(){
        colors.add(Color.GREEN);
    }
    public void YellowColor(){
        colors.add(Color.ORANGE);
    }
    public void RedColor(){
        colors.add(Color.RED);
    }

    public void worst(int index){
        colors.set(index, Color.BLACK);
    }


    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        setText(value.toString());
        if (index >= 0) {
            setForeground(colors.get(index)); //Foreground or text color is the color previously assigned (red, green, orange)
        }
        if (isSelected) {
            setBackground(Color.LIGHT_GRAY); //sets background color of selected item to light gray
        } else {
            setBackground(Color.WHITE); //if not selected, background color is white.
        }
        return this;
    }

    public void clearColors() {
        colors.clear();
    }

}
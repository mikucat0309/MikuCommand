package com.github.mikucat0309;

import java.awt.GraphicsEnvironment;
import javax.swing.JOptionPane;

public class MeowMain {

    private static final String ERROR = "Meow! meow?\n"
            + "meow meowww meo..w?";

    public static void main(String[] args) throws UnsupportedOperationException {
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null,
                    ERROR,
                    "Do NOT run me directly!",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            throw new UnsupportedOperationException(ERROR);
        }
    }
}

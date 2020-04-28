package com.github.mikucat0309.command;


public class CommandMessageFormatting {

    public static final String PIPE_TEXT = "|";
    public static final String SPACE_TEXT = " ";
    public static final String STAR_TEXT = "*";
    public static final String LT_TEXT = "<";
    public static final String GT_TEXT = ">";
    public static final String ELLIPSIS_TEXT = "…";

    private CommandMessageFormatting() {
    }

    public static String error(String error) {
        return "[ERROR] " + error;
    }

    public static String debug(String debug) {
        return "[DEBUG] " + debug;
    }

}

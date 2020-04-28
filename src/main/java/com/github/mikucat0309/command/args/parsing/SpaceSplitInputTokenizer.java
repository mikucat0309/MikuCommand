package com.github.mikucat0309.command.args.parsing;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

class SpaceSplitInputTokenizer implements InputTokenizer {

    public static final SpaceSplitInputTokenizer INSTANCE = new SpaceSplitInputTokenizer();
    private static final Pattern SPACE_REGEX = Pattern.compile("^[ ]*$");

    private SpaceSplitInputTokenizer() {
    }

    @Override
    public List<SingleArg> tokenize(String arguments, boolean lenient) {
        if (SPACE_REGEX.matcher(arguments).matches()) {
            return ImmutableList.of();
        }

        var ret = new ArrayList<SingleArg>();
        int lastIndex = 0;
        int spaceIndex;
        while ((spaceIndex = arguments.indexOf(" ")) != -1) {
            if (spaceIndex != 0) {
                ret.add(new SingleArg(arguments.substring(0, spaceIndex), lastIndex, lastIndex + spaceIndex));
                arguments = arguments.substring(spaceIndex);
            } else {
                arguments = arguments.substring(1);
            }
            lastIndex += spaceIndex + 1;
        }

        ret.add(new SingleArg(arguments, lastIndex, lastIndex + arguments.length()));
        return ret;
    }
}

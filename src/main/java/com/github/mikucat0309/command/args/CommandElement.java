package com.github.mikucat0309.command.args;

import com.github.mikucat0309.command.CommandSource;

import java.util.List;

import javax.annotation.Nullable;

public abstract class CommandElement {

    @Nullable
    private final String key;

    protected CommandElement(@Nullable String key) {
        this.key = key;
    }

    @Nullable
    public String getKey() {
        return this.key;
    }

    public void parse(CommandSource source, CommandArgs args, CommandContext context) throws ArgumentParseException {
        var val = parseValue(source, args);
        var key = getKey();
        if (key != null && val != null) {
            if (val instanceof Iterable<?>) {
                for (Object ent : ((Iterable<?>) val)) {
                    context.putArg(key, ent);
                }
            } else {
                context.putArg(key, val);
            }
        }
    }

    @Nullable
    protected abstract Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException;

    public abstract List<String> complete(CommandSource src, CommandArgs args, CommandContext context);

    public String getUsage(CommandSource src) {
        return getKey() == null ? "" : "<" + getKey() + ">";
    }
}

package com.github.mikucat0309.command;

import com.github.mikucat0309.command.locale.Locales;

import java.util.Locale;
import java.util.Optional;

public interface CommandSource {

    String getName();

    void sendMessage(String message);

    Optional<CommandSource> getCommandSource();

    default Locale getLocale() {
        return Locales.DEFAULT;
    }

}

#!/usr/bin/env python3

'''

Author:
Mikucat0309

'''

### Options ###
LICENSE_REMOVE = True
STRING_REPLACE = False
LOCAL_VARIABLE_TYPE_INFERENCE = False
JAVADOC_REMOVE = False # javadocs (starts with `/**`)
COMMENT_REMOVE = False # comments (starts with `/*`)
PACKAGE_REPLACE = "com.github.mikucat0309"

from pathlib import Path
import re

java_src = Path("./src")

### Prepared Regex ###

one_time_replace_patterns = {
    r"import org\.spongepowered\.api\.(?!command).*\n" : "",
    r"import static org\.spongepowered\.api\.util\.SpongeApiTranslationHelper\.t;\n" : "",
    r"TestPlainTextSerializer\.inject\(\);\n" : ""
}

replace_patterns = {
    r"Text\.builder\(\)": "new StringBuilder()",
    r"Text\.EMPTY": '""',
    r"Text\.NEW_LINE": "\n",
    r"Text\.of": "of",
    r"\.toPlain\(\)": "",
    r"\.getText\(\)": ".getMessage()",
    r"(?P<prefix>[\s(])t(": r"\g<prefix>String.format(",
    r"(?P<prefix>[\s<(])Text(?P<suffix>[\s>)])": r"\g<prefix>String\g<suffix>",
}

### main entry ###

if __name__ == "__main__":

    for child in java_src.rglob("*.java"):
        print(f"========== {child.name} ==========")
        raw = child.read_text('utf-8')

        # remove license
        result = re.match(r"\s*\/\*.* \*\/\n", raw, flags=re.S)
        if result != None:
            license = result[0]
            raw = raw[result.end(0):]

        # change package
        if type(PACKAGE_REPLACE) == str and PACKAGE_REPLACE.strip() != "" and re.match(r"^[\w.]+$", PACKAGE_REPLACE) != None:
            raw = raw.replace("org.spongepowered.api", PACKAGE_REPLACE)

        if JAVADOC_REMOVE:
            raw = re.sub(r"\/\*\*.*?\*\/", "", raw, flags=re.S)

        if COMMENT_REMOVE:
            raw = re.sub(r"\/\*[^*].*?\*\/", "", raw, flags=re.S)

        if STRING_REPLACE:
            for pattern, repl in one_time_replace_patterns.items():
                re.sub(pattern, repl, raw, 1)

            for pattern, repl in replace_patterns.items():
                re.sub(pattern, repl, raw)

        if LOCAL_VARIABLE_TYPE_INFERENCE:
            re.sub(r"^(?P<indent>[\t ]+)(final )?[A-Z][\w.<>?*]+[\t ]+(?P<name>\w+)[\t ]+=", r"\g<indent>var \g<name> =",raw)

        if not LICENSE_REMOVE:
            raw = license + raw

        # print(raw)
        child.write_text(raw, "utf-8")
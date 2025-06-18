package xyz.itseve.picoedit.utils;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LuaHighlighter {
    private static final String[] KEYWORDS = new String[]{
        "and", "break", "do", "else", "elseif", "end", "false", "for",
        "function", "goto", "if", "in", "local", "nil", "not", "or",
        "repeat", "return", "then", "true", "until", "whilwqe"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";

    private static final String MULTILINE_COMMENT_PATTERN = "--\\[\\[.*?\\]\\]";
    private static final String SINGLELINE_COMMENT_PATTERN = "--[^\n]*";
    private static final String COMMENT_PATTERN = MULTILINE_COMMENT_PATTERN + "|" + SINGLELINE_COMMENT_PATTERN;

    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'|\\[\\[(.|\\R)*?]]";
    private static final String NUMBER_PATTERN = "\\b\\d+(\\.\\d+)?\\b";
    private static final String FUNCTION_CALL_PATTERN = "\\b(\\w+)\\s*(?=\\()";

    private static final Pattern PATTERN = Pattern.compile(
        "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
            + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
            + "|(?<STRING>" + STRING_PATTERN + ")"
            + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
            + "|(?<FUNC>" + FUNCTION_CALL_PATTERN + ")",
        Pattern.DOTALL
    );

    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        int lastEnd = 0;
        while (matcher.find()) {
            String styleClass =
                matcher.group("KEYWORD") != null ? "keyword" :
                    matcher.group("COMMENT") != null ? "comment" :
                        matcher.group("STRING") != null ? "string" :
                            matcher.group("NUMBER") != null ? "number" :
                                matcher.group("FUNC") != null ? "function" :
                                    null;

            assert styleClass != null;

            spansBuilder.add(Collections.singleton("default"), matcher.start() - lastEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }

        spansBuilder.add(Collections.emptyList(), text.length() - lastEnd);
        return spansBuilder.create();
    }
}

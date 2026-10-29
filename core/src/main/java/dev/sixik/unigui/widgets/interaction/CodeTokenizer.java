package dev.sixik.unigui.widgets.interaction;

import java.util.List;

@FunctionalInterface
public interface CodeTokenizer {
    CodeTokenizer NONE = context -> List.of();

    List<CodeToken> tokenize(CodeTokenizationContext context);
}
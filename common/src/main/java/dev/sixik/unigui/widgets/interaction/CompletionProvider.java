package dev.sixik.unigui.widgets.interaction;

import java.util.List;

@FunctionalInterface
public interface CompletionProvider {
    CompletionProvider NONE = context -> List.of();

    List<CompletionItem> complete(CompletionContext context);
}
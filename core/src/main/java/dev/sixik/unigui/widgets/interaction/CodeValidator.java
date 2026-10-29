package dev.sixik.unigui.widgets.interaction;

import java.util.List;

@FunctionalInterface
public interface CodeValidator {
    CodeValidator NONE = context -> List.of();

    List<CodeDiagnostic> validate(CodeValidationContext context);
}
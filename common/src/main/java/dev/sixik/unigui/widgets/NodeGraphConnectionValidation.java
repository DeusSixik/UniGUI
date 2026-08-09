package dev.sixik.unigui.widgets;

public record NodeGraphConnectionValidation(boolean valid, String reason) {
    public NodeGraphConnectionValidation {
        reason = reason == null ? "" : reason;
    }

    public static NodeGraphConnectionValidation accepted() {
        return new NodeGraphConnectionValidation(true, "");
    }

    public static NodeGraphConnectionValidation invalid(String reason) {
        return new NodeGraphConnectionValidation(false, reason);
    }
}

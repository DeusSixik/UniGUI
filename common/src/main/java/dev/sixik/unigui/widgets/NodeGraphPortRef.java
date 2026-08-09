package dev.sixik.unigui.widgets;

public record NodeGraphPortRef(String itemId, String portId) {
    public NodeGraphPortRef {
        itemId = normalize(itemId);
        portId = normalize(portId);
    }

    public boolean empty() {
        return itemId.isEmpty() || portId.isEmpty();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}


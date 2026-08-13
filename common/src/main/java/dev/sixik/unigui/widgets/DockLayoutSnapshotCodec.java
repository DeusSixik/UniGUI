package dev.sixik.unigui.widgets;

import java.nio.charset.StandardCharsets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Compact string codec for {@link DockLayoutSnapshot}.
 *
 * <p>This class intentionally uses a documented custom format instead of the
 * generic FieldCodec infrastructure because a docking layout is a recursive
 * tree and the stored value is usually embedded as a short UI preference.</p>
 *
 * <p>Format version {@code DLS1}: {@code DLS1|activePaneId|node}. Strings are
 * URL-safe Base64 without padding, with {@code -} representing an empty value.
 * A leaf node is encoded as {@code L[selectedPaneId|paneId,paneId,...]}; a
 * split node is encoded as {@code S[H|ratio|first|second]} or
 * {@code S[V|ratio|first|second]}.</p>
 *
 * <p>Versioning policy: incompatible future formats must use a new
 * {@code DLS&lt;n&gt;} prefix and add an explicit decode branch. Unsupported,
 * malformed or missing data decodes to an empty snapshot instead of throwing.</p>
 */
public final class DockLayoutSnapshotCodec {
    public static final int FORMAT_VERSION = 1;
    private static final String PREFIX = "DLS" + FORMAT_VERSION + "|";
    private static final String EMPTY = "-";

    /**
     * Encodes a snapshot as the compact {@code DLS1} string format.
     */
    public static String encode(DockLayoutSnapshot snapshot) {
        DockLayoutSnapshot normalized = snapshot == null ? new DockLayoutSnapshot(null, "") : snapshot;
        return PREFIX + encodeString(normalized.activePaneId()) + "|" + encodeNode(normalized.root());
    }

    /**
     * Decodes the compact {@code DLS1} string format.
     *
     * <p>Unknown versions and malformed payloads fail closed to an empty
     * snapshot so user preferences cannot break docking initialization.</p>
     */
    public static DockLayoutSnapshot decode(String encoded) {
        if (encoded == null || !encoded.startsWith(PREFIX)) {
            return new DockLayoutSnapshot(null, "");
        }
        try {
            Parser parser = new Parser(encoded.substring(PREFIX.length()));
            String active = decodeString(parser.readUntil('|'));
            parser.expect('|');
            DockLayoutSnapshot.Node root = parser.readNode();
            return new DockLayoutSnapshot(root, active);
        } catch (RuntimeException ignored) {
            return new DockLayoutSnapshot(null, "");
        }
    }

    private static String encodeNode(DockLayoutSnapshot.Node node) {
        DockLayoutSnapshot.Node normalized = node == null ? DockLayoutSnapshot.Node.emptyLeaf() : node;
        if (normalized.kind() == DockNode.Kind.LEAF) {
            StringBuilder builder = new StringBuilder("L[")
                    .append(encodeString(normalized.selectedPaneId()))
                    .append('|');
            for (int i = 0; i < normalized.paneIds().size(); i++) {
                if (i > 0) builder.append(',');
                builder.append(encodeString(normalized.paneIds().get(i)));
            }
            return builder.append(']').toString();
        }
        return "S["
                + (normalized.orientation() == DockSplitOrientation.VERTICAL ? "V" : "H")
                + '|'
                + String.format(Locale.ROOT, "%.6f", normalized.splitRatio())
                + '|'
                + encodeNode(normalized.first())
                + '|'
                + encodeNode(normalized.second())
                + ']';
    }

    private static String encodeString(String value) {
        if (value == null || value.isEmpty()) return EMPTY;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeString(String value) {
        if (value == null || value.isEmpty() || EMPTY.equals(value)) return "";
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static final class Parser {
        private final String source;
        private int index;

        private Parser(String source) {
            this.source = source == null ? "" : source;
        }

        private DockLayoutSnapshot.Node readNode() {
            if (match("L[")) {
                String selected = decodeString(readUntil('|'));
                expect('|');
                List<String> panes = new ObjectArrayList<>();
                String paneList = readUntil(']');
                if (!paneList.isEmpty()) {
                    for (String pane : paneList.split(",")) {
                        String decoded = decodeString(pane);
                        if (!decoded.isEmpty()) panes.add(decoded);
                    }
                }
                expect(']');
                return new DockLayoutSnapshot.Node(DockNode.Kind.LEAF, null, 1.0f, panes, selected, null, null);
            }
            if (match("S[")) {
                DockSplitOrientation orientation = "V".equals(readUntil('|'))
                        ? DockSplitOrientation.VERTICAL
                        : DockSplitOrientation.HORIZONTAL;
                expect('|');
                float ratio = parseRatio(readUntil('|'));
                expect('|');
                DockLayoutSnapshot.Node first = readNode();
                expect('|');
                DockLayoutSnapshot.Node second = readNode();
                expect(']');
                return new DockLayoutSnapshot.Node(DockNode.Kind.SPLIT, orientation, ratio, List.of(), "", first, second);
            }
            return DockLayoutSnapshot.Node.emptyLeaf();
        }

        private boolean match(String token) {
            if (!source.startsWith(token, index)) return false;
            index += token.length();
            return true;
        }

        private String readUntil(char delimiter) {
            int start = index;
            while (index < source.length() && source.charAt(index) != delimiter) {
                index++;
            }
            return source.substring(start, Math.min(index, source.length()));
        }

        private void expect(char c) {
            if (index < source.length() && source.charAt(index) == c) {
                index++;
            }
        }

        private static float parseRatio(String text) {
            try {
                return Float.parseFloat(text);
            } catch (RuntimeException ignored) {
                return 0.5f;
            }
        }
    }

    private DockLayoutSnapshotCodec() {
    }
}

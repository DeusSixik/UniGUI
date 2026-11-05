package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

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
@AiDoc(summary = "Compact string serializer and deserializer (DLS1 format) for persisting docking layouts across application restarts."
)
@WikiPage(
        title = "DockLayoutSnapshotCodec",
        slug = "dock-layout-snapshot-codec",
        category = "<key:docking.category>",
        summary = "<key:dock.layout.snapshot.codec.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:dock.layout.snapshot.codec.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:dock.layout.snapshot.codec.example.roundtrip.title>",
        code = "String encoded = DockLayoutSnapshotCodec.encode(snapshot);\n"
                + "DockLayoutSnapshot restored = DockLayoutSnapshotCodec.decode(encoded);"
)
@WikiLink(
        label = "DockLayoutSnapshot",
        target = "page:dock-layout-snapshot",
        description = "<key:dock.layout.snapshot.summary>"
)
@WikiLink(
        label = "DockingManager",
        target = "page:docking-manager",
        description = "<key:docking.manager.summary>"
)
public final class DockLayoutSnapshotCodec {
    public static final int FORMAT_VERSION = 1;
    private static final String PREFIX = "DLS" + FORMAT_VERSION + "|";
    private static final String EMPTY = "-";

    private DockLayoutSnapshotCodec() {
    }

    /**
     * Encodes a snapshot as the compact {@code DLS1} string format.
     */
    @AiDoc(summary = "Serializes a DockLayoutSnapshot into a compact DLS1-encoded string.")
    @WikiMethod(summary = "<key:dock.layout.snapshot.codec.method.encode>")
    @WikiReturn("<key:dock.layout.snapshot.codec.return.encoded>")
    public static String encode(
            @WikiParameter(description = "<key:dock.layout.snapshot.codec.param.snapshot>")
            DockLayoutSnapshot snapshot
    ) {
        DockLayoutSnapshot normalized = snapshot == null ? new DockLayoutSnapshot(null, "") : snapshot;
        return PREFIX + encodeString(normalized.activePaneId()) + "|" + encodeNode(normalized.root());
    }

    /**
     * Decodes the compact {@code DLS1} string format.
     *
     * <p>Unknown versions and malformed payloads fail closed to an empty
     * snapshot so user preferences cannot break docking initialization.</p>
     */
    @AiDoc(summary = "Deserializes a DLS1-encoded string back into a DockLayoutSnapshot, falling back cleanly to an empty snapshot on errors.")
    @WikiMethod(summary = "<key:dock.layout.snapshot.codec.method.decode>")
    @WikiReturn("<key:dock.layout.snapshot.codec.return.snapshot>")
    public static DockLayoutSnapshot decode(
            @WikiParameter(description = "<key:dock.layout.snapshot.codec.param.encoded>")
            String encoded
    ) {
        if (encoded == null || !encoded.startsWith(PREFIX)) {
            return new DockLayoutSnapshot(null, "");
        }
        try {
            Parser parser = new Parser(encoded.substring(PREFIX.length()));
            String activePaneId = parser.readString();
            parser.expect('|');
            DockLayoutSnapshot.Node root = parser.readNode();
            return new DockLayoutSnapshot(root, activePaneId);
        } catch (RuntimeException e) {
            return new DockLayoutSnapshot(null, "");
        }
    }

    private static String encodeNode(DockLayoutSnapshot.Node node) {
        if (node == null || node.isLeaf()) {
            StringBuilder builder = new StringBuilder("L[");
            builder.append(encodeString(node == null ? "" : node.selectedPaneId()));
            builder.append('|');
            List<String> panes = node == null ? List.of() : node.paneIds();
            for (int i = 0; i < panes.size(); i++) {
                if (i > 0) builder.append(',');
                builder.append(encodeString(panes.get(i)));
            }
            builder.append(']');
            return builder.toString();
        }

        char orientation = node.orientation() == DockSplitOrientation.VERTICAL ? 'V' : 'H';
        String ratio = String.format(Locale.ROOT, "%.4f", node.splitRatio());
        return "S[" + orientation + '|' + ratio + '|' + encodeNode(node.first()) + '|' + encodeNode(node.second()) + ']';
    }

    private static String encodeString(String value) {
        if (value == null || value.isEmpty()) return EMPTY;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeString(String token) {
        if (token == null || token.equals(EMPTY)) return "";
        try {
            return new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static final class Parser {
        private final String input;
        private int index;

        Parser(String input) {
            this.input = input;
        }

        String readString() {
            int start = index;
            while (index < input.length()) {
                char current = input.charAt(index);
                if (current == '|' || current == ',' || current == ']') break;
                index++;
            }
            return decodeString(input.substring(start, index));
        }

        DockLayoutSnapshot.Node readNode() {
            expectAny('L', 'S');
            char kind = input.charAt(index - 1);
            expect('[');
            if (kind == 'L') {
                String selected = readString();
                expect('|');
                List<String> panes = new ObjectArrayList<>();
                if (peek() != ']') {
                    while (true) {
                        panes.add(readString());
                        if (peek() == ',') {
                            index++;
                            continue;
                        }
                        break;
                    }
                }
                expect(']');
                return new DockLayoutSnapshot.Node(
                        DockNode.Kind.LEAF,
                        null,
                        1.0f,
                        panes,
                        selected,
                        null,
                        null);
            }

            char orientationChar = nextChar();
            DockSplitOrientation orientation = orientationChar == 'V'
                    ? DockSplitOrientation.VERTICAL
                    : DockSplitOrientation.HORIZONTAL;
            expect('|');
            float ratio = Float.parseFloat(readToken('|'));
            expect('|');
            DockLayoutSnapshot.Node first = readNode();
            expect('|');
            DockLayoutSnapshot.Node second = readNode();
            expect(']');
            return new DockLayoutSnapshot.Node(
                    DockNode.Kind.SPLIT,
                    orientation,
                    ratio,
                    List.of(),
                    "",
                    first,
                    second);
        }

        String readToken(char delimiter) {
            int start = index;
            while (index < input.length() && input.charAt(index) != delimiter) {
                index++;
            }
            return input.substring(start, index);
        }

        char nextChar() {
            if (index >= input.length()) throw new IllegalStateException("Unexpected end of input");
            return input.charAt(index++);
        }

        char peek() {
            if (index >= input.length()) return '\0';
            return input.charAt(index);
        }

        void expect(char expected) {
            char actual = nextChar();
            if (actual != expected) {
                throw new IllegalStateException("Expected '" + expected + "' but got '" + actual + "'");
            }
        }

        void expectAny(char first, char second) {
            char actual = nextChar();
            if (actual != first && actual != second) {
                throw new IllegalStateException("Expected '" + first + "' or '" + second + "' but got '" + actual + "'");
            }
        }
    }
}

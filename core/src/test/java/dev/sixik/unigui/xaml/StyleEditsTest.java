package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.StyleEdits;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StyleEditsTest {
    private final EditorSession session = EditorSession.create(
            "<VBox id=\"root\"><Button id=\"b\" Style=\"{StaticResource Primary}\"/></VBox>");

    private UiNode root() {
        return session.document().root();
    }

    private boolean add(String key, String target, String basedOn) {
        return session.apply(StyleEdits.addStyle(session.document(), root(), key, target, basedOn, session.schema()));
    }

    @Test
    void aStyleIsAddedWithSettersAndAppliedByTheLoader() {
        // Без стиля ссылка в документе уже ошибочна, и правка, которая её чинит, допустима.
        assertTrue(add("Primary", "Button", null));
        assertTrue(session.apply(StyleEdits.setSetter(session.document(), root(), "Primary", "text", "Go", session.schema())));
        assertTrue(session.apply(StyleEdits.setSetter(session.document(), root(), "Primary", "text", "Buy", session.schema())),
                "setting an existing setter replaces its value");
        assertEquals(List.of("Primary"), StyleEdits.keys(root()));

        Widget loaded = Xaml.load(session.text());
        Button button = (Button) Xaml.find(loaded, "b").orElseThrow();
        assertEquals("Buy", button.text());
    }

    @Test
    void removingASetterAndAStyleWorksAndUndoRestores() {
        assertTrue(add("Primary", "Button", null));
        assertTrue(session.apply(StyleEdits.setSetter(session.document(), root(), "Primary", "text", "Go", session.schema())));
        assertTrue(session.apply(StyleEdits.removeSetter(session.document(), root(), "Primary", "text", session.schema())));
        assertFalse(session.text().contains("Setter"), session.text());
        assertTrue(session.undo());
        assertTrue(session.text().contains("Setter"), session.text());
    }

    @Test
    void duplicatesAndMissingStylesAreRefused() {
        assertTrue(add("Primary", "Button", null));
        assertFalse(add("Primary", "Button", null), "the key is taken");
        assertFalse(session.apply(StyleEdits.setSetter(session.document(), root(), "Primary", "noSuchProperty", "1",
                session.schema())), "an unknown property would make the document worse");
        assertFalse(session.apply(StyleEdits.setSetter(session.document(), root(), "Primary", "opacity", "lots",
                session.schema())), "a value the property cannot take is refused");
        assertFalse(session.apply(StyleEdits.removeStyle(session.document(), root(), "Missing", session.schema())));
        assertFalse(session.apply(StyleEdits.setSetter(session.document(), root(), "Missing", "text", "x", session.schema())));
    }
}

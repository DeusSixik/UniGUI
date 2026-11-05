package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.struct.StructEdits;
import dev.sixik.unigui.xaml.struct.StructType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StructEditsTest {
    private final EditorSession session = EditorSession.create(
            "<Struct x:Name=\"Item\" xmlns:x=\"https://unigui.dev/xaml\"><Field Name=\"title\" Type=\"string\"/></Struct>");

    private List<String> fields() {
        return session.document().root().children().stream().map(field -> field.attributeText("Name", "")).toList();
    }

    @Test
    void fieldsAreAddedMovedRetypedRenamedAndRemoved() {
        assertTrue(StructEdits.addField(session, "price", "int"));
        assertTrue(StructEdits.addField(session, "icon", "texture"));
        assertEquals(List.of("title", "price", "icon"), fields());

        assertTrue(StructEdits.move(session, "icon", -1));
        assertEquals(List.of("title", "icon", "price"), fields());
        assertTrue(StructEdits.setType(session, "price", "float"));
        assertTrue(StructEdits.renameField(session, "title", "name"));
        assertTrue(StructEdits.removeField(session, "icon"));
        assertEquals(List.of("name", "price"), fields());
        StructType.parse(session.text());
    }

    @Test
    void everyEditIsOneStepOfUndo() {
        int steps = session.undoCount();
        assertTrue(StructEdits.addField(session, "price", "int"));
        assertEquals(steps + 1, session.undoCount());
        assertTrue(session.undo());
        assertEquals(List.of("title"), fields());
    }

    @Test
    void badNamesDuplicatesAndTypesAreRefusedWithAReason() {
        int steps = session.undoCount();
        assertFalse(StructEdits.addField(session, "title", "int"), "duplicate");
        assertFalse(session.diagnostics(EditorSession.Channel.EDIT).isEmpty());
        assertFalse(StructEdits.addField(session, "1bad", "int"));
        assertFalse(StructEdits.addField(session, "x", "nonsense"));
        assertFalse(StructEdits.setType(session, "title", "nonsense"));
        assertFalse(StructEdits.removeField(session, "missing"));
        assertFalse(StructEdits.renameField(session, "title", "bad name"));
        assertEquals(steps, session.undoCount(), "refused edits leave no history");
        assertEquals(List.of("title"), fields());
    }
}

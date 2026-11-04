package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.component.ComponentDefinition;
import dev.sixik.unigui.xaml.component.Contract;
import dev.sixik.unigui.xaml.component.ContractEdits;
import dev.sixik.unigui.xaml.edit.Edit;
import dev.sixik.unigui.xaml.edit.EditorSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ContractEditsTest {
    private static final String COMPONENT = """
            <Component x:Name="Card" xmlns:x="https://unigui.dev/xaml">
              <Component.Contract>
                <Param Name="Title" Type="string" Default="idle"/>
                <Event Name="Buy" Direction="Out" Args="id:string"/>
              </Component.Contract>
              <Label text="{Param Title}"/>
            </Component>""";

    private final EditorSession session = EditorSession.create(COMPONENT);

    private UiNode root() {
        return session.document().root();
    }

    private boolean apply(Edit edit) {
        return session.apply(edit);
    }

    private Contract contract() {
        return ComponentDefinition.parse("t", session.document()).contract();
    }

    private String problem() {
        return session.diagnostics(EditorSession.Channel.EDIT).get(0).message();
    }

    @Test
    void paramsVarsAndEventsAreAddedAndUndone() {
        String before = session.text();
        assertTrue(apply(ContractEdits.addParam(root(), "Price", "int", "5")));
        assertTrue(apply(ContractEdits.addVar(root(), "Hovered", "bool", null)));
        assertTrue(apply(ContractEdits.addEvent(root(), "Invalidate", "In", null, true)));

        Contract contract = contract();
        assertEquals(2, contract.params().size());
        assertEquals(1, contract.vars().size());
        assertEquals(2, contract.events().size());
        assertTrue(session.text().contains("Coalesce=\"true\""), session.text());

        assertTrue(session.undo());
        assertTrue(session.undo());
        assertTrue(session.undo());
        assertEquals(1, contract().params().size());
        assertEquals(0, contract().vars().size());
        assertEquals(1, contract().events().size());
        assertFalse(session.text().contains("Price"), session.text());
        assertEquals(EditorSession.create(before).document().root().children().size(),
                root().children().size(), "the body is untouched");
    }

    @Test
    void anInvalidNameOrTypeIsRejectedAndTheDocumentStaysIntact() {
        String before = session.text();
        assertFalse(apply(ContractEdits.addParam(root(), "1bad", "string", null)));
        assertTrue(problem().contains("not a valid"), problem());
        assertFalse(apply(ContractEdits.addParam(root(), "Title", "string", null)));
        assertTrue(problem().contains("already declared"), problem());
        assertFalse(apply(ContractEdits.addParam(root(), "Count", "integer", null)));
        assertFalse(apply(ContractEdits.addParam(root(), "Count", "int", "many")));
        assertTrue(problem().contains("not a valid int"), problem());
        assertFalse(apply(ContractEdits.addEvent(root(), "Construct", "Out", null, false)));
        assertEquals(before, session.text());
    }

    @Test
    void anAttributeIsSetAndClearedWithTheSameChecks() {
        assertTrue(apply(ContractEdits.setAttribute(root(), "Param", "Title", "Default", "hello")));
        assertEquals("hello", contract().param("Title").orElseThrow().defaultText());
        assertTrue(apply(ContractEdits.setAttribute(root(), "Param", "Title", "Default", null)));
        assertFalse(session.text().contains("Default="), session.text());

        assertTrue(apply(ContractEdits.setAttribute(root(), "Event", "Buy", "Args", "id:string, qty:int")));
        assertEquals(2, contract().event("Buy").orElseThrow().args().size());
        assertFalse(apply(ContractEdits.setAttribute(root(), "Event", "Buy", "Args", "broken")));
        assertTrue(problem().contains("name:type"), problem());
        assertFalse(apply(ContractEdits.setAttribute(root(), "Param", "Title", "Name", "Other")));
        assertFalse(apply(ContractEdits.setAttribute(root(), "Param", "Missing", "Type", "int")));
    }

    @Test
    void anElementIsRemoved() {
        assertTrue(apply(ContractEdits.remove(root(), "Event", "Buy")));
        assertEquals(0, contract().events().size());
        assertFalse(apply(ContractEdits.remove(root(), "Event", "Buy")));
        assertTrue(session.undo());
        assertEquals(1, contract().events().size());
    }

    @Test
    void aFreshNameSkipsTakenOnes() {
        assertEquals("Param1", ContractEdits.freshName(root(), "Param"));
        assertTrue(apply(ContractEdits.addParam(root(), "Param1", "int", null)));
        assertEquals("Param2", ContractEdits.freshName(root(), "Param"));
    }

    @Test
    void aMissingContractIsCreatedByTheFirstEdit() {
        EditorSession bare = EditorSession.create("""
                <Component x:Name="Bare" xmlns:x="https://unigui.dev/xaml"><Label text="x"/></Component>""");
        assertTrue(bare.apply(ContractEdits.addParam(bare.document().root(), "A", "string", null)));
        assertTrue(bare.text().contains("Component.Contract"), bare.text());
        assertTrue(bare.undo());
        assertFalse(bare.text().contains("Component.Contract"), bare.text());
    }
}

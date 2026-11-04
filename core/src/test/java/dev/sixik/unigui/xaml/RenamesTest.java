package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.component.Renames;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.load.DocumentValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RenamesTest {
    private static final String COMPONENT = """
            <Component x:Name="Card" xmlns:x="https://unigui.dev/xaml">
              <Component.Contract>
                <Param Name="Title" Type="string" Default="idle"/>
                <Var Name="Seen" Type="int"/>
                <Event Name="Buy" Direction="Out" Args="id:string"/>
                <Event Name="Refresh" Direction="In"/>
              </Component.Contract>
              <VBox id="root">
                <Label id="title" text="{Param Title}"/>
                <Button id="buy" text="Buy" onClick="{Raise Buy}"/>
              </VBox>
              <Component.Graph>
                <Node Id="in" Type="Event.Refresh"/>
                <Node Id="set" Type="Param.Set" Name="Title" value="x"/>
                <Node Id="count" Type="Var.Set" Name="Seen" value="1"/>
                <Node Id="shout" Type="Raise.Buy" id="a"/>
                <Wire From="in.then" To="set.exec"/>
                <Wire From="set.then" To="count.exec"/>
                <Wire From="count.then" To="shout.exec"/>
              </Component.Graph>
            </Component>""";

    private final EditorSession session = EditorSession.create(COMPONENT);

    private boolean clean() {
        return new DocumentValidator(Xaml.defaultSchema()).validate(session.text()).valid();
    }

    @Test
    void theDocumentStartsClean() {
        assertTrue(clean(), new DocumentValidator(Xaml.defaultSchema()).validate(session.text()).diagnostics().toString());
    }

    @Test
    void aParameterIsRenamedInTheContractTheBodyAndTheGraph() {
        assertTrue(Renames.param(session, "Title", "Caption"), session.diagnostics(EditorSession.Channel.EDIT).toString());
        String text = session.text();
        assertTrue(text.contains("Name=\"Caption\""), text);
        assertTrue(text.contains("{Param Caption}"), text);
        assertFalse(text.contains("Title"), text);
        assertTrue(clean());
    }

    @Test
    void aRenameIsOneStepOfUndo() {
        int steps = session.undoCount();
        assertTrue(Renames.param(session, "Title", "Caption"));
        assertEquals(steps + 1, session.undoCount());
        assertTrue(session.undo());
        assertTrue(session.text().contains("{Param Title}"));
        assertTrue(session.text().contains("Name=\"Title\""));
        assertTrue(clean());
    }

    @Test
    void anOutgoingEventIsRenamedWithItsRaiseReferences() {
        assertTrue(Renames.event(session, "Buy", "Purchase"), session.diagnostics(EditorSession.Channel.EDIT).toString());
        String text = session.text();
        assertTrue(text.contains("{Raise Purchase}"), text);
        assertTrue(text.contains("Type=\"Raise.Purchase\""), text);
        assertFalse(text.contains("Name=\"Buy\""), text);
        assertTrue(clean());
    }

    @Test
    void anIncomingEventIsRenamedWithItsEntryNode() {
        assertTrue(Renames.event(session, "Refresh", "Reload"));
        assertTrue(session.text().contains("Type=\"Event.Reload\""), session.text());
        assertTrue(clean());
    }

    @Test
    void aVariableIsRenamedInTheGraph() {
        assertTrue(Renames.variable(session, "Seen", "Visits"));
        assertTrue(session.text().contains("Name=\"Visits\""), session.text());
        assertFalse(session.text().contains("Seen"), session.text());
        assertTrue(clean());
    }

    @Test
    void aTakenOrInvalidNameIsRefusedAndNothingChanges() {
        int steps = session.undoCount();
        assertFalse(Renames.param(session, "Title", "Seen"), "the name belongs to a variable");
        assertFalse(session.diagnostics(EditorSession.Channel.EDIT).isEmpty(), "the reason is reported");
        assertFalse(Renames.param(session, "Title", "1bad"));
        assertFalse(Renames.param(session, "Missing", "Other"));
        assertEquals(steps, session.undoCount());
        assertTrue(session.text().contains("{Param Title}") && session.text().contains("Name=\"Title\""));
        assertTrue(clean(), "the document is exactly as it was");
    }
}

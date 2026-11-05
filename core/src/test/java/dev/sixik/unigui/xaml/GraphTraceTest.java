package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.logic.GraphTrace;
import dev.sixik.unigui.xaml.preview.ComponentPreview;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GraphTraceTest {
    private static final String COMPONENT = """
            <Component x:Name="Traced" xmlns:x="https://unigui.dev/xaml">
              <Component.Contract>
                <Param Name="Title" Type="string" Default="idle"/>
                <Param Name="Count" Type="int" Default="1"/>
                <Event Name="Go" Direction="In" Args="n:int"/>
              </Component.Contract>
              <Label text="{Param Title}"/>
              <Component.Graph>
                <Node Id="in" Type="Event.Go"/>
                <Node Id="add" Type="Math.Add" b="10"/>
                <Node Id="set" Type="Param.Set" Name="Count"/>
                <Wire From="in.then" To="set.exec"/>
                <Wire From="in.n" To="add.a"/>
                <Wire From="add.result" To="set.value"/>
              </Component.Graph>
            </Component>""";

    private static ComponentPreview shown(String xaml) {
        ComponentPreview preview = new ComponentPreview(Xaml.defaultSchema(), LoadOptions.DEFAULT);
        assertTrue(preview.show(XamlFormat.INSTANCE.read(xaml)).shown(), preview.error());
        return preview;
    }

    @Test
    void aRunRecordsTheNodesInOrderWithTheirOutputs() {
        try (ComponentPreview preview = shown(COMPONENT)) {
            preview.send("Go", Args.of("n", 5));
            GraphTrace.Run run = preview.trace().last();
            assertNotNull(run);
            assertEquals("in", run.entry());
            assertEquals("", run.error());

            // «in» стартует, у «set» запрашивают значение, и «add» считается в момент запроса.
            assertEquals("in", run.steps().get(0).nodeId());
            assertEquals("Event.Go", run.steps().get(0).type());
            GraphTrace.Step add = run.steps().stream().filter(step -> step.nodeId().equals("add")).findFirst().orElseThrow();
            assertTrue(add.pure());
            assertEquals("15.0", add.outputs().get("result"));
            GraphTrace.Step set = run.steps().stream().filter(step -> step.nodeId().equals("set")).findFirst().orElseThrow();
            assertFalse(set.pure());
            assertEquals(java.util.List.of("then"), set.continuations());
            assertEquals(15, preview.host().orElseThrow().instance().get("Count"));
        }
    }

    @Test
    void everyEventIsItsOwnRunAndRebuildingClearsTheRecord() {
        try (ComponentPreview preview = shown(COMPONENT)) {
            int before = preview.trace().runs().size();
            preview.send("Go", Args.of("n", 1));
            preview.send("Go", Args.of("n", 2));
            assertEquals(before + 2, preview.trace().runs().size());

            preview.show(XamlFormat.INSTANCE.read(COMPONENT));
            assertTrue(preview.trace().runs().isEmpty(), "a new instance starts with an empty record");
        }
    }

    @Test
    void aFailingRunIsRecordedWithTheNodeThatFailed() throws Exception {
        String failing = COMPONENT.replace("<Wire From=\"in.then\" To=\"set.exec\"/>",
                "<Node Id=\"boom\" Type=\"Call.trace.boom\"/><Wire From=\"in.then\" To=\"boom.exec\"/>");
        try (AutoCloseable ignored = dev.sixik.unigui.xaml.logic.GraphFunctions.register(
                dev.sixik.unigui.xaml.logic.GraphFunction.define("trace.boom").body(args -> {
                    throw new IllegalStateException("boom");
                }));
             ComponentPreview preview = shown(failing)) {
            try {
                preview.send("Go", Args.of("n", 1));
            } catch (RuntimeException expected) {
                // ошибка может дойти до вызывающего; запись должна быть сделана в любом случае
            }
            GraphTrace.Run run = preview.trace().last();
            assertNotNull(run);
            assertTrue(run.error().contains("boom"), run.error());
            assertEquals("boom", run.failedNode());
        }
    }

    @Test
    void theRecordIsBounded() {
        try (ComponentPreview preview = shown(COMPONENT)) {
            for (int i = 0; i < GraphTrace.MAX_RUNS + 10; i++) preview.send("Go", Args.of("n", i));
            assertEquals(GraphTrace.MAX_RUNS, preview.trace().runs().size());
        }
    }

    @Test
    void listenersHearEveryFinishedRun() {
        try (ComponentPreview preview = shown(COMPONENT)) {
            int[] heard = {0};
            preview.trace().onChange(() -> heard[0]++);
            preview.send("Go", Args.of("n", 1));
            assertEquals(1, heard[0]);
        }
    }
}

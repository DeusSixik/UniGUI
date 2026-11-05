package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.component.ComponentInstance;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.logic.GraphDebugger;
import dev.sixik.unigui.xaml.preview.ComponentPreview;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GraphDebuggerTest {
    private static final String COMPONENT = """
            <Component x:Name="Debugged" xmlns:x="https://unigui.dev/xaml">
              <Component.Contract>
                <Param Name="A" Type="string" Default="-"/>
                <Param Name="B" Type="string" Default="-"/>
                <Param Name="C" Type="string" Default="-"/>
                <Event Name="Go" Direction="In" Args="v:string"/>
              </Component.Contract>
              <Label text="{Param A}"/>
              <Component.Graph>
                <Node Id="in" Type="Event.Go"/>
                <Node Id="first" Type="Param.Set" Name="A"/>
                <Node Id="second" Type="Param.Set" Name="B" value="2"/>
                <Node Id="third" Type="Param.Set" Name="C" value="3"/>
                <Wire From="in.v" To="first.value"/>
                <Wire From="in.then" To="first.exec"/>
                <Wire From="first.then" To="second.exec"/>
                <Wire From="second.then" To="third.exec"/>
              </Component.Graph>
            </Component>""";

    private ComponentPreview preview;

    private ComponentInstance shown() {
        preview = new ComponentPreview(Xaml.defaultSchema(), LoadOptions.DEFAULT);
        assertTrue(preview.show(XamlFormat.INSTANCE.read(COMPONENT)).shown(), preview.error());
        return preview.host().orElseThrow().instance();
    }

    private static String param(ComponentInstance instance, String name) {
        return String.valueOf(instance.get(name));
    }

    @Test
    void aRunStopsBeforeTheBreakpointNodeAndContinuesOnResume() {
        ComponentInstance instance = shown();
        GraphDebugger debugger = preview.debugger();
        debugger.set("second");

        preview.send("Go", Args.of("v", "x1"));
        assertEquals("x1", param(instance, "A"), "the nodes before the breakpoint ran");
        assertEquals("-", param(instance, "B"), "the breakpoint node has not run");
        GraphDebugger.Pause pause = debugger.paused().orElseThrow();
        assertEquals("second", pause.nodeId());
        assertEquals("in", pause.entry());
        assertEquals("Param.Set", pause.type());

        debugger.resume();
        assertEquals("2", param(instance, "B"));
        assertEquals("3", param(instance, "C"));
        assertTrue(debugger.paused().isEmpty());
    }

    @Test
    void stepRunsOneNodeAndStopsBeforeTheNext() {
        ComponentInstance instance = shown();
        GraphDebugger debugger = preview.debugger();
        debugger.set("first");
        preview.send("Go", Args.of("v", "x1"));
        assertEquals("first", debugger.paused().orElseThrow().nodeId());
        assertEquals("-", param(instance, "A"));

        debugger.step();
        assertEquals("x1", param(instance, "A"));
        assertEquals("second", debugger.paused().orElseThrow().nodeId(), "stepping stops before the next node without a breakpoint");
        assertEquals("-", param(instance, "B"));

        debugger.step();
        assertEquals("2", param(instance, "B"));
        debugger.step();
        assertEquals("3", param(instance, "C"));
        assertTrue(debugger.paused().isEmpty(), "the run finished");
    }

    @Test
    void eventsThatArriveWhileStoppedWaitForTheRunToFinish() {
        ComponentInstance instance = shown();
        GraphDebugger debugger = preview.debugger();
        debugger.set("second");
        preview.send("Go", Args.of("v", "x1"));
        preview.send("Go", Args.of("v", "x2"));
        assertEquals("x1", param(instance, "A"), "the second event is queued, not run in the middle of the first");

        debugger.resume(); // доводит первый запуск и тут же запускает второй, который снова встаёт на точке
        assertEquals("x2", param(instance, "A"));
        assertEquals("second", debugger.paused().orElseThrow().nodeId(), "the queued run stops at the same breakpoint");
        debugger.toggle("second");
        debugger.resume();
        assertEquals("3", param(instance, "C"));
        assertTrue(debugger.paused().isEmpty());
    }

    @Test
    void breakpointsCanBeToggledAndSurviveARebuild() {
        shown();
        GraphDebugger debugger = preview.debugger();
        assertTrue(debugger.toggle("second"));
        assertTrue(debugger.isBreakpoint("second"));
        assertTrue(preview.show(XamlFormat.INSTANCE.read(COMPONENT)).shown());
        assertTrue(debugger.isBreakpoint("second"), "the editor's breakpoints outlive the preview instance");
        assertFalse(debugger.toggle("second"));
        debugger.set("a");
        debugger.set("b");
        debugger.retain(java.util.Set.of("a"));
        assertEquals(java.util.Set.of("a"), debugger.breakpoints());
        debugger.clearBreakpoints();
        assertTrue(debugger.breakpoints().isEmpty());
    }

    @Test
    void withoutBreakpointsRunsFinishAtOnceAndListenersHearChanges() {
        ComponentInstance instance = shown();
        GraphDebugger debugger = preview.debugger();
        int[] heard = {0};
        debugger.onChange(() -> heard[0]++);
        preview.send("Go", Args.of("v", "x1"));
        assertEquals("3", param(instance, "C"));
        assertTrue(debugger.paused().isEmpty());

        debugger.set("second");
        preview.send("Go", Args.of("v", "x2"));
        assertTrue(heard[0] >= 2, "setting a breakpoint and stopping both notify");
    }
}

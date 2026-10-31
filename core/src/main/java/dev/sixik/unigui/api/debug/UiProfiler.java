package dev.sixik.unigui.api.debug;

import dev.sixik.unigui.api.docs.AiDoc;

@AiDoc(
        summary = "Frame profiler abstraction exposed by `UIContext`.",
        purpose = "Creates named, closeable timing scopes without coupling UI code to the built-in profiler implementation.",
        useWhen = "Instrument custom widget, layout, or rendering work that should appear in profiler snapshots.",
        criticalRules = {
                "Always close the `ProfileScope`, preferably with try-with-resources, or the timing sample remains incomplete.",
                "Call `beginFrame` once at the frame boundary before opening scopes when implementing a profiler.",
                "`NOOP` is safe and intentionally produces empty snapshots."
        },
        related = {"ProfileScope", "ProfilerSnapshot", "UIContext"},
        examples = @AiDoc.Example(
                title = "Time custom work",
                code = """
                        try (ProfileScope ignored = context.profiler().scope("custom-layout")) {
                            performLayout();
                        }
                        """))
public interface UiProfiler {
    UiProfiler NOOP = name -> ProfileScope.NOOP;

    default void beginFrame(long frameIndex) {
    }

    @AiDoc(section = "Instrumentation", sectionOrder = 10, order = 10,
            summary = "Opens a named timing scope that must be closed after the measured work.")
    ProfileScope scope(String name);

    @AiDoc(section = "Reading", sectionOrder = 20, order = 10,
            summary = "Returns the latest immutable profiler snapshot.")
    default ProfilerSnapshot snapshot() {
        return ProfilerSnapshot.EMPTY;
    }
}

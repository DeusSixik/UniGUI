package dev.sixik.unigui.cleanroom.backend;

interface CleanroomRenderHost {
    int logicalWidth();

    int logicalHeight();

    float backendScale();

    float opacity();
}

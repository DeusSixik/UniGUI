package dev.sixik.unigui.widgets.rendering;

import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.xaml.Xaml;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.preview.Preview;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Проверяет настройки камеры и создание Scene3D через путь предпросмотра редактора. */
final class Scene3DXamlTest {
    /** Допуск при конвертации углов из градусов в радианы и обратно, в градусах. */
    private static final double DEGREES_EPSILON = 1e-9;

    /** Загружает позу камеры, параметры текстуры и наложенный UI. */
    @Test
    void preview_withCameraAndOverlay_createsConfiguredScene() {
        var document = XamlFormat.INSTANCE.read("""
                <Scene3D width="320" height="200" camera="fixed" yaw="30" pitch="25" distance="8"
                         updateMode="every-frame" resolutionScale="2">
                    <Label text="overlay"/>
                </Scene3D>
                """);
        try (Preview preview = new Preview(Xaml.defaultSchema(), LoadOptions.DEFAULT)) {
            assertTrue(preview.show(document).shown(), preview::error);
            Scene3D scene = assertInstanceOf(Scene3D.class, preview.root().orElseThrow());

            assertFalse(scene.cameraControls());
            assertEquals(30, Math.toDegrees(scene.orbit().yaw()), DEGREES_EPSILON);
            assertEquals(25, Math.toDegrees(scene.orbit().pitch()), DEGREES_EPSILON);
            assertEquals(8, scene.orbit().distance(), 0);
            assertEquals(RenderToTexture.UpdateMode.EVERY_FRAME, scene.updateMode());
            assertEquals(2, scene.resolutionScale(), 0);
            assertEquals("overlay", assertInstanceOf(Label.class, scene.content()).text());
        }
    }
}

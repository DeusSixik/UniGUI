package dev.sixik.unigui.widgets.rendering;

import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.xaml.Xaml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Разметка {@code <RenderToTexture>}.
 */
class RenderToTextureXamlTest {
    @Test
    void loadsPropertiesAndContent() {
        RenderToTexture rtt = Xaml.load("""
                <RenderToTexture updateMode="ON_CHANGE" resolutionScale="2" canvasSize="64x32"
                                 depth="true" displayed="false" fit="CONTAIN" filter="NEAREST">
                    <Label text="inside" />
                </RenderToTexture>
                """, RenderToTexture.class);

        assertEquals(RenderToTexture.UpdateMode.ON_CHANGE, rtt.updateMode());
        assertEquals(2.0f, rtt.resolutionScale(), 1.0e-6f);
        assertTrue(rtt.hasCanvasSize());
        assertTrue(rtt.targetOptions().useDepth());
        assertFalse(rtt.displayed());
        assertEquals(ImageFit.CONTAIN, rtt.fit());
        assertEquals(TextureFilter.NEAREST, rtt.filter());
        assertInstanceOf(Label.class, rtt.content());
    }
}

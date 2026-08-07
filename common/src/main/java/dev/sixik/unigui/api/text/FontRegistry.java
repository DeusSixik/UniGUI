package dev.sixik.unigui.api.text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

/** Registry for fonts loaded without the Minecraft resource or font managers. */
public interface FontRegistry {
    FontFace register(String id, Path path) throws IOException;

    FontFace register(String id, InputStream input) throws IOException;

    FontFace register(String id, byte[] data) throws IOException;

    FontFace find(String id);

    FontFace defaultFace();

    void defaultFace(FontFace face);

    Map<String, FontFace> faces();
}

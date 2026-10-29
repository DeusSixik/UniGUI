package dev.sixik.unigui.api.text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

/**
 * Registry шрифтов, загруженных независимо от Minecraft resource/font managers.
 *
 * <p>Этот контракт нужен для standalone renderer'ов, editor preview и случаев, когда UniGUI должен
 * работать с собственным набором шрифтов. Registry хранит {@link FontFace} по строковому id и
 * предоставляет default face для виджетов, у которых не указан явный шрифт.</p>
 *
 * @see Fonts#global()
 */
public interface FontRegistry {
    /**
     * Загружает font face из файла.
     *
     * @param id стабильный id шрифта внутри registry
     * @param path путь к font file
     * @return зарегистрированный face
     * @throws IOException если файл нельзя прочитать или распарсить
     */
    FontFace register(String id, Path path) throws IOException;

    /**
     * Загружает font face из stream'а.
     *
     * @param id стабильный id шрифта внутри registry
     * @param input stream с font data; владение закрытием зависит от реализации
     * @return зарегистрированный face
     * @throws IOException если stream нельзя прочитать или распарсить
     */
    FontFace register(String id, InputStream input) throws IOException;

    /**
     * Загружает font face из массива байтов.
     *
     * @param id стабильный id шрифта внутри registry
     * @param data font data
     * @return зарегистрированный face
     * @throws IOException если данные нельзя распарсить
     */
    FontFace register(String id, byte[] data) throws IOException;

    /**
     * Ищет зарегистрированный font face.
     *
     * @param id id шрифта
     * @return face или {@code null}, если id неизвестен
     */
    FontFace find(String id);

    /**
     * Возвращает default face для text widgets.
     *
     * @return default font face
     */
    FontFace defaultFace();

    /**
     * Назначает default face для виджетов без явного font.
     *
     * @param face новый default face
     */
    void defaultFace(FontFace face);

    /**
     * Возвращает snapshot или read-only view зарегистрированных шрифтов.
     *
     * @return map {@code fontId -> fontFace}
     */
    Map<String, FontFace> faces();
}
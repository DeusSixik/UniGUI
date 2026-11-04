package dev.sixik.unigui.xaml.format;

import dev.sixik.unigui.xaml.UiDocument;

/**
 * Формат XAML: чтение и запись {@link UiDocument}.
 *
 * <p>Реализация без состояния, поэтому экземпляр {@link #INSTANCE} безопасно разделять между потоками.</p>
 *
 * <pre>{@code
 * UiDocument document = XamlFormat.INSTANCE.read("<VBox><Button Content=\"Save\"/></VBox>");
 * String text = XamlFormat.INSTANCE.write(document);
 * }</pre>
 */
public final class XamlFormat implements UiFormat {
    /** Общий экземпляр формата. */
    public static final XamlFormat INSTANCE = new XamlFormat();

    private XamlFormat() {
    }

    @Override
    public String id() {
        return "xaml";
    }

    @Override
    public UiDocument read(CharSequence source, ReadOptions options) {
        if (source == null) throw new IllegalArgumentException("XAML source must not be null");
        ReadOptions normalized = options == null ? ReadOptions.DEFAULT : options;
        return new XamlReader(source, normalized.maxDepth()).read();
    }

    @Override
    public String write(UiDocument document, WriteOptions options) {
        if (document == null) throw new IllegalArgumentException("document must not be null");
        return XamlWriter.write(document, options == null ? WriteOptions.PRETTY : options);
    }
}

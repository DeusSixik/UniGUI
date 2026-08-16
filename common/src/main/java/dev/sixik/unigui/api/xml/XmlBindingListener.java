package dev.sixik.unigui.api.xml;

@FunctionalInterface
public interface XmlBindingListener<T> {
    void changed(XmlBindingChange<T> change);
}

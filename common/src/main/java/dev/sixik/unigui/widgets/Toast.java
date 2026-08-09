package dev.sixik.unigui.widgets;

public final class Toast extends NotificationView {
    public Toast() {
        super();
    }

    public Toast(String text) {
        super(text);
    }

    @Override
    public Toast duration(float seconds) {
        super.duration(seconds);
        return this;
    }

    @Override
    public Toast toast(String text) {
        super.toast(text);
        return this;
    }
}

package dev.sixik.unigui.demo.ui.data;

public record SolarNavigationShipState(float shipX, float shipY, float velocityX, float velocityY, float angle) {

    public static final SolarNavigationShipState DEFAULT = new SolarNavigationShipState(0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

}

package dev.sixik.unigui.demo.ui.data;

public record SolarNavigationStationInfo(String name, String code, float x, float y, float radius, boolean quest, long seed, int color, float distance) {

    public SolarNavigationStationInfo {
        name = name == null || name.isBlank() ? "Unknown Station" : name;
        code = code == null || code.isBlank() ? StationCodeGenerator.code(seed, x, y) : code;
        radius = Math.max(1.0F, radius);
        distance = Math.max(0.0F, distance);
    }

}

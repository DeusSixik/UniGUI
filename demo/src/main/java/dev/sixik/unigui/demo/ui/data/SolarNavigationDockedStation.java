package dev.sixik.unigui.demo.ui.data;

public record SolarNavigationDockedStation(long seed, String name, String code, float x, float y) {

    public SolarNavigationDockedStation {
        name = name == null || name.isBlank() ? "Unknown Station" : name;
        code = code == null || code.isBlank() ? StationCodeGenerator.code(seed, x, y) : code;
    }

}

package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.demo.ui.DemoAssets;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;

public class TestUI {

    public static void open() {
        UniScreen screen = new UniScreen(create());
        screen.openGui();
    }

    private static Widget create() {

        VBox vbox = new VBox();

        vbox.addChild(
                new Label("Удерживайте {key:key.attack} для перемещения по полю.")
        );
        vbox.addChild(
                new Label(RichText.builder()
                        .append("HP: ")
                        .append("20", s -> s.color(new MutableColor(1, 0, 0, 1)).size(12))
                        .styled(s -> s.color(new MutableColor(1, 0.5f, 1, 1)), b -> b.append(" Награда: ")
                                .icon("coin", DemoAssets.texture(
                                        "unigui_testmod:keys/gui_background",
                                        "assets/testmod/textures/gui_background.png",
                                        TextureOptions.defaults())).append(" 50"))
                        .append(" — готово")   // снова базовый стиль
                        .build())
        );

        return vbox;
    }
}

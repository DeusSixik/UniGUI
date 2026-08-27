package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.interaction.AdminCommandRegistry;
import dev.sixik.unigui.widgets.interaction.AdminConsole;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AdminConsoleDemo {
    private static final List<String> DEMO_PLAYERS = new ArrayList<>(List.of("Steve", "Alex", "Sixik", "Operator"));
    private static final List<String> RELOAD_TARGETS = List.of("styles", "textures", "layout", "all");
    private static final List<String> TELEPORT_COORDINATES = List.of("~", "0", "64", "100", "-100");
    private static final List<String> DEMO_COMMAND_NAMES = List.of("ping", "status", "debug", "profile");

    private AdminConsoleDemo() {
    }

    public static void openDemoClient() {
        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
        AdminConsole console = createConsole();
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(
                Component.literal("UniGUI Admin Console"),
                console,
                context);
        screen.renderPolicy(UiRenderPolicy.vsync());
        screen.scaleWithMinecraftGui(true);
        Minecraft.getInstance().setScreen(screen);
    }

    public static AdminConsole createConsole() {
        AdminConsole console = new AdminConsole()
                .title("UniGUI Admin Console")
                .prompt("/")
                .maxOutputLines(300);
        console.onCloseRequested(event -> Minecraft.getInstance().setScreen(null));

        console.registerCommand(AdminCommandRegistry.command("reload", "Reload selected subsystem")
                .argument("target", "Subsystem name", RELOAD_TARGETS)
                .executor((view, invocation) -> {
                    String target = invocation.arguments().isEmpty() ? "all" : invocation.arguments().get(0);
                    view.appendInfo("Reload requested: " + target);
                })
                .build());

        console.registerCommand(AdminCommandRegistry.command("teleport", "Mock teleport command")
                .argument("player", "Online player", () -> DEMO_PLAYERS)
                .customArgument("x", "X coordinate", TELEPORT_COORDINATES)
                .customArgument("y", "Y coordinate", TELEPORT_COORDINATES)
                .customArgument("z", "Z coordinate", TELEPORT_COORDINATES)
                .executor((view, invocation) -> {
                    if (invocation.arguments().size() < 4) {
                        view.appendWarning("Usage: teleport <player> <x> <y> <z>");
                        return;
                    }
                    view.appendInfo("Teleport " + invocation.arguments().get(0)
                            + " -> " + invocation.arguments().get(1)
                            + ", " + invocation.arguments().get(2)
                            + ", " + invocation.arguments().get(3));
                })
                .build());

        console.registerCommand(AdminCommandRegistry.command("ban", "Mock moderation command")
                .argument("player", "Online player", () -> DEMO_PLAYERS)
                .executor((view, invocation) -> {
                    if (invocation.arguments().isEmpty()) {
                        view.appendWarning("Usage: ban <player> [reason]");
                        return;
                    }
                    String reason = invocation.arguments().size() > 1
                            ? String.join(" ", invocation.arguments().subList(1, invocation.arguments().size()))
                            : "no reason";
                    view.appendError("Banned " + invocation.arguments().get(0) + " (demo): " + reason);
                })
                .build());

        console.registerCommand(AdminCommandRegistry.command("say", "Broadcast a demo message")
                .customArgument("message", "Message template", List.of("hello", "server restart in 5 minutes", "maintenance mode"))
                .executor((view, invocation) -> view.appendOutput("[Broadcast] " + String.join(" ", invocation.arguments())))
                .build());

        console.registerCommand(AdminCommandRegistry.command("players", "List demo online players")
                .executor((view, invocation) -> view.appendInfo("Online: " + String.join(", ", DEMO_PLAYERS)))
                .build());

        console.registerCommand(AdminCommandRegistry.command("playeradd", "Add player to dynamic completion list")
                .customArgument("name", "Player name", List.of("Herobrine", "Notch", "Builder", "Guest"))
                .executor((view, invocation) -> {
                    if (invocation.arguments().isEmpty()) {
                        view.appendWarning("Usage: playeradd <name>");
                        return;
                    }
                    String player = invocation.arguments().get(0);
                    if (!containsIgnoreCase(DEMO_PLAYERS, player)) {
                        DEMO_PLAYERS.add(player);
                        view.appendInfo("Player added to completion list: " + player);
                    } else {
                        view.appendWarning("Player already exists: " + player);
                    }
                })
                .build());

        console.registerCommand(AdminCommandRegistry.command("playerremove", "Remove player from dynamic completion list")
                .argument("name", "Online player", () -> DEMO_PLAYERS)
                .executor((view, invocation) -> {
                    if (invocation.arguments().isEmpty()) {
                        view.appendWarning("Usage: playerremove <name>");
                        return;
                    }
                    String player = invocation.arguments().get(0);
                    if (DEMO_PLAYERS.removeIf(value -> value.equalsIgnoreCase(player))) {
                        view.appendInfo("Player removed from completion list: " + player);
                    } else {
                        view.appendWarning("Unknown player: " + player);
                    }
                })
                .build());

        console.registerCommand(AdminCommandRegistry.command("registerdemo", "Register command at runtime")
                .customArgument("name", "New command name", DEMO_COMMAND_NAMES)
                .executor((view, invocation) -> {
                    if (invocation.arguments().isEmpty()) {
                        view.appendWarning("Usage: registerdemo <name>");
                        return;
                    }
                    String name = normalizeCommandName(invocation.arguments().get(0));
                    if (name.isEmpty()) {
                        view.appendWarning("Command name is empty");
                        return;
                    }
                    view.registerCommand(AdminCommandRegistry.command(name, "Runtime generated demo command")
                            .executor((runtimeView, runtimeInvocation) -> runtimeView.appendInfo(
                                    "Runtime command '" + name + "' executed with args: "
                                            + String.join(" ", runtimeInvocation.arguments())))
                            .build());
                    view.appendInfo("Runtime command registered: " + name);
                })
                .build());

        console.registerCommand(AdminCommandRegistry.command("unregisterdemo", "Unregister command at runtime")
                .argument("name", "Registered command", () -> console.registeredCommands().keySet())
                .executor((view, invocation) -> {
                    if (invocation.arguments().isEmpty()) {
                        view.appendWarning("Usage: unregisterdemo <name>");
                        return;
                    }
                    String name = normalizeCommandName(invocation.arguments().get(0));
                    view.unregisterCommand(name);
                    view.appendInfo("Command unregistered if it existed: " + name);
                })
                .build());

        console.appendInfo("Try: teleport <player> <x> <y> <z>. Press Tab after every argument.");
        console.appendInfo("Dynamic demo: playeradd Guest, playerremove Steve, registerdemo ping.");
        return console;
    }

    private static boolean containsIgnoreCase(List<String> values, String needle) {
        for (String value : values) {
            if (value.equalsIgnoreCase(needle)) return true;
        }
        return false;
    }

    private static String normalizeCommandName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
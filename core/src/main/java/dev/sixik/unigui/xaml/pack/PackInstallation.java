package dev.sixik.unigui.xaml.pack;

import dev.sixik.unigui.xaml.component.Components;
import dev.sixik.unigui.xaml.struct.StructType;
import dev.sixik.unigui.xaml.struct.Structs;

import java.util.ArrayList;
import java.util.List;

/**
 * Результат установки пака: что зарегистрировано и что не удалось.
 *
 * <p>Установка не обрывается на первой ошибке: исправные файлы ставятся, а для остальных в {@link #problems()}
 * записано, какой файл и почему. {@link #close()} снимает всё, что поставила установка, — пак можно выгрузить или
 * заменить новой версией без перезапуска.</p>
 */
public final class PackInstallation implements AutoCloseable {
    private final AssetPack pack;
    private final List<String> structs = new ArrayList<>();
    private final List<String> components = new ArrayList<>();
    private final List<String> problems = new ArrayList<>();
    private boolean closed;

    private PackInstallation(AssetPack pack) {
        this.pack = pack;
    }

    static PackInstallation install(AssetPack pack) {
        PackInstallation installation = new PackInstallation(pack);
        installation.installStructs();
        installation.installComponents();
        return installation;
    }

    /** Структуры ссылаются друг на друга, поэтому ставятся проходами, пока что-то ещё удаётся поставить. */
    private void installStructs() {
        List<String> pending = new ArrayList<>(pack.names("structs/"));
        pending.removeIf(name -> !name.endsWith(".xaml"));
        List<String> failures = new ArrayList<>();
        boolean progress = true;
        while (progress && !pending.isEmpty()) {
            progress = false;
            failures.clear();
            for (java.util.Iterator<String> it = pending.iterator(); it.hasNext(); ) {
                String path = it.next();
                try {
                    StructType type = StructType.parse(pack.text(path).orElseThrow());
                    Structs.register(type);
                    structs.add(type.name());
                    it.remove();
                    progress = true;
                } catch (RuntimeException failure) {
                    failures.add(path + ": " + failure.getMessage());
                }
            }
        }
        problems.addAll(failures);
    }

    private void installComponents() {
        for (String path : pack.names("components/")) {
            if (!path.endsWith(".xaml")) continue;
            String file = path.substring(path.lastIndexOf('/') + 1, path.length() - ".xaml".length());
            String id = pack.manifest().id() + "/" + file;
            try {
                Components.register(id, pack.text(path).orElseThrow());
                components.add(id);
            } catch (RuntimeException failure) {
                problems.add(path + ": " + failure.getMessage());
            }
        }
    }

    /** @return пак, который установлен */
    public AssetPack pack() {
        return pack;
    }

    /** @return имена установленных структур */
    public List<String> structs() {
        return List.copyOf(structs);
    }

    /** @return идентификаторы установленных компонентов ({@code packId/файл}) */
    public List<String> components() {
        return List.copyOf(components);
    }

    /** @return проблемы: файл и причина; пусто, если всё поставилось */
    public List<String> problems() {
        return List.copyOf(problems);
    }

    /** @return {@code true}, если проблем нет */
    public boolean clean() {
        return problems.isEmpty();
    }

    /**
     * Требует чистой установки.
     *
     * @return эта установка
     * @throws IllegalStateException если были проблемы; сообщение перечисляет их все
     */
    public PackInstallation requireClean() {
        if (!problems.isEmpty()) {
            close();
            throw new IllegalStateException("Pack " + pack.manifest().id() + " did not install cleanly:\n  "
                    + String.join("\n  ", problems));
        }
        return this;
    }

    /** Снимает регистрацию установленных компонентов и структур. */
    @Override
    public void close() {
        if (closed) return;
        closed = true;
        for (String id : components) Components.unregister(id);
        for (String name : structs) Structs.unregister(name);
        components.clear();
        structs.clear();
    }
}

package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.TextureHandle;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * LRU-кэш texture binding'ов GUI-бэкенда: повторный resolve того же хэндла
 * обязан вернуть тот же объект без новой аллокации, смена options — новый,
 * вытеснение — перестроить.
 *
 * <p>Кэш приватный, поэтому тест идёт через рефлексию и оперирует ссылками:
 * используются только Integer-native хэндлы, на пути resolve нет
 * Minecraft-классов.</p>
 */
public final class TextureBindingCacheSelfTest {
    public static void main(String[] args) throws Exception {
        Class<?> cacheClass = Class.forName(
                "dev.sixik.unigui.backend.minecraft_impl.MinecraftGuiRenderBackend$TextureBindingCache");
        Constructor<?> constructor = cacheClass.getDeclaredConstructor(int.class);
        constructor.setAccessible(true);
        Method resolve = cacheClass.getDeclaredMethod("resolve", TextureHandle.class);
        resolve.setAccessible(true);

        Object cache = constructor.newInstance(2);
        TextureHandle first = new SimpleTextureHandle("cache:a", 8, 8, (Object) 11);
        TextureHandle second = new SimpleTextureHandle("cache:b", 8, 8, (Object) 22);

        Object firstResolve = resolve.invoke(cache, first);
        Object secondResolve = resolve.invoke(cache, second);
        check(firstResolve != null && secondResolve != null, "resolve should bind integer natives");
        check(firstResolve != secondResolve, "distinct handles should get distinct bindings");

        // Хит: тот же хэндл — тот же объект, без нового resolve.
        check(resolve.invoke(cache, first) == firstResolve, "cache should hit same handle");
        check(resolve.invoke(cache, second) == secondResolve, "cache should hit second handle");

        // Вытеснение при capacity=2: третий хэндл выбивает least-recently-used (first).
        TextureHandle third = new SimpleTextureHandle("cache:c", 8, 8, (Object) 33);
        Object thirdResolve = resolve.invoke(cache, third);
        check(thirdResolve != null, "third handle should resolve");
        check(resolve.invoke(cache, third) == thirdResolve, "third handle should hit");
        check(resolve.invoke(cache, second) == secondResolve, "recently used entry should survive");
        check(resolve.invoke(cache, first) != firstResolve, "evicted entry should rebuild");

        System.out.println("TextureBindingCacheSelfTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

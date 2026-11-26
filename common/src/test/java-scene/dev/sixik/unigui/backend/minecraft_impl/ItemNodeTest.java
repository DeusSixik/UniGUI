package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Aabb;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Проверяет границы пустого ItemNode и явное переопределение. */
final class ItemNodeTest {
    /** Загружает реестры, чтобы создать тестовые стеки. */
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /** Проверяет приоритет пользовательского AABB и возврат к отсутствию границ после сброса. */
    @Test
    void bounds_explicitOverride_staysUntilCleared() {
        ItemNode node = new ItemNode(ItemStack.EMPTY);
        Aabb custom = new Aabb(0, 0, 0, 2, 1, 1);

        assertNull(node.bounds());
        node.setBounds(custom);

        assertSame(custom, node.bounds());
        node.setBounds(null);
        assertNull(node.bounds());
    }
}

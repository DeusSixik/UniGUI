package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.load.DocumentValidator;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.schema.Converters;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.SchemaContributions;
import dev.sixik.unigui.xaml.schema.TypeInfo;
import dev.sixik.unigui.xaml.snapshot.Snapshotter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SchemaContributionsTest {
    private static final String SERVICE_DOCUMENT = """
            <VBox xmlns:t="urn:unigui-test:service">
                <t:ServiceBadge id="badge" text="from a mod" importance="3"/>
            </VBox>
            """;

    @Test
    void contributorsDeclaredInMetaInfServicesAreDiscovered() {
        VBox root = Xaml.load(SERVICE_DOCUMENT, VBox.class);
        ServiceTestContributor.ServiceBadge badge = Xaml.get(root, "badge", ServiceTestContributor.ServiceBadge.class);
        assertEquals("from a mod", badge.text());
        assertEquals(3.0f, badge.importance());
        assertTrue(SchemaContributions.contributors().stream().anyMatch(c -> c instanceof ServiceTestContributor));
    }

    @Test
    void aBrokenServiceProviderIsReportedAndDoesNotPreventTheOthers() {
        assertTrue(SchemaContributions.loadFailures().stream().anyMatch(m -> m.contains("ThisClassDoesNotExist")),
                SchemaContributions.loadFailures().toString());
        assertTrue(Xaml.defaultSchema().contains(new UiName(ServiceTestContributor.NAMESPACE, "ServiceBadge", "t")));
    }

    @Test
    void contributedTypesStayInTheirOwnNamespace() {
        assertTrue(Xaml.defaultSchema().type("ServiceBadge").isEmpty(), "not visible without the namespace");
        assertThrows(XamlLoadException.class, () -> Xaml.load("<ServiceBadge/>"));
        assertFalse(Schema.builtIns().contains(new UiName(ServiceTestContributor.NAMESPACE, "ServiceBadge", "")),
                "a pure built-ins schema is not affected by global contributions");
    }

    @Test
    void validatorAndSnapshotterUnderstandContributedNamespaces() {
        DocumentValidator.Result result = new DocumentValidator(Xaml.defaultSchema()).validate(SERVICE_DOCUMENT);
        assertTrue(result.valid(), result.diagnostics().toString());

        Widget loaded = Xaml.load(SERVICE_DOCUMENT);
        Snapshotter.Result snapshot = new Snapshotter(Xaml.defaultSchema()).snapshot(loaded);
        String text = XamlFormat.INSTANCE.write(snapshot.document());
        assertTrue(text.contains("xmlns:svc=\"urn:unigui-test:service\"") && text.contains("<svc:ServiceBadge"),
                "the root declares the prefix of every contributed namespace it uses:\n" + text);

        Widget reloaded = Xaml.load(text);
        assertEquals(TreeDump.dump(loaded), TreeDump.dump(reloaded));
        assertEquals(3.0f, Xaml.get(reloaded, "badge", ServiceTestContributor.ServiceBadge.class).importance());
    }

    @Test
    void registeringAndClosingAContributionRebuildsTheDefaultSchema() throws Exception {
        Schema before = Xaml.defaultSchema();
        assertSame(before, Xaml.defaultSchema(), "the default schema is cached while nothing changes");

        AutoCloseable handle = SchemaContributions.register(schema -> schema.define("urn:unigui-test:dynamic", "Dyn",
                () -> TypeInfo.of(new UiName("urn:unigui-test:dynamic", "Dyn", "dyn"), VBox.class, VBox::new)
                        .property("spacing", Converters.FLOAT, VBox::spacing).build()));
        Schema during = Xaml.defaultSchema();
        assertNotSame(before, during);
        assertNotNull(Xaml.load("<dyn:Dyn xmlns:dyn=\"urn:unigui-test:dynamic\" spacing=\"5\"/>", VBox.class));
        assertEquals(5.0f, Xaml.load("<dyn:Dyn xmlns:dyn=\"urn:unigui-test:dynamic\" spacing=\"5\"/>", VBox.class).spacing());

        handle.close();
        assertNotSame(during, Xaml.defaultSchema());
        assertThrows(XamlLoadException.class,
                () -> Xaml.load("<dyn:Dyn xmlns:dyn=\"urn:unigui-test:dynamic\"/>"), "the type is gone after close");
    }

    @Test
    void annotatedWidgetsCanBeContributed() throws Exception {
        try (AutoCloseable ignored = SchemaContributions.registerAnnotated(XamlLoaderTest.AnnotatedBadge.class)) {
            XamlLoaderTest.AnnotatedBadge badge = Xaml.load("<AnnotatedBadge text=\"x\" importance=\"2\"/>",
                    XamlLoaderTest.AnnotatedBadge.class);
            assertEquals(2.0f, badge.importance());
        }
        assertThrows(XamlLoadException.class, () -> Xaml.load("<AnnotatedBadge/>"));
    }

    private static void assertNotNull(Object value) {
        org.junit.jupiter.api.Assertions.assertNotNull(value);
    }
}

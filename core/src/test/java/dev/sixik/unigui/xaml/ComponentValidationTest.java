package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.load.DocumentValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComponentValidationTest {
    private static final DocumentValidator VALIDATOR = new DocumentValidator(Xaml.defaultSchema());

    private static String component(String body) {
        return """
                <Component x:Name="Card" xmlns:x="https://unigui.dev/xaml">
                  <Component.Contract>
                    <Param Name="Title" Type="string" Default="idle"/>
                    <Param Name="Count" Type="int"/>
                    <Event Name="Buy" Direction="Out"/>
                    <Event Name="Refresh" Direction="In"/>
                  </Component.Contract>
                  %s
                </Component>""".formatted(body);
    }

    private static String problems(String body) {
        return VALIDATOR.validate(component(body)).diagnostics().toString();
    }

    @Test
    void aBodyThatUsesTheContractIsClean() {
        DocumentValidator.Result result = VALIDATOR.validate(component(
                "<Button text=\"{Param Title}\" onClick=\"{Raise Buy}\"/>"));
        assertTrue(result.valid(), result.diagnostics().toString());
    }

    @Test
    void anUnknownParameterIsReportedWithTheDeclaredNames() {
        String text = problems("<Label text=\"{Param Missing}\"/>");
        assertTrue(text.contains("no parameter 'Missing'") && text.contains("Title, Count"), text);
    }

    @Test
    void aParameterOfTheWrongTypeIsReported() {
        String text = problems("<Label text=\"{Param Title}\" visible=\"{Param Title}\"/>");
        assertTrue(text.contains("parameter 'Title' is string") && text.contains("visible"), text);
    }

    @Test
    void anUnknownOrIncomingEventCannotBeRaised() {
        assertTrue(problems("<Button onClick=\"{Raise Nope}\"/>").contains("no event 'Nope'"));
        assertTrue(problems("<Button onClick=\"{Raise Refresh}\"/>").contains("incoming"));
    }

    @Test
    void aMalformedUseIsReported() {
        assertTrue(problems("<Label text=\"{Param}\"/>").contains("requires a name"));
        assertTrue(problems("<Label text=\"{Param Title, Count}\"/>").contains("single name"));
    }
}

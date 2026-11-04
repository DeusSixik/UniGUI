package dev.sixik.unigui.xaml;

/**
 * Служебные пространства имён XAML.
 *
 * <p>«Языковой» namespace ({@code x:}) содержит конструкции, которые принадлежат самому формату, а не
 * виджетам: {@code x:Name}, {@code x:Key}, {@code x:String}. Принимаются два URI: собственный и привычный по
 * WPF/Noesis, чтобы документы можно было переносить без правок.</p>
 */
public final class XamlNamespaces {
    /** Собственный URI языковых конструкций. */
    public static final String LANGUAGE = "https://unigui.dev/xaml";
    /** URI из WPF/Noesis; принимается как синоним {@link #LANGUAGE}. */
    public static final String LANGUAGE_WPF = "http://schemas.microsoft.com/winfx/2006/xaml";

    private XamlNamespaces() {
    }

    /**
     * Проверяет, что namespace относится к языковым конструкциям.
     *
     * @param namespace URI
     * @return {@code true} для {@link #LANGUAGE} и {@link #LANGUAGE_WPF}
     */
    public static boolean isLanguage(String namespace) {
        return LANGUAGE.equals(namespace) || LANGUAGE_WPF.equals(namespace);
    }
}

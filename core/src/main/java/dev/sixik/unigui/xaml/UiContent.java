package dev.sixik.unigui.xaml;

/**
 * Элемент содержимого узла: вложенный узел, текст или комментарий.
 *
 * <p>Комментарии хранятся в дереве, чтобы редактор мог сохранить документ без потери пользовательских заметок.</p>
 */
public sealed interface UiContent permits UiNode, UiContent.Text, UiContent.Comment {
    /**
     * Текстовый фрагмент.
     *
     * @param value текст после раскрытия entity
     */
    record Text(String value) implements UiContent {
        public Text {
            value = value == null ? "" : value;
        }

        /**
         * Показывает, что текст состоит только из пробельных символов.
         *
         * @return {@code true}, если текст пуст или содержит одни пробелы
         */
        public boolean blank() {
            return value.isBlank();
        }
    }

    /**
     * Комментарий.
     *
     * @param value текст между {@code <!--} и {@code -->}
     */
    record Comment(String value) implements UiContent {
        public Comment {
            value = value == null ? "" : value;
        }
    }
}

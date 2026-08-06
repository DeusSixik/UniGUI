/**
 * Public Layout v2 contracts.
 *
 * <p>New code should configure a widget through
 * {@code widget.layout(style -> ...)}. The legacy {@code preferredSize},
 * {@code minSize}, {@code maxSize}, {@code margin}, {@code align} and
 * {@code grow} methods remain supported as focused convenience methods and
 * do not replace unrelated Layout v2 properties.</p>
 *
 * <pre>{@code
 * VBox content = new VBox();
 * content.layout(style -> style
 *         .padding(8.0f)
 *         .gap(6.0f)
 *         .overflowY(Overflow.AUTO));
 *
 * Button action = new Button("Action");
 * action.layout(style -> style
 *         .widthPercent(100.0f)
 *         .minHeight(22.0f)
 *         .flex(1.0f, 1.0f, SizeValue.auto()));
 * }</pre>
 */
package dev.sixik.unigui.api.layout;

# Wiki Page Generation

Wiki pages are authored on API types with annotations from `dev.sixik.unigui.api.docs`.
The generator runs as a build tool; annotations do not trigger work during application runtime.

```java
@WikiPage(
        title = "VirtualListView",
        slug = "virtual-list-view",
        category = "Data",
        summary = "Virtualized list and responsive grid.")
@WikiIcon(src = "assets/widgets/virtual-list.svg", alt = "Rows and grid cells")
@WikiGif(src = "assets/demos/virtual-list-grid.gif", alt = "Resizing the virtual grid")
@WikiSection(title = "Overview", markdown = "Authored Markdown content.", order = 10)
@WikiExample(title = "Create a grid", code = "new VirtualListView().grid(true);")
public class VirtualListView { ... }
```

Use repeatable `@WikiGif`, `@WikiSection`, and `@WikiExample` to add multiple assets,
content sections, or examples. Methods and constructors are published with `@WikiMethod`.
Use `@WikiParameter` on parameters, `@WikiReturn` on a non-void method, and repeatable
`@WikiThrows` for exception contracts. `@WikiExample` can be placed on either a type or
an individual documented member. Media `src` values are site-relative paths and are
emitted as-is; the generator does not copy or validate assets.

Generate Markdown for the current Minecraft profile:

```powershell
./gradlew :1.20.1:common:generateWiki
```

By default, output is written under `build/stonecutter/1.20.1/common/generated/wiki`.
The directory contains `index.md` and one page per annotated type in `pages/`. To write
directly into a site's content directory, set `wikiOutputDir`:

```powershell
./gradlew :1.20.1:common:generateWiki -PwikiOutputDir=../site/content/unigui
```

The output directory is not cleaned by the generator. Point it at a build-owned or
site-generated directory so removed annotations do not leave stale published pages.

## AI documentation

`@AiDoc` is intentionally independent from wiki annotations. It never appears in website
wiki pages, and a type does not need `@WikiPage` to participate in the AI collection.
`@AiDoc` can be applied to types, public methods, and public constructors.

```powershell
./gradlew :1.20.1:common:generateAiDocs
```

The default output is `build/stonecutter/1.20.1/common/generated/ai-docs`. Override it with:

```powershell
./gradlew :1.20.1:common:generateAiDocs -PaiDocsOutputDir=../ai-context/unigui
```

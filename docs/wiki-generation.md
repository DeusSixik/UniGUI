# Wiki Page Generation

Wiki pages are authored on API types with annotations from `dev.sixik.unigui.api.docs`.
The generator runs as a build tool; annotations do not trigger work during application runtime.

```java
@WikiPage(
        title = "VirtualListView",
        slug = "virtual-list-view",
        category = "<key:virtual.list.view.category>",
        summary = "<key:virtual.list.view.summary>")
@WikiIcon(src = "assets/widgets/virtual-list.svg", alt = "Rows and grid cells")
@WikiGif(
        src = "<key:virtual.list.view.gif.resize>",
        alt = "<key:virtual.list.view.gif.resize.alt>")
@WikiSection(title = "Overview", markdown = "Authored Markdown content.", order = 10)
@WikiExample(title = "Create a grid", code = "new VirtualListView().grid(true);")
public class VirtualListView { ... }
```

Use repeatable `@WikiGif`, `@WikiSection`, and `@WikiExample` to add multiple assets,
content sections, or examples. Methods and constructors are published with `@WikiMethod`.
Use `@WikiParameter` on parameters, `@WikiReturn` on a non-void method, and repeatable
`@WikiThrows` for exception contracts. `@WikiExample` can be placed on either a type or
an individual documented member. Media `src` values are site-relative paths and are
resolved per locale. Files from `docs/assets` are copied once into the shared generated
`assets` directory and are not duplicated per language.

## Localization

Language files live in `docs/lang/<locale>.json` and use the same flat JSON format as
Minecraft language files. English (`en.json`) is mandatory and is the fallback locale.

```json
{
  "virtual.list.view.category": "Widgets/Data",
  "virtual.list.view.summary": "Virtualized list and responsive grid",
  "virtual.list.view.gif.resize": "assets/demos/virtual-list-grid.gif",
  "virtual.list.view.gif.resize.alt": "Resizing the virtual grid"
}
```

Any display string or media path in a wiki annotation can contain `<key:...>`. `slug`
remains a stable non-localized URL identifier. When a key is missing in a non-English
locale, generation logs a warning and uses English. When the key is absent from English,
generation fails. The English category determines the shared directory path, while the
displayed category can be translated. Slash-separated category segments are slugified
individually, so `Widgets/Data` produces `widgets/data/virtual-list-view.md`. Asset links
include one `../` for each category segment, plus one for the locale directory.

Generate Markdown for the current Minecraft profile:

```powershell
./gradlew :1.20.1:common:generateWiki
```

By default, output is written under `build/stonecutter/1.20.1/common/generated/wiki`.
The directory contains shared `assets` and one tree per locale:

```text
wiki/
├── assets/
├── en/
│   ├── index.md
│   └── widgets/
│       └── data/virtual-list-view.md
└── ru/
    ├── index.md
    └── widgets/
        └── data/virtual-list-view.md
```

To write
directly into a site's content directory, set `wikiOutputDir`:

```powershell
./gradlew :1.20.1:common:generateWiki -PwikiOutputDir=../site/content/unigui
```

Input directories can also be replaced with `-PwikiLangDir=...` and
`-PwikiAssetsDir=...`.

The output directory is not cleaned by the generator. Point it at a build-owned or
site-generated directory so removed annotations do not leave stale published pages.

## Documentation Site (MkDocs)

The project includes an automatic documentation website based on **Material for MkDocs**
with bilingual support (English and Russian).

### Local Preview

1. Install Python dependencies:
   ```powershell
   pip install -r requirements-docs.txt
   ```
2. Generate Markdown files and start the local development server:
   ```powershell
   ./gradlew :1.20.1:common:generateWiki
   python -m mkdocs serve
   ```
3. Open `http://127.0.0.1:8000/` in your browser. Live reloading is active as docs are generated.

### Building Static Site

```powershell
python -m mkdocs build
```
Build output is generated into the `site/` directory.

### GitHub Pages Deployment

Deployment is automated via `.github/workflows/deploy-docs.yml`. On every push to
`Main_other` or `main`, GitHub Actions automatically:
1. Runs `./gradlew :1.20.1:common:generateWiki`
2. Builds the website with MkDocs
3. Publishes it directly to GitHub Pages.

## AI documentation

`@AiDoc` is intentionally independent from wiki annotations. It never appears in website
wiki pages, and a type does not need `@WikiPage` to participate in the AI collection.
`@AiDoc` can be applied to types, public methods, and public constructors. Type-level
metadata supports a purpose, use cases, critical rules/gotchas, related APIs, and examples.
Method-level `section`, `sectionOrder`, and `order` group and sort curated API entries.
The generator writes Java signatures and return types from reflection, and uses
`@XmlAttribute` descriptions/defaults where available. Only explicitly `@AiDoc`-annotated
public methods are included, so prose stays reviewed rather than inferred from names.

```java
@AiDoc(
        purpose = "Displays a large model as a virtual list or grid.",
        criticalRules = {
                "itemFactory receives the absolute model index, not a grid row.",
                "Do not add children manually; items are managed by the virtualizer."
        },
        related = {"GridBox", "ScrollView"},
        examples = @AiDoc.Example(
                title = "Build a grid",
                code = "view.grid(true).itemCount(entries.size());"))
public class VirtualListView { ... }

@AiDoc(
        section = "Core Configuration & Builder Methods",
        sectionOrder = 10,
        order = 20,
        summary = "Enables the responsive grid when true.")
public VirtualListView grid(boolean enabled) { ... }
```

```powershell
./gradlew :1.20.1:common:generateAiDocs
```

The default output is `build/stonecutter/1.20.1/common/generated/ai-docs`. Override it with:

```powershell
./gradlew :1.20.1:common:generateAiDocs -PaiDocsOutputDir=../ai-context/unigui
```

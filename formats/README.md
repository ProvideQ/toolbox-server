# ProvideQ Formats Library

A standalone library for handling problem- and solution-format conversions, built around a
slim core with pluggable format modules on top.

## What it does

- **Conversion**: converts values between formats using a graph of registered converters.
  All reachable conversion paths are **pre-computed at registration time**, so runtime
  route lookups are O(1).
- **Attribute access**: reads a single named, typed parameter (e.g. "dimension",
  "variable_count") out of a format via `AttributeReader`s declared directly on the format.
  If a format does not define a reader, the `AttributeService` falls back to
  converting the value to the nearest format that does.

## Modules

| Module         | Content                                             |
|----------------|-----------------------------------------------------|
| `formats-core` | Format/Converter/Attribute abstractions, registry, services, errors |
| `formats-cnf`  | DIMACS CNF + solution formats, logical expressions  |
| `formats-gml`  | GML graph format                                    |
| `formats-uvl`  | UVL feature models (FeatureIDE-based, heavy dependency) |
| `formats-all`  | Aggregate that depends on all of the above          |

## Usage

```java
// 1. Create a registry and register formats and converters
var registry = new ConversionRegistry()
CnfFormats.register(registry)      // registers dimacs-cnf, logical-expression + converter
GmlFormats.register(registry)      // registers gml
registry.seal()                    // optional: forbid further registration

// 2. Create the toolbox facade
var toolbox = new FormatsToolbox(registry)

// 3. Convert between formats (direct or multi-hop, resolved via the pre-computed tree)
DimacsCnf cnf = toolbox.convert("a and (b or not c)", "logical-expression", "dimacs-cnf")

// 4. Read attributes (with automatic conversion fallback)
Integer variableCount =
    toolbox.readAttribute(cnf, DimacsCnfFormat.INSTANCE, "variable_count", Integer.class)
```

## Adding a new format

1. Create a record implementing `Format<T>`:

```java
public record QuboFormat() implements Format<Qubo> {
  public static final QuboFormat INSTANCE = new QuboFormat()

  @Override public String getName() { return "qubo" }
  @Override public Set<String> getAliases() { return Set.of() }
  @Override public Set<String> getFileExtensions() { return Set.of("qubo") }
  @Override public FormatKind getKind() { return FormatKind.PROBLEM }

  @Override public Qubo parse(Reader reader) throws ParseException { /* ... */ }
  @Override public void serialize(Qubo value, Writer writer) throws SerializationException { /* ... */ }
}
```

2. Add converters. For simple ones, use `FuncConverter` with a lambda:

```java
var quboToCnf = new FuncConverter<>(QuboFormat.INSTANCE, DimacsCnfFormat.INSTANCE, false,
    qubo -> convertToCnf(qubo)) // may throw FormatException
registry.register(quboToCnf)
```

3. Declare attribute readers directly on the format:

```java
@Override
public Map<String, AttributeReader<Qubo, ?>> getAttributeReaders() {
  return Map.of(
      "dimension", AttributeReader.of("dimension", Integer.class, Qubo::dimension))
}
```

For expensive formats, use `AttributeReader.lazy(...)` to read the attribute directly from
raw input with a cheap single-pass scan instead of parsing the full object.

4. Register everything via a `<Family>Formats.register(...)` bundle method, as done in
   `CnfFormats`. Attribute readers need no separate registration.

## Design notes

- **No fixed intermediate representation.** A "standard format" per problem type is the
  convention (the hub of the conversion graph), but the core treats every format equally.
  Shortest paths over the converter graph are pre-computed whenever a converter is
  registered.
- **Pure interfaces.** `Format` and `Converter` are interfaces: implementors can use
  records, plain classes or lambdas (`FuncConverter`).
- Attribute readers are declared inline via the `AttributeReader.of(...)` factory.
- **Performance-first core**: synchronous, allocation-conscious. `Reader`/`Writer`-based
  format I/O leaves room for a streaming add-on later without a core redesign.
- Problem and solution formats share one registry, distinguished by `FormatKind`.

## Building

```shell
./gradlew :formats:formats-core:check :formats:formats-cnf:check :formats:formats-gml:check
```

## Exporting the conversion graph

`ConversionGraphExporter` renders the currently registered formats and converters as a
diagram (formats as nodes, conversions as directed edges), which can be embedded in
Markdown via Mermaid:

```java
var markdown = new ConversionGraphExporter().toMarkdown(registry)
Files.writeString(Path.of("format-graph.md"), markdown)
```

In the toolbox-server, `ConversionGraphExportTest` regenerates `format-graph.md` from the shared registry,
including the toolbox's available solvers as dashed edges from each problem type's input format to its
output format:

```shell
./gradlew :test --tests "edu.kit.provideq.toolbox.formats.ConversionGraphExportTest"
```

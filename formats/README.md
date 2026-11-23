# ProvideQ Formats Library

A standalone library for handling problem- and solution-format conversions, built around a
slim core with pluggable format modules on top.

## What it does

- **Conversion**: converts values between formats using a graph of registered converters. All reachable conversion paths are **pre-computed at registration time**, so runtime route lookups are O(1).
- **Format families**: families of formats that describe the same problem type can be registered together, e.g. DIMACS CNF and logical expressions for SAT formulas. Converters are expected to be registered so you can convert between any two formats in the same family, possibly via intermediate formats.
- **Attribute access**: reads a single named, typed parameter (e.g. "dimension", "variable_count") out of a format via `AttributeReader`s declared directly on the format. If a format does not define a reader, the `AttributeService` falls back to converting the value to the nearest format that does.

## Modules

| Module         | Content                                             |
|----------------|-----------------------------------------------------|
| `formats` | Format/Converter/Attribute abstractions, registry, services, errors |
| `formats-sat`  | DIMACS CNF + solution formats, logical expressions  |
| `formats-gml`  | GML graph format                                    |
| `formats-knapsack`  | Knapsack problem format                             |
| `formats-lp`  | LP problem format (QUBO)                            |
| `formats-qasm`  | OpenQASM - Open Quantum Assembly Language                             |
| `formats-tsplib`  | [TSPLib](https://comopt.ifi.uni-heidelberg.de/software/TSPLIB95/) format (Traveling Salesperson) and CVRP format (Capacitated Vehicle Routing Problem) |
| `formats-uvl`  | UVL feature models (FeatureIDE-based, heavy dependency) |
| `formats-xyz`  | XYZ molecule format |
| `formats-all`  | Aggregate that depends on all of the above          |

## Usage

```java
// 1. Create a registry and register formats and converters
var registry = new ConversionRegistry();
var conversionService = new ConversionService(registry);
CnfFormats.register(registry);      // registers dimacs-cnf, logical-expression + converter
registry.seal();                    // optional: forbid further registration

// 2. Convert between formats (direct or multi-hop, resolved via the pre-computed tree)
DimacsCnf cnf = conversionService.convert("a and (b or not c)", "logical-expression", "dimacs-cnf");

// 3. Read attributes (with automatic conversion fallback)
var attributeService = new AttributeService(registry, conversionService);
Integer variableCount =
        attributeService.read(cnf, DimacsCnfFormat.INSTANCE, "variable_count", Integer.class);
```

## Adding a new format

1. Create a record implementing `Format<T>`:

```java
public record QuboFormat() implements Format<Qubo> {
  public static final QuboFormat INSTANCE = new QuboFormat();

  @Override public String getName() { return "qubo"; }
  @Override public Set<String> getAliases() { return Set.of(); }
  @Override public Set<String> getFileExtensions() { return Set.of("qubo"); }
  @Override public FormatType getType() { return FormatType.PROBLEM; }
  @Override
  public Class<Qubo> getRepresentationClass() {
    return Qubo.class;
    }

  @Override public Qubo parse(Reader reader) throws ParseException { /* ... */ }
  @Override public void serialize(Qubo value, Writer writer) throws SerializationException { /* ... */ }
}
```

2. Add converters. For simple ones, use `FuncConverter` with a lambda:

```java
var quboToCnf = new FuncConverter<>(QuboFormat.INSTANCE, DimacsCnfFormat.INSTANCE,
        qubo -> convertToCnf(qubo)); // may throw FormatException
registry.registerConverter(quboToCnf);
```

3. Declare attribute readers directly on the format:

```java
@Override
public Map<String, AttributeReader<Qubo, ?>> getAttributeReaders() {
  return Map.of(
      "dimension", AttributeReader.of(Integer.class, Qubo::dimension));
}
```

For expensive formats, use `AttributeReader.lazy(...)` to read the attribute directly from
raw input with a cheap single-pass scan instead of parsing the full object.

4. Register everything via a `<Family>Formats.register(...)` bundle method, as done in
   `SatFormats`. Attribute readers need no separate registration.

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
- Problem and solution formats share one registry, distinguished by `FormatType`.

## Exporting the conversion graph

`ConversionGraphExporter` renders the currently registered formats and converters as a
diagram (formats as nodes, conversions as directed edges), which can be embedded in
Markdown via Mermaid:

```java
var markdown = new ConversionGraphExporter().toMarkdown(registry, List.of());
Files.writeString(Path.of("format-graph.md"), markdown);
```

In the toolbox-server, `ConversionGraphExportTest` regenerates `format-graph.md` from the shared registry,
including the toolbox's available solvers as dashed edges from each problem type's input format to its
output format:

```shell
./gradlew :toolbox:test --tests "edu.kit.provideq.toolbox.formats.ConversionGraphExportTest"
```

# OMX Matrix Specification

Version 0.2.1

This document is the normative OMX File Specification for the 0.2.x family. Where this document uses[RFC 2119](https://datatracker.ietf.org/doc/html/rfc2119) keywords (MUST/SHOULD/MAY) those words are normative. Background, rationale, and extended ideas are non-normative and are maintained in the project wiki.

## 1. Definitions

### 1.1 OMX Specification

The combination of the OMX File Specification and OMX Language Binding Specifications.

### 1.2 OMX File

A single `.omx` file implementing the [OMX File Specification](#2-omx-file-specification).

### 1.3 OMX Language Binding

Language-specific code that implements the [OMX Language Binding Specification](#3-omx-language-binding-specification).

### 1.3.1 Official OMX Language Bindings

OMX Language Bindings that are maintained by the OMX core developers and implement the latest version of the [OMX Language Binding Specification](#3-omx-language-binding-specification). Official bindings SHOULD provide a compatibility matrix listing supported `OMX_VERSION` values.

### 1.3.2 Community OMX Language Bindings

OMX Language Bindings that are maintained by the broader community and may or may not implement the latest version of the [OMX Language Binding Specification](#3-omx-language-binding-specification).

### 1.4 OMX-Compliant Software

Software that fully implements the features in the [OMX Language Binding Specification](#3-omx-language-binding-specification) and passes an OMX validation for the declared `OMX_VERSION`. Use the term "OMX-compliant"  where full conformance is intended; retain "OMX-compatible" for software that supports a subset of features.

### 1.5 OMX Plugin

Software that helps other software provide support for the features defined in [OMX Language Binding Specification](#3-omx-language-binding-specification).

## 2. OMX File Specification

### 2.1 File type and extension

- An OMX file **MUST** be a valid HDF5 file.
- The canonical extension **MUST** be `.omx`.
- The file **MUST** be readable by standard HDF5 tooling; proprietary compression filters that prevent this are not allowed.

### 2.2 Top-level HDF5 structure

At the HDF5 root `/`:

- A group `/data` **MUST** exist and contain all matrix datasets.
- A group `/lookup` **MAY** exist and, if present, **MUST** contain all lookup/index datasets.
- Additional groups such as `/meta` or `/provenance` **MAY** exist but **MUST NOT** be required for basic interpretation of matrix contents.

### 2.3 Required root attributes

The root group `/` **MUST** define:

- `OMX_VERSION` (string)  
  - Version string of the OMX core spec implemented by the file (e.g. `"0.2"`, `"0.3"`).
- `SHAPE` (2-element integer array)  
  - `[rows, cols]`. Every dataset in `/data` **MUST** conform to this shape.

The root **SHOULD** additionally define:

- `CREATED` (ISO-8601 string) – creation timestamp.
- `CREATOR` (string) – software that created the file ("omx-python 0.3.5", "EMME OMX adapter 1.0", etc.).
- `DESCRIPTION` (string) – human-readable description of contents.
- `INDEX_BASE` (integer, default `0`) – logical index base used in lookups and labels. `INDEX_BASE` **MUST** be either `0` or `1`.
- `DATA_TYPE` (string, optional) – optional file-level suggested default cell data type (for example "float64" or "int32"). Dataset-level storage datatypes declared on individual `/data/*` datasets are authoritative and take precedence over `DATA_TYPE` if both are present.

Recommended (reserved) attribute keys

The following root attributes are recommended for interoperability and **SHOULD** be used where applicable (types shown):

- `year` (int)
- `scenario` (string)
- `units` (string)
- `creator` (string)
- `created` (ISO-8601 string)
- `description` (string)

Use of these keys is RECOMMENDED but not REQUIRED; readers MUST ignore unknown root attributes.

### 2.4 Matrix datasets: `/data/*`

Each dataset under `/data` represents one matrix “core”:

- **Name**  
  - Path: `/data/{matrix_name}` where `{matrix_name}` is a unique string within the file.
- **Shape**  
  - **MUST** be exactly `SHAPE` as defined at the root.
- **Datatype**  
  - **MUST** be one of the standard HDF5 numeric or string types (e.g. `int32`, `int64`, `float32`, `float64`, fixed-length strings).
  - Different matrices in the same file **MAY** have different datatypes.
- **Storage**  
  - Datasets **SHOULD** be chunked in a way that supports efficient row/column access.
  - Datasets **SHOULD** use zlib/gzip compression when compressed. Use of other filters **MAY** be allowed but such files are considered less portable. If non-standard compression or HDF5 filters are used the root **SHOULD** include a `COMPRESSION_FILTERS` attribute listing required filters so readers can report appropriate warnings.

#### 2.4.1 Standard per-matrix attributes

Each dataset in `/data` **MAY** define:

- `title` (string) – human-readable title for the matrix.
- `NA` (same type as dataset) – sentinel value for missing data.
- Arbitrary additional attributes (key/value pairs) for metadata such as:
  - `year` (int), `scenario` (string), `units` (string), etc.

Matrix attributes **MUST NOT** be required for basic interpretation of the data (i.e., a reader that ignores non-standard attributes can still interpret the matrix).

### 2.5 Lookup datasets: `/lookup/*`

Lookups are 1-D datasets that map an axis index (row/column) to some label, such as TAZ IDs or districts.

For each dataset under `/lookup`:

- **Name**  
  - Path: `/lookup/{lookup_name}`, where `{lookup_name}` is unique within `/lookup`.
- **Shape**  
  - **MUST** be 1-D and length **MUST** match either `SHAPE[0]` or `SHAPE[1]`.
- **Datatype**  
  - **MUST** support equality comparison (e.g. integer or string).
- **Attributes**  
 - **Attributes**  
  - `dim` (int, REQUIRED for 2-D files)
    - `0` means the lookup applies to the row dimension,
    - `1` means the lookup applies to the column dimension.
    - For portability and clarity `dim` **MUST** be present for 2-D OMX files. When `dim` is absent it **MAY** be interpreted as applying to both dimensions for square matrices, but this usage is deprecated and SHOULD be avoided.

As with matrices, lookups **MAY** have arbitrary extra attributes.

## 3. OMX Language Binding Specification

Each **OMX language binding** (e.g. `omx-python`, `omx-r`, `omx-java`, `omx-cpp`, `omx-csharp`) **MUST** support this common minimal feature set.

### 3.1 Core operations

A language binding **MUST** implement, in idiomatic form:

- Open / close:
  - `open(file_path, mode)` – at least `"r"`, `"r+"`, `"w"` (or equivalents).
- Query:
  - List matrix names in `/data`.
  - List lookup names in `/lookup`.
  - Read root attributes (especially `OMX_VERSION`, `SHAPE`).
- Matrix access:
  - Read entire matrix by name.
  - Read row(s) and column(s) by index.
  - Write a new matrix.
  - Overwrite an existing matrix.
  - Delete a matrix.
  - Get / set per-matrix attributes.
- Lookup access:
  - Read lookup vectors.
  - Write or overwrite lookup vectors.
  - Get / set per-lookup attributes.
- Metadata:
  - Get / set arbitrary root attributes.

Each binding **SHOULD** expose a way to:

- Map from labels (e.g. TAZ IDs from a lookup) to matrix indices, where this is practical;
- Integrate with the language’s standard array / table structures (NumPy, R matrices, etc.).

### 3.2 Validation (Normative)

Language bindings **MUST** provide a validation helper (for example `validate_omx(path)`) that performs at least the following checks for the declared `OMX_VERSION`:

- The file is a readable HDF5 file.
- The root group defines `OMX_VERSION` and `SHAPE` and `SHAPE` is a 2-element integer array.
- `INDEX_BASE`, if present, is either `0` or `1`.
- Every dataset under `/data` exists and has shape equal to `SHAPE`.
- Each dataset under `/data` has a valid HDF5 datatype.
- Each dataset under `/lookup`, if present, is 1-D and its length matches either `SHAPE[0]` or `SHAPE[1]`, and includes a valid `dim` attribute.

The validator **MUST** return a structured result (boolean + list of errors/warnings) suitable for programmatic consumption. Bindings **SHOULD** provide a small CLI wrapper (for example `omx-validate path.omx`).

### 3.3 Version handling

Bindings **MUST**:

- Read `OMX_VERSION` and expose it to callers.
- Document which `OMX_VERSION` values they fully support.
- Handle unknown `OMX_VERSION` gracefully (no silent corruption – fail fast, or require an explicit override).


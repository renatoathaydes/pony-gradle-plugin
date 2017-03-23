## Pony Basic Sample

This basic sample has a single dependency ("jemc/pony-zmq", see [bundle.json](bundle.json)),
a simple package called `basic` and a test package called `test`.

### Building

```
../../gradlew compile
```

### Running

```
build/basic
```

### Testing

```
../../gradlew test
```

### Clean up

Delete all built resources, but not downloaded dependencies in `ext-libs`.

```
../../gradlew clean
```
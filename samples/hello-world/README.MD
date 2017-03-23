## Hello World Sample

This sample contains only three files:

* build.gradle
* hello/Main.pony
* test/pony-test.pony

### Building

```
../../gradlew compile
```

### Running

```
build/hello
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

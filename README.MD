# Pony Gradle Plugin

🐴 A Gradle Plugin to build [Pony](http://www.ponylang.org/) projects.

## Features

* compile Pony sources.
* resolve dependencies using `bundle.json` - compatible with [pony-stable](https://github.com/jemc/pony-stable).
* resolve transitive dependencies (if the dependencies have a `bundle.json` file).

## Getting started

Run the following commands to create a Pony project with Gradle support:

* create and enter your new project directory.

```
mkdir my-project && cd my-project
```

* create the Gradle build file.

```
echo '
plugins {
  id "com.athaydes.pony" version "0.2.0"
}
' > build.gradle
```

* create a Pony package with a `Main` actor.

```
mkdir main
echo '
actor Main
    new create(env: Env) =>
        env.out.print("Hello world!")
' > main/main.pony
```

* compile with Gradle.

```
gradle compile
```

* run the generated binary.

```
build/main
```

The following sections explain how this all works in detail.

## Build files

To enable Gradle support, a basic `build.gradle` file must be supplied to apply this plugin.
Dependencies are declared separately (to be compatible with `pony-stable`) in the `bundle.json` file.

### `build.gradle` - the Gradle build file.

Simplest possible example without any configuration (see below for defaults):

```groovy
plugins {
  id "com.athaydes.pony" version "0.2.0"
}
```

The above is all you need, but you may configure the build using the `pony` block:

```groovy
plugins {
  id "com.athaydes.pony" version "0.2.0"
}

pony {
  packageName = 'hello'
  docs = true
}
```

### Configuration options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
|packageName|String|`main`|name of the package to compile.|
|testPackage|String|`test`|name of the test package to compile and run.|
|docs|Boolean|false|whether to generate docs.|
|debug|Boolean|false|Don't optimise the output.|
|library|Boolean|false|Generate a C-API compatible static library.|
|strip|Boolean|false|Strip debug info.| 
|runtimebc|Boolean|false|Compile with the LLVM bitcode file for the runtime.|
|pic|Boolean|false|Compile using position independent code.|
|compileOptions|List|`[]`|Extra compiler options to use when compiling.|
|testOptions|List|`[]`|Extra compiler options to use when compiling and running tests.|

To get started, see the [Hello World](samples/hello-world) sample.

See the [pony-configured](samples/pony-configured) sample for a build file that uses all options. 

### `bundle.json` - pony-stable dependencies file.

Example:

```json
{
  "name": "my-project",
  "version": "2.3.4",
  "deps": [
    { "type": "github", "repo": "jemc/pony-inspect", "version": "1.0.1" },
    { "type": "github", "repo": "other/dep" },
    { "type": "local", "local-path": "../my-other-dep" }
  ]
}
```

Everything in the json file is optional. Unrecognized fields are ignored.

## Dependency types

The currently supported dependency types are the following:

### GitHub

> GitHub dependencies are downloaded using the GitHub API, so other Git repositories are not
  currently supported.

GitHub dependencies have the following fields:

* `type` (mandatory): must be `github`.
* `repo` (mandatory): `<github-user>/<repository-name>`.
* `version` (optional): tag/branch/commit

If `version` is not given, the latest tag will be used, or if there's no tags, the main branch
(usually `master`).

See the [pony-basic](samples/pony-basic) sample for an example.

### Local

Local dependencies are simple Pony projects that are located in the local file system.

They are declared using the following fields:

* `type` (mandatory): must be `local`.
* `local-path` (mandatory): the path, relative to the root of this project, to the dependency project.

See the [pony-multi-modules](samples/pony-multi-modules) sample for an example.

## Tasks

The following tasks are added by this plugin:

* `cleanPony`: deletes all artifacts created by previous builds.
* `resolvePonyDependencies`: resolves the project dependencies.
* `unpackPonyDependencies`: unpacks the dependencies archives.
* `compilePony`: compiles Pony sources and dependencies.
* `testPony`: compiles Pony sources and tests, then runs the test package.

Normally, all you need to do to build your project is run `compilePony`, or just `compile`:

```
gradle compile
```

The other tasks will be run automatically if necessary.

You can run only the task you want as well, for example:

```
gradle resolve
```

The above will resolve all dependencies. If any dependencies need to be downloaded, they will be,
so that transitive dependencies can be also resolved.

To compile both sources and test sources, then run tests, simply run the `test` task:

```
gradle test
```

## Build layout

Everything the Pony plugin creates goes into Gradle's `project.buildDir`, which is `build` by default.

Sources should go into the `packageName` directory, as in any Pony project.

For a project called `pony-basic`, the layout would look something like this **after** built:

```
├── ext-libs          # external libraries sources
│   ├── unpacked      # unpacked libraries
│   │   ├── jemc-pony-inspect-03a65d4
│   │   │   ...
│   │   ├── jemc-pony-sodium-d293cdb
│   │   │   ...
│   │   └── jemc-pony-zmq-6e9157e
│   │       ...
│   └── zips          # packaged libraries (not necessary to compile)
│       ├── jemc-pony-inspect.zip
│       ├── jemc-pony-sodium.zip
│       └── jemc-pony-zmq.zip
├── build                 # generated by the Pony plugin
│   ├── pony-basic        # generated by ponyc
│   └── pony-basic.dSYM
│       └── Contents
│           ├── Info.plist
│           └── Resources
│               └── DWARF
│                   └── pony-basic
├── build.gradle          # Gradle build file
├── bundle.json           # dependencies file
└── pony-basic            # source directory
    └── Main.pony

20 directories, 79 files
```


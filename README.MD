# Pony Gradle Plugin

A Gradle Plugin to build [Pony](http://www.ponylang.org/) projects.

## Features

* compile Pony sources.
* resolve dependencies using `bundle.json` - compatible with [pony-stable](https://github.com/jemc/pony-stable).
* resolve transitive dependencies from GitHub projects.

## Build files

To enable Gradle support, a basic `build.gradle` file must be supplied.
Dependencies are declared separately (to be compatible with `pony-stable`) in the `bundle.json` file.

### `build.gradle` - the Gradle build file.

Simplest possible example without any configuration (see below for defaults):

```groovy
plugins {
  id "com.athaydes.pony" version "1.0"
}
```

The above is all you need, but you may configure the build with the following options:

* `packageName`: String - name of the package to compile. Defaults to `src`.
* `docs`: boolean - whether to generate docs. Defaults to `false`.
* `debug`: boolean - whether to use the `--debug` option (will skip the LLVM optimizations passes).
           Defaults to `false`.
           
See the [pony-configured](samples/pony-configured) sample for a build file that uses all options. 

### `bundle.json` - pony-stable dependencies file.

Example:

```json
{
  "name": "my-project",
  "version": "2.3.4",
  "deps": [
    { "type": "github", "repo": "jemc/pony-inspect", "version": "1.0.1" },
    { "type": "github", "repo": "other/dep" }
  ]
}
```

Everything in the json file is optional. Unrecognized fields are ignored.

## Dependency types

The only dependency type currently supported is `github` (more will be added later).

### GitHub

> GitHub dependencies are downloaded using the GitHub API, so other Git repositories are not
  currently supported.

GitHub dependencies have the following fields:

* `type` (mandatory): must be `github`.
* `repo` (mandatory): `<github-user>/<repository-name>`.
* `version` (optional): tag/branch/commit

If `version` is not given, the latest tag will be used, or if there's no tags, the main branch
(usually `master`).

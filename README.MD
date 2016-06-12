# Pony Gradle Plugin

A Gradle Plugin to build [Pony](http://www.ponylang.org/) projects.

## Features

* resolve dependencies using `bundle.json` - compatible with [pony-stable](https://github.com/jemc/pony-stable).
* resolve transitive dependencies from GitHub projects.

## Build files

The following build files are required at the root folder of the project:

* `build.gradle` - the Gradle build file.

```groovy
plugins {
  id "com.athaydes.pony" version "1.0"
}
```

> Currently, no configuration can be added to the Pony Gradle plugin, so the above is all you need.

* `bundle.json` - pony-stable dependencies file.

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

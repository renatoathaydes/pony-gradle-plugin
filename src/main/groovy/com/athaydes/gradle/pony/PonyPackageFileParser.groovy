package com.athaydes.gradle.pony

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.Project

@CompileStatic
class PonyPackageFileParser {

    final Project project

    PonyPackageFileParser( Project project ) {
        this.project = project
    }

    PonyPackage parse( String packageFile ) {
        def json = new JsonSlurper().parseText( packageFile )

        def deps = json[ "deps" ] ?: [ ]

        def dependencies = deps.collect { parseAnyDep( it ) }

        println( json )
        return new PonyPackage( project.name, project.version.toString(), dependencies )
    }

    PonyDependency parseAnyDep( any ) {
        switch ( any ) {
            case ( Map ): return parseMapDep( any as Map )
            default:
                throw new GradleException( "Invalid type for dependency (expected Map): $any" )
        }
    }

    PonyDependency parseMapDep( Map map ) {
        def type = map[ "type" ]

        // TODO add other types of dependencies
        switch ( type ) {
            case "github": return parseGitHubDep( map )
            case null: throw new GradleException( "Must provide dependency type for '$map'" )
            default:
                throw new GradleException( "Unrecognized dependency type: $type" )
        }
    }

    PonyDependency parseGitHubDep( Map map ) {
        def properties = new HashMap<String, String>( map.size() )

        map.each { key, value ->
            properties[ key.toString() ] = value.toString()
        }

        def repo = map[ "repo" ] as String

        return new GitHubPonyDependency( repo, project )
    }

}

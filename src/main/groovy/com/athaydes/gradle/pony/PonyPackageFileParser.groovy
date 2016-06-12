package com.athaydes.gradle.pony

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

@CompileStatic
class PonyPackageFileParser {

    static final Logger logger = Logging.getLogger( PonyPackageFileParser )

    final Project project

    PonyPackageFileParser( Project project ) {
        this.project = project
    }

    PonyPackage parse( String packageFile ) {
        def json = new JsonSlurper().parseText( packageFile )

        String name = json[ "name" ] ?: '<Unnamed package>'
        String version = json[ "version" ] ?: '0'
        def deps = ( json[ "deps" ] ?: [ ] ) as List

        logger.info( "Parsing ${deps.size()} dependencies of package $name - $version" )

        List<PonyDependency> dependencies = [ ]

        try {
            dependencies = deps.collect { parseAnyDep( it ) }
        } catch ( ClassCastException e ) {
            throw new GradleException( "Dependencies of package $name are invalid - " +
                    "${e.message ?: ''}", e )
        }

        return new PonyPackage( name, version, dependencies )
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
            case "local": return parseLocalDep( map )
            case null: throw new GradleException( "Must provide dependency type for '$map'" )
            default:
                throw new GradleException( "Unrecognized dependency type: $type" )
        }
    }

    PonyDependency parseGitHubDep( Map map ) {
        def repo = map[ "repo" ] as String
        def version = map[ "version" ] as String

        return new GitHubPonyDependency( project, repo, version )
    }

    PonyDependency parseLocalDep( Map map ) {
        def path = map[ "local-path" ] as String

        return new LocalDependency( project, path )
    }

}

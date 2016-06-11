package com.athaydes.gradle.pony

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.gradle.api.Project

import java.nio.file.Path

@CompileStatic
interface PonyDependency {
    Path resolvedPath()
}

@Immutable( knownImmutableClasses = [ Project ] )
class GitHubPonyDependency implements PonyDependency {

    String repo
    Project project

    static final gitHubHost = "https://api.github.com"

    @Override
    Path resolvedPath() {
        def tags = new JsonSlurper().parseText(
                URI.create( "$gitHubHost/repos/$repo/tags" )
                        .toURL().text ) as List

        println( tags )

        URL zipBallUrl = resolveZipUrl( tags )

        def task = project.tasks.getByName( ResolveDependenciesTask.NAME ) as ResolveDependenciesTask

        def repoZip = new File( task.outputDir(), zipName( repo ) )

        // ensure the repository does not exist
        project.delete( repoZip )

        zipBallUrl.withInputStream { InputStream ins ->
            repoZip.withOutputStream { out ->
                out.write( ins.bytes )
            }
        }

        println( "Downloaded ${repoZip.length()} bytes to ${repoZip.name}" )

        return repoZip.toPath()
    }

    private URL resolveZipUrl( List tags ) {
        def actualTags = tags.findAll { it instanceof Map }
        if ( actualTags.empty ) {
            println "Repo does not have any tags, downloading master"
            return URI.create( "$gitHubHost/repos/$repo/zipball" )
                    .toURL()
        } else {
            println "Downloading tag ${actualTags.first()}"
            // FIXME find the right version, not just the latest!!
            return URI.create( actualTags.first()[ "zipball_url" ].toString() ).toURL()
        }
    }

    static String zipName( String repo ) {
        repo.replace( '/', '-' ).replace( '\\', '-' ) + '.zip'
    }

}

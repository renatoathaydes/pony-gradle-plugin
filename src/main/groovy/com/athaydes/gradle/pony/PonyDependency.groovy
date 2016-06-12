package com.athaydes.gradle.pony

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.nio.file.Path

@CompileStatic
interface PonyDependency {
    Path resolvedPath()
}

@Immutable( knownImmutableClasses = [ Project ] )
class GitHubPonyDependency implements PonyDependency {

    static final Logger logger = Logging.getLogger( GitHubPonyDependency )

    String repo
    Project project

    static final gitHubHost = "https://api.github.com"

    @Override
    Path resolvedPath() {
        def tags = new JsonSlurper().parseText(
                URI.create( "$gitHubHost/repos/$repo/tags" )
                        .toURL().text ) as List

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

        logger.info( "Downloaded ${repoZip.length()} bytes to ${repoZip.name}" )

        return repoZip.toPath()
    }

    private URL resolveZipUrl( List tags ) {
        def actualTags = tags.findAll { it instanceof Map }

        logger.info( "GitHub repo $repo has ${tags.size()} tags." )

        if ( actualTags.empty ) {
            logger.info "Downloading master of $repo because no tags were found."
            return URI.create( "$gitHubHost/repos/$repo/zipball" )
                    .toURL()
        } else {
            logger.info "Downloading repository latest tag ${actualTags.first()}"
            // FIXME find the right version, not just the latest!!
            return URI.create( actualTags.first()[ "zipball_url" ].toString() ).toURL()
        }
    }

    static String zipName( String repo ) {
        repo.replace( '/', '-' ).replace( '\\', '-' ) + '.zip'
    }

}

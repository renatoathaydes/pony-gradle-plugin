package com.athaydes.gradle.pony

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.gradle.api.Nullable
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

    Project project
    String repo
    @Nullable
    String version

    static final gitHubHost = "https://api.github.com"

    @Override
    Path resolvedPath() {
        def tags = new JsonSlurper().parseText(
                URI.create( "$gitHubHost/repos/$repo/tags" )
                        .toURL().text ) as List

        URL zipBallUrl = resolveZipUrl( tags )

        def task = project.tasks.getByName( ResolveDependenciesTask.NAME ) as ResolveDependenciesTask

        def repoZip = new File( task.outputDir( project ), zipName( repo ) )

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

        if ( actualTags.empty && !version ) {
            logger.info "Downloading main branch of $repo because no version was specified and no tags exist."
            return URI.create( "$gitHubHost/repos/$repo/zipball" ).toURL()
        }

        def actualVersion = version ? actualTags.find { Map tag -> tag.name == version } : null

        if ( actualVersion ) {
            logger.info "Downloading tag for $repo matching required version: $version"
        } else if ( !version && !actualTags.empty ) {
            logger.info "Downloading repository latest tag ${actualTags.first()} (no version was specified)"
        } else { // here, there must be a version specified
            logger.info "Trying to download branch $version of $repo (no tag matching the required version exists)"
            return URI.create( "$gitHubHost/repos/$repo/zipball/$version" ).toURL()
        }

        def tag = actualVersion ?: actualTags.first()

        return URI.create( tag[ "zipball_url" ].toString() ).toURL()
    }

    static String zipName( String repo ) {
        repo.replace( '/', '-' ).replace( '\\', '-' ) + '.zip'
    }

}

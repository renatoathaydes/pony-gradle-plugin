package com.athaydes.gradle.pony

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.gradle.api.DefaultTask
import org.gradle.api.Nullable
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskAction

import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Pony Gradle plugin.
 */
@CompileStatic
class PonyPlugin implements Plugin<Project> {

    @Override
    void apply( Project project ) {
        project.tasks.create( ResolveDependenciesTask.NAME, ResolveDependenciesTask )
        project.tasks.create( UnpackArchivesTask.NAME, UnpackArchivesTask )
        project.tasks.create( CleanTask.NAME, CleanTask )
    }

}

@Immutable
class PonyPackage {
    String name
    String version
    List<PonyDependency> dependencies = [ ]
}

@CompileStatic
class CleanTask extends Delete {

    static final String NAME = 'cleanPony'

    CleanTask() {
        delete( Paths.get( project.buildDir.absolutePath, "ext-libs" ).toFile() )
    }
}

@CompileStatic
class ResolveDependenciesTask extends DefaultTask {

    static final String NAME = "resolvePonyDependencies"

    final PonyPackageFileParser packageParser = new PonyPackageFileParser( project )

    ResolveDependenciesTask() {
        getInputs().file( project.file( "bundle.json" ) )
        getOutputs().dir( outputDir() )
    }

    @TaskAction
    def run() {
        def bundle = project.file( "bundle.json" )

        if ( !bundle.file ) {
            logger.info( "bundle.json file does not exist. No dependencies to resolve." )
            return
        }

        outputDir().mkdirs()

        PonyPackage ponyPackage = packageParser.parse( bundle.text )

        logger.debug( ponyPackage.toString() )

        resolveDependencies( ponyPackage.dependencies )
    }

    File outputDir() {
        Paths.get( project.buildDir.absolutePath, "ext-libs" ).toFile()
    }

    private void resolveDependencies( List<PonyDependency> dependencies ) {
        logger.debug( "Resolving ${dependencies.size()} dependencies" )
        dependencies.parallelStream().each { PonyDependency it ->
            Path depPath = it.resolvedPath()
            logger.debug( "Resolved dependency: {}", depPath )
            def depBundle = bundleFromZip( depPath )
            if ( depBundle ) {
                logger.info( "Resolving transitive dependencies of $depPath" )
                resolveDependencies( packageParser.parse( depBundle ).dependencies )
            } else {
                logger.info( "No transitive dependencies found for $depPath" )
            }
        }
    }

    @Nullable
    private String bundleFromZip( Path zipPath ) {
        def zip = new ZipFile( zipPath.toFile() )
        try {
            def bundle = zip.entries().find { ZipEntry entry ->
                entry.name.endsWith( '/bundle.json' )
            } as ZipEntry
            if ( bundle ) {
                return zip.getInputStream( bundle ).text
            }
        } catch ( e ) {
            logger.error( "Problem parsing bundle.json for $zipPath", e )
        } finally {
            zip.close()
        }

        return null
    }

}

class UnpackArchivesTask extends DefaultTask {

    static final String NAME = "unpackPonyDependencies"

    UnpackArchivesTask() {
        getInputs().file( outputDir() )
    }

    @TaskAction
    def run() {
        def outputDir = outputDir()
        if ( outputDir.directory ) {
            outputDir.listFiles( { File f, String name ->
                name.endsWith( '.zip' )
            } as FilenameFilter ).each { File file ->
                logger.debug "Unzipping $file"
                try {
                    ant.unzip( src: file,
                            dest: outputDir,
                            overwrite: true )
                } catch ( e ) {
                    logger.error( "Failed to unzip file {} due to {}", file, e )
                }
            }
        }
    }

    File outputDir() {
        Paths.get( project.buildDir.absolutePath, "ext-libs" ).toFile()
    }

}

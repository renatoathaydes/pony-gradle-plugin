package com.athaydes.gradle.pony

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

import java.nio.file.Paths

/**
 * Pony Gradle plugin.
 */
@CompileStatic
class PonyPlugin implements Plugin<Project> {

    @Override
    void apply( Project project ) {
        project.tasks.create( ResolveDependenciesTask.NAME, ResolveDependenciesTask )
        project.tasks.create( UnpackArchivesTask.NAME, UnpackArchivesTask )
    }

}

@Immutable
class PonyPackage {
    String name
    String version
    List<PonyDependency> dependencies = [ ]
}

@CompileStatic
class ResolveDependenciesTask extends DefaultTask {

    static final String NAME = "resolvePonyDependencies"

    ResolveDependenciesTask() {
        getInputs().file( project.buildFile )
        getOutputs().dir( outputDir() )
    }

    @TaskAction
    def run() {
        outputDir().mkdirs()

        PonyPackage ponyPackage = new PonyPackageFileParser( project )
                .parse( project.file( "bundle.json" ).text )

        println( ponyPackage )

        resolveDependencies( ponyPackage.dependencies )
    }

    File outputDir() {
        Paths.get( project.buildDir.absolutePath, "ext-libs" ).toFile()
    }

    private static resolveDependencies( List<PonyDependency> dependencies ) {
        println( "Resolved all dependencies: ${dependencies.size()}" )
        println( dependencies.collect { it.resolvedPath() } )
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
                println "Unzipping $file"
                try {
                    ant.unzip( src: file,
                            dest: outputDir,
                            overwrite: true )
                } catch ( e ) {
                    e.printStackTrace()
                }
            }
        }
    }

    File outputDir() {
        Paths.get( project.buildDir.absolutePath, "ext-libs" ).toFile()
    }

}

package com.athaydes.gradle.pony

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
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
        project.extensions.create( 'pony', PonyConfig )
        project.tasks.create( ResolveDependenciesTask.NAME, ResolveDependenciesTask )
        project.tasks.create( UnpackArchivesTask.NAME, UnpackArchivesTask )
        project.tasks.create( CleanTask.NAME, CleanTask )
        project.tasks.create( CompilePonyTask.NAME, CompilePonyTask )
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
        description = 'deletes all artifacts created by previous builds'
        group = 'build'

        delete( project.buildDir )
    }
}

@CompileStatic
class ResolveDependenciesTask extends DefaultTask {

    static final String NAME = "resolvePonyDependencies"

    final PonyPackageFileParser packageParser = new PonyPackageFileParser( project )

    ResolveDependenciesTask() {
        description = 'resolves the project dependencies'
        group = 'build'

        getInputs().file( project.file( "bundle.json" ) )
        getOutputs().dir( outputDir( project ) )
    }

    @TaskAction
    def run() {
        def bundle = project.file( "bundle.json" )

        if ( !bundle.file ) {
            logger.info( "bundle.json file does not exist. No dependencies to resolve." )
            return
        }

        outputDir( project ).mkdirs()

        PonyPackage ponyPackage = packageParser.parse( bundle.text )

        logger.debug( ponyPackage.toString() )

        resolveDependencies( ponyPackage.dependencies )
    }

    static File outputDir( Project project ) {
        Paths.get( project.buildDir.absolutePath, "ext-libs/zips" ).toFile()
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
        description = 'unpacks the dependencies archives'
        group = 'build'

        getInputs().dir( ResolveDependenciesTask.outputDir( project ) )
        getOutputs().dir( outputDir( project ) )
        dependsOn project.tasks.getByName( ResolveDependenciesTask.NAME )
    }

    @TaskAction
    def run() {
        def zipsDir = ResolveDependenciesTask.outputDir( project )
        if ( zipsDir.directory ) {
            def output = outputDir( project )
            output.mkdirs()

            zipsDir.listFiles( { File f, String name ->
                name.endsWith( '.zip' )
            } as FilenameFilter ).each { File file ->
                logger.debug "Unzipping $file"
                try {
                    ant.unzip( src: file,
                            dest: output,
                            overwrite: true )
                } catch ( e ) {
                    logger.error( "Failed to unzip file {} due to {}", file, e )
                }
            }
        }
    }

    static File outputDir( Project project ) {
        Paths.get( project.buildDir.absolutePath, "ext-libs/unpacked" ).toFile()
    }

}

@CompileStatic
class CompilePonyTask extends DefaultTask {

    static final String NAME = 'compilePony'

    CompilePonyTask() {
        description = 'compiles Pony sources and dependencies'

        getInputs().dir( UnpackArchivesTask.outputDir( project ) )
        getOutputs().dir( outputDir( project ) )
        dependsOn project.tasks.getByName( UnpackArchivesTask.NAME )
    }

    @TaskAction
    void run() {
        def config = project.extensions.getByName( 'pony' ) as PonyConfig

        def options = "${pathOption()}${outputOption()}${docsOption( config )}${debugOption( config )}"
        def command = "ponyc $options ${packageName( config )}"

        logger.info( "Running ponyc command: {}", command )

        def process = command.execute( null as List, project.projectDir )

        process.consumeProcessOutput( System.out as OutputStream, System.err )
        def status = process.waitFor()
        process.waitForProcessOutput()

        if ( status != 0 ) {
            throw new GradleException( "Ponyc failed, see output for details. Exit code: $status" )
        }
    }

    private static String packageName( PonyConfig config ) {
        config.packageName
    }

    private String pathOption() {
        def dirs = UnpackArchivesTask.outputDir( project ).listFiles( { File f -> f.directory } as FileFilter )
        if ( dirs ) {
            return ' --path ' + dirs.collect { File f -> f.absolutePath }.join( ':' )
        } else {
            return ''
        }
    }

    private String outputOption() {
        " --output ${outputDir( project ).absolutePath}"
    }

    private static String debugOption( PonyConfig config ) {
        config.debug ? ' --debug' : ''
    }

    private static String docsOption( PonyConfig config ) {
        config.docs ? ' --docs' : ''
    }

    static File outputDir( Project project ) {
        project.buildDir
    }

}

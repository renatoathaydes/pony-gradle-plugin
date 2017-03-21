package com.athaydes.gradle.pony

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Nullable
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
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
        project.tasks.create( PonyTestTask.NAME, PonyTestTask )
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
    }

    @Optional
    @InputFile
    File getBundleFile() {
        def bundle = project.file( "bundle.json" )
        if ( bundle.file ) {
            return bundle
        } else {
            return null
        }
    }

    @OutputDirectory
    File getOutputDir() {
        outputDir( project )
    }

    static File outputDir( Project project ) {
        Paths.get( project.buildDir.absolutePath, "ext-libs/zips" ).toFile()
    }

    @TaskAction
    def run() {
        if ( !bundleFile.file ) {
            logger.info( "bundle.json file does not exist. No dependencies to resolve." )
            return
        }

        outputDir.mkdirs()

        PonyPackage ponyPackage = packageParser.parse( bundleFile.text )

        logger.debug( ponyPackage.toString() )

        resolveDependencies( ponyPackage.dependencies )
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

@CompileStatic
class UnpackArchivesTask extends DefaultTask {

    static final String NAME = "unpackPonyDependencies"

    UnpackArchivesTask() {
        description = 'unpacks the dependencies archives'
        group = 'build'

        dependsOn project.tasks.getByName( ResolveDependenciesTask.NAME )
    }

    static File outputDir( Project project ) {
        Paths.get( project.buildDir.absolutePath, "ext-libs/unpacked" ).toFile()
    }

    @Optional
    @InputDirectory
    File getInputDir() {
        def input = ResolveDependenciesTask.outputDir( project )
        if ( input.directory ) {
            return input
        } else {
            return null
        }
    }

    @OutputDirectory
    File getOutputDir() {
        outputDir( project )
    }

    @CompileDynamic
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

}

@CompileStatic
class CompilePonyTask extends BaseCompileTask {

    static final String NAME = 'compilePony'

    CompilePonyTask() {
        description = 'compiles Pony sources and dependencies'
        group = 'build'
    }

    @Override
    String packageName( PonyConfig config ) {
        config.packageName
    }

}

@CompileStatic
class PonyTestTask extends BaseCompileTask {

    static final String NAME = 'testPony'

    PonyTestTask() {
        description = 'compiles Pony sources, dependencies and the test package, then runs the tests'
        group = 'verification'
    }

    @InputDirectory
    File getMainPackageDir() {
        def config = project.extensions.getByName( 'pony' ) as PonyConfig

        // besides the package of the test itself, the package of the application must be added
        project.file( config.packageName )
    }

    @Override
    String packageName( PonyConfig config ) {
        config.testPackage
    }

}

@CompileStatic
abstract class BaseCompileTask extends DefaultTask {

    BaseCompileTask() {
        dependsOn project.tasks.getByName( UnpackArchivesTask.NAME )
    }

    @Optional
    @InputDirectory
    File getDepsDir() {
        def dir = UnpackArchivesTask.outputDir( project )
        if ( dir.directory ) {
            return dir
        } else {
            return null
        }
    }

    @InputDirectory
    File getPackageDir() {
        def config = project.extensions.getByName( 'pony' ) as PonyConfig
        project.file( packageName( config ) )
    }

    @OutputDirectory
    File getOutputDir() {
        project.buildDir
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

    abstract String packageName( PonyConfig config )

    private String pathOption() {
        def dirs = otherPackages()
        if ( dirs ) {
            return ' --path ' + dirs.join( ':' )
        } else {
            return ''
        }
    }

    private List<String> otherPackages() {
        UnpackArchivesTask.outputDir( project )
                .listFiles( { File f -> f.directory } as FileFilter )
                .collect { File f -> f.absolutePath }
    }

    private String outputOption() {
        " --output ${outputDir.absolutePath}"
    }

    private static String debugOption( PonyConfig config ) {
        config.debug ? ' --debug' : ''
    }

    private static String docsOption( PonyConfig config ) {
        config.docs ? ' --docs' : ''
    }

}

package com.athaydes.gradle.pony

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class PonyPackageFileParserSpec extends Specification {

    @Shared
    def testProject = Stub( Project ) {
        getName() >> 'test-project'
        getVersion() >> '0'
    }

    @Shared
    def parsedSimplePackage = new PonyPackage( 'test-project', '0',
            [ new GitHubPonyDependency( testProject, 'jemc/pony-inspect', null ) ] )

    static final simplePackage = '''
{
  "name": "test-project",
  "deps": [
    { "type": "github", "repo": "jemc/pony-inspect" }
  ]
}'''

    @Shared
    def parsedVersionedPackage = new PonyPackage( 'versioned-project', '2.3.4',
            [ new GitHubPonyDependency( testProject, 'jemc/pony-inspect', '1.0.1' ),
              new GitHubPonyDependency( testProject, 'other/dep', null ) ] )

    static final versionedPackage = '''
{
  "name": "versioned-project",
  "version": "2.3.4",
  "deps": [
    { "type": "github", "repo": "jemc/pony-inspect", "version": "1.0.1" },
    { "type": "github", "repo": "other/dep" }
  ]
}'''


    @Subject
    final parser = new PonyPackageFileParser( testProject )


    def "Pony Plugin can understand Pony package files as defined by pony-stable"() {
        when: 'an example, valid Pony package file is parsed'
        def result = parser.parse( ponyPackageFileContents )

        then: 'A Map is returned containing all the correct information'
        result == expectedResult

        where:
        ponyPackageFileContents | expectedResult
        simplePackage           | parsedSimplePackage
        versionedPackage        | parsedVersionedPackage
    }

    def "Can resolve a simple package"() {
        given: 'A real project'
        Project project = ProjectBuilder.builder()
                .withName( 'test1' ).build()
        project.buildDir = new File( 'build' )

        when: 'the com.athaydes.pony plugin is applied to the project'
        project.pluginManager.apply( 'com.athaydes.pony' )

        then: 'no errors should occur'
        true

        and: 'the tasks should be added'
        project.tasks.getByName( ResolveDependenciesTask.NAME )
        project.tasks.getByName( UnpackArchivesTask.NAME )
        project.tasks.getByName( CleanTask.NAME )
        project.tasks.getByName( CompilePonyTask.NAME )

        and: 'the extensions should be added'
        project.extensions.getByName( 'pony' )
    }

}

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
            [ new GitHubPonyDependency( 'jemc/pony-inspect', testProject ) ] )

    static final simplePackage = '''
{
  "name": "test-project",
  "deps": [
    { "type": "github", "repo": "jemc/pony-inspect" }
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
    }

}

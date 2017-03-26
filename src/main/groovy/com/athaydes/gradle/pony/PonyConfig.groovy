package com.athaydes.gradle.pony

/**
 * The pony configuration block.
 */
class PonyConfig {

    String packageName = 'src'
    String testPackage = 'test'
    boolean debug = false
    boolean docs = false
    boolean library = false
    boolean strip = false
    boolean runtimebc = false
    boolean pic = false
    List<String> compileOptions = [ ]
    List<String> testOptions = [ ]

}

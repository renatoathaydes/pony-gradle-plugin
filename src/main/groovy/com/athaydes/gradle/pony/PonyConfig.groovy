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
    boolean stripDebugInfo = false
    boolean runtimeBC = false
    boolean usePic = false

}

package io.github.takusan23.dougaundroid.akaricorev5.gl

data class GlslSyntaxErrorException(
    val compileErrorMessage: String
) : RuntimeException()
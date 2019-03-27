package com.github.dreef3.teamcity.util.extensions

import java.nio.charset.StandardCharsets
import java.util.*

fun String.toUUID() = UUID.nameUUIDFromBytes(this.toByteArray(StandardCharsets.UTF_8)).toString()

fun String.toPascalCase() = this.toLowerCase().split(Regex("[\\W_]+")).joinToString("") { it.capitalize() }

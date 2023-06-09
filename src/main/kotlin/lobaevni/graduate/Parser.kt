package lobaevni.graduate

import lobaevni.graduate.jez.data.*
import lobaevni.graduate.jez.data.JezSourceConstant
import lobaevni.graduate.jez.data.JezVariable

private val letterRegex = "^[A-Z][A-Z0-9]*$".toRegex()
private val variableRegex = "^[a-z][a-z0-9]*$".toRegex()

internal fun String.parseLetters(): List<JezSourceConstant> {
    val letters = parseSequence().map { JezSourceConstant(it) }
    assert(letters.find { !(it.value as String).matches(letterRegex) } == null)
    return letters
}

internal fun String.parseVariables(): List<JezVariable> {
    val variables = parseSequence().map { JezVariable(it) }
    assert(variables.find { !(it.name as String).matches(variableRegex) } == null)
    return variables
}

internal fun String.parseEquation(letters: List<JezSourceConstant>, variables: List<JezVariable>): JezEquation {
    val eqStr = split("=").toMutableList()
    assert(eqStr.size == 2)

    val eqParts: MutableList<JezEquationPart> = mutableListOf(mutableListOf(), mutableListOf())
    eqStr.forEachIndexed { eqPartIdx, sourceEqPartStr ->
        var eqPartStr = sourceEqPartStr.trim()
        assert(eqPartStr.isNotEmpty())

        do {
            val element: JezElement? = if (eqPartStr[0].isUpperCase()) { //letter
                letters.find { eqPartStr.startsWith(it.value as String) }
            } else { //variable
                variables.find { eqPartStr.startsWith(it.name as String) }
            }
            assert(element != null) { """Couldn't parse element "$element"""" }
            eqParts[eqPartIdx] += listOf(element!!)

            val length: Int = when (element) {
                is JezSourceConstant -> (element.value as String).length
                is JezVariable -> (element.name as String).length
                else -> 0
            }
            assert(length > 0) { """Empty length of element "$element"""" }
            eqPartStr = eqPartStr.substring(length)
        } while (eqPartStr.isNotEmpty())
    }
    return JezEquation(eqParts[0], eqParts[1])
}

private fun String.parseSequence(): List<String> =
    trim('{', '}')
        .split(",")
        .map { it.trim() }

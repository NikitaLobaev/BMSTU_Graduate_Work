package lobaevni.graduate

object Utils {

    /**
     * Knuth–Morris–Pratt algorithm.
     * @return [List] with specified [newPart] replacement of all [repPart] occurrences.
     */
    fun <T> List<T>.replace(
        repPart: Collection<T>,
        newPart: Collection<T>,
        prefixFunctionValues: Collection<Int>? = null,
    ): List<T> {
        if (isEmpty() || repPart.isEmpty() || repPart == newPart) return this

        data class Acc(
            val element: T? = null,
            val buffer: MutableList<T> = mutableListOf(),
            val result: MutableList<T> = mutableListOf(),
        )

        val result = (listOf(Acc(null)) + map { element ->
            Acc(element)
        }).reduce { lastAcc, curAcc ->
            lastAcc.buffer.add(curAcc.element!!)

            while (lastAcc.buffer.isNotEmpty() &&
                lastAcc.buffer.last() != repPart.elementAt(lastAcc.buffer.size - 1)) {
                val dropCount = if (prefixFunctionValues != null) {
                    val pfValue = prefixFunctionValues.elementAt(lastAcc.buffer.size - 1)
                    if (pfValue != 0) {
                        lastAcc.buffer.size - pfValue
                    } else 1
                } else 1
                for (i in 0 until dropCount) {
                    lastAcc.result.add(lastAcc.buffer.removeFirst())
                }
            }

            if (lastAcc.buffer.size == repPart.size) {
                lastAcc.buffer.clear()
                lastAcc.result.addAll(newPart)
            }
            lastAcc
        }.also { acc ->
            acc.result.addAll(acc.buffer)
        }.result
        return result
    }

    /**
     * Computes prefix function for [Collection].
     * @return [ArrayList] of values of computed prefix function.
     */
    fun <T: Any> Collection<T>.prefixFunction(): ArrayList<Int> {
        data class Acc(
            val elements: ArrayList<T>,
            val pfValues: ArrayList<Int>,
        )

        return this
            .map { element ->
                Acc(arrayListOf(element), arrayListOf(0))
            }
            .reduceIndexedOrNull { i, lastAcc, curAcc ->
                lastAcc.elements.add(curAcc.elements.first())

                var j = lastAcc.pfValues.last()
                while (j > 0 && lastAcc.elements[i] != lastAcc.elements[j]) { //TODO: тоже можно в функциональном стиле, reversed reduce?...
                    j = lastAcc.pfValues[j - 1]
                }
                if (lastAcc.elements[i] == lastAcc.elements[j]) {
                    j++
                }

                lastAcc.pfValues.add(j)

                lastAcc
            }
            ?.pfValues ?: arrayListOf()
    }

    /**
     * @return cartesian product of two collections.
     */
    fun <T, U> cartesianProduct(c1: Collection<T>, c2: Collection<U>): List<Pair<T, U>> =
        c1.flatMap { lhsElem -> c2.map { rhsElem -> lhsElem to rhsElem } }

}

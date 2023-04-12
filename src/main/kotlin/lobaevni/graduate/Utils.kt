package lobaevni.graduate

object Utils {

    /**
     * Computes prefix function for [Collection].
     * @return [ArrayList] of values of computed prefix function.
     */
    fun <T: Any> Collection<T>.prefixFunction(): ArrayList<Int> {
        data class Acc(
            val elements: ArrayList<T>,
            val pfValues: ArrayList<Int>,
        )

        return map { element ->
            Acc(arrayListOf(element), arrayListOf(0))
        }.reduceIndexedOrNull { i, lastAcc, curAcc ->
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
        }?.pfValues ?: arrayListOf()
    }

    /**
     * @return cartesian product of two collections.
     */
    fun <T, U> cartesianProduct(c1: Collection<T>, c2: Collection<U>): List<Pair<T, U>> {
        return c1.flatMap { lhsElem -> c2.map { rhsElem -> lhsElem to rhsElem } }
    }

}

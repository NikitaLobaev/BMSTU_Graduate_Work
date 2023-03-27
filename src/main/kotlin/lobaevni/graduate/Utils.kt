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
            while (j > 0 && lastAcc.elements[i] != lastAcc.elements[j]) {
                j = lastAcc.pfValues[j - 1]
            }
            if (lastAcc.elements[i] == lastAcc.elements[j]) {
                j++
            }

            lastAcc.pfValues.add(j)

            lastAcc
        }?.pfValues ?: arrayListOf()
    }

}

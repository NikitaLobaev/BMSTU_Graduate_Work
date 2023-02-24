package lobaevni.graduate

object Utils {

    /**
     * Computes prefix function for [Collection].
     * @return [ArrayList] of values of computed prefix function.
     */
    fun <T: Any> Collection<T>.prefixFunction(): ArrayList<Int> { //TODO: try to rewrite this function in functional style
        val p = arrayListOf<Int>()
        if (isEmpty()) return p

        for (i in indices) {
            p.add(0)
        }

        var i = 1
        var j = 0
        while (i < size) {
            if (elementAt(i) == elementAt(j)) {
                j++
                p[i] = j
                i++
            } else if (j == 0) {
                p[i] = 0
                i++
            } else {
                j = p[j - 1]
            }
        }
        return p
    }

}

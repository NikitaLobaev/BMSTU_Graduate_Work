package lobaevni.graduate.jez.action

import lobaevni.graduate.jez.data.JezConstant
import lobaevni.graduate.jez.data.JezGeneratedConstant
import lobaevni.graduate.jez.data.JezState

internal data class JezReplaceConstantsAction(
    override val replaces: Collection<Pair<List<JezConstant>, List<JezGeneratedConstant>>>,
) : JezReplaceAction() {

    init {
        assert(replaces.all { (from, to) ->
            to.size == 1 && !from.contains(to.first())
        })
    }

    override fun applyAction(state: JezState): Boolean {
        if (!super.applyAction(state)) return false

        replaces.forEach { (_, to) ->
            val constant = to.first()
            assert(state.getOrPutGeneratedConstant(constant.source) == constant)
        }
        return true
    }

    override fun revertAction(state: JezState): Boolean {
        if (!super.revertAction(state)) return false

        replaces.forEach { (_, to) ->
            val constant = to.first()
            assert(state.removeGeneratedConstant(constant))
            /*if (state.removeGeneratedConstant(constant)) {
                println("WTH")
            }*/
        }
        return true
    }

}

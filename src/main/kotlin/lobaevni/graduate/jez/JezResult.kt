package lobaevni.graduate.jez

import io.github.rchowell.dotlin.DotRootGraph

/**
 * Describes result of Jez algorithm.
 */
data class JezResult(
    val sigma: JezSigma,
    val solutionState: SolutionState,
    val historyDotGraph: DotRootGraph?,
) {

    sealed class SolutionState {

        /**
         * Describes state, when equation was successfully solved.
         */
        object Found : SolutionState()

        sealed class NoSolution : SolutionState() {

            /**
             * Describes state, when algorithm is out of iterations and solution wasn't found, but may exist.
             */
            object NotEnoughIterations : NoSolution()

            /**
             * Describes state, when there is exactly no any solution may exist.
             */
            object Absolutely : NoSolution()

        }

    }

}

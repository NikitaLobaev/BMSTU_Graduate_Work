package lobaevni.graduate.jez.data

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

        /**
         * Describes states, when equation wasn't solved.
         */
        sealed class NotFound : SolutionState() {

            /**
             * Describes state, when there is exactly no any solution may exist.
             */
            object NoSolution : NotFound()

            /**
             * Describes state, when algorithm is out of iterations and solution wasn't found, but may exist.
             */
            object NotEnoughIterations : NotFound()

        }

    }

}

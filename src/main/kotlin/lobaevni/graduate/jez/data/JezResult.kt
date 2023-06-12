package lobaevni.graduate.jez.data

import io.github.rchowell.dotlin.DotRootGraph
import java.math.BigInteger

/**
 * Describes result of Jez algorithm.
 */
data class JezResult(
    val sigma: JezSigma,
    val solutionState: SolutionState,
    val iterationsCount: BigInteger,
    val historyDotGraph: DotRootGraph?,
) {

    sealed class SolutionState {

        /**
         * Describes states, when equation was successfully solved.
         */
        sealed class Found : SolutionState() {

            /**
             * Describes state, when exactly the minimal solution was found.
             */
            object Minimal : Found()

            /**
             * Describes state, when any of existing arbitrary solutions was found, optionally minimal.
             */
            object Arbitrary : Found()

        }

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

            /**
             * Describes state, when it's impossible to check history of recompressions and algorithm doesn't know
             * exactly why does the solution wasn't found.
             */
            object HistoryNotStored : NotFound()

            /**
             * Describes state, when revert was possible after dead-end final branch, but revert is not allowed.
             */
            object RevertNotAllowed : NotFound()

            /**
             * Describes state, when heuristic of extended negative restriction might have been a reason of failure.
             */
            object HeurExtNegRestDamage : NotFound()

        }

    }

}

package flaskoski.rs.smartmuseum.util

/**
 *
* Copyright (c) 2019 Felipe Ferreira Laskoski
* código fonte licenciado pela MIT License - https://opensource.org/licenses/MIT
 * */

class ParallelRequestsManager(
        internal var numberOfRequestsToFinish: Int) {

    val NONE: Int = 0
    val ONE_OF_TWO: Int = 1
    val BOTH = 2
    private var originalNumberOfRequestsToFinish: Int
    val isComplete: Boolean
        get() = numberOfRequestsToFinish == 0

    init {
        this.originalNumberOfRequestsToFinish = numberOfRequestsToFinish
    }

    fun getNumberOfRequestsToFinish(): Int? {
        return numberOfRequestsToFinish
    }

    fun setNumberOfRequestsToFinish(numberOfRequestsToFinish: Int) {
        this.numberOfRequestsToFinish = numberOfRequestsToFinish
        this.originalNumberOfRequestsToFinish = numberOfRequestsToFinish
    }

    fun decreaseRemainingRequests(): Int? {
        if (numberOfRequestsToFinish > 0)
            numberOfRequestsToFinish--
        return numberOfRequestsToFinish
    }

    fun reset() {
        this.numberOfRequestsToFinish = this.originalNumberOfRequestsToFinish
    }
}

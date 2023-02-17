package fsrs

import java.time.LocalDateTime

enum class State(val value: Int) {
    New(0),
    Learning(1),
    Review(2),
    Relearning(3)
}

enum class Rating(val value: Int) {
    Again(0),
    Hard(1),
    Good(2),
    Easy(3)
}

data class ReviewLog(
    var rating: Rating,
    var elapsedDays: Int,
    var scheduledDays: Int,
    var review: LocalDateTime,
    var state: State
)

data class Card(
    var due: LocalDateTime = LocalDateTime.now(),
    var stability: Float = 0F,
    var difficulty: Float = 0F,
    var elapsedDays: Int = 0,
    var scheduledDays: Int = 0,
    var reps: Int = 0,
    var lapses: Int = 0,
    var state: State = State.New,
    var lastReview: LocalDateTime = LocalDateTime.now()
)
data class SchedulingInfo(
    var card: Card,
    var reviewLog: ReviewLog
)

class SchedulingCards(card: Card) {
    var again: Card = card.copy()
    var hard: Card = card.copy()
    var good: Card = card.copy()
    var easy: Card = card.copy()

    fun updateState(state: State) {
        when (state) {
            State.New -> {
                again.state = State.Learning
                hard.state = State.Learning
                good.state = State.Learning
                easy.state = State.Review
                again.lapses += 1
            }
            State.Learning, State.Relearning -> {
                again.state = state
                hard.state = State.Review
                good.state = State.Review
                easy.state = State.Review
            }
            State.Review -> {
                again.state = State.Learning
                hard.state = State.Review
                good.state = State.Review
                easy.state = State.Review
                again.lapses += 1
            }
        }
    }

    fun schedule(now: LocalDateTime, hardInterval: Int, goodInterval: Int, easyInterval: Int) {
        again.scheduledDays = 0
        hard.scheduledDays = hardInterval
        good.scheduledDays = goodInterval
        easy.scheduledDays = easyInterval
        again.due = now.plusMinutes(5)
        hard.due = now.plusDays(hardInterval.toLong())
        good.due = now.plusDays(goodInterval.toLong())
        easy.due = now.plusDays(easyInterval.toLong())
    }

    fun recordLog(card:Card,now: LocalDateTime): Map<Rating, SchedulingInfo> {
        return mapOf(
            Rating.Again to SchedulingInfo(this.again,ReviewLog(Rating.Again,this.again.scheduledDays,card.elapsedDays,now,card.state)),
            Rating.Hard to SchedulingInfo(this.hard,ReviewLog(Rating.Hard,this.hard.scheduledDays,card.elapsedDays,now,card.state)),
            Rating.Good to SchedulingInfo(this.good,ReviewLog(Rating.Good,this.good.scheduledDays,card.elapsedDays,now,card.state)),
            Rating.Easy to SchedulingInfo(this.easy,ReviewLog(Rating.Easy,this.easy.scheduledDays,card.elapsedDays,now,card.state))

        )
    }


}


class Parameters(
    var requestRetention: Float = 0.9f,
    var maximumInterval: Int = 36500,
    var easyBonus: Float = 1.3f,
    var hardFactor: Float = 1.2f,
    var w: FloatArray = floatArrayOf(1.0f, 1.0f, 5.0f, -0.5f, -0.5f, 0.2f, 1.4f, -0.12f, 0.8f, 2.0f, -0.2f, 0.2f, 1.0f)
)
package fsrs

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 自由间隔重复调度算法
 * 地址：https://github.com/open-spaced-repetition/free-spaced-repetition-scheduler
 */
class FSRS {
    private var p = Parameters()
    init {
        p = Parameters()
    }
    fun repeat(card: Card, now: LocalDateTime): Map<Rating, SchedulingInfo> {
        if (card.state == State.New) {
            card.elapsedDays = 0
        } else {
            card.elapsedDays = ChronoUnit.DAYS.between(card.lastReview, now).toInt()
        }
        card.lastReview = now
        card.reps++

        val s = SchedulingCards(card)
        s.updateState(card.state)

        if (card.state == State.New) {
            initDS(s)

            s.again.due = now.plusMinutes(1)
            s.hard.due = now.plusMinutes(5)
            s.good.due = now.plusMinutes(10)
            val easyInterval = nextInterval(s.easy.stability * p.easyBonus)
            s.easy.scheduledDays = easyInterval
            s.easy.due = now.plusDays(easyInterval.toLong())
        } else if (card.state == State.Learning || card.state == State.Relearning) {
            val hardInterval = nextInterval(s.hard.stability)
            val goodInterval = maxOf(nextInterval(s.good.stability), hardInterval + 1)
            val easyInterval = maxOf(nextInterval(s.easy.stability * p.easyBonus), goodInterval + 1)

            s.schedule(now, hardInterval, goodInterval, easyInterval)
        } else if (card.state == State.Review) {
            val interval = card.elapsedDays
            val lastD = card.difficulty
            val lastS = card.stability
            val retrievability = exp(ln(0.9) * interval / lastS).toFloat()
            nextDs(s, lastD, lastS, retrievability)

            var hardInterval = nextInterval(lastS * p.hardFactor)
            var goodInterval = nextInterval(s.good.stability)
            hardInterval = minOf(hardInterval, goodInterval)
            goodInterval = maxOf(goodInterval, hardInterval + 1)
            val easyInterval = maxOf(nextInterval(s.easy.stability * p.hardFactor), goodInterval + 1)
            s.schedule(now, hardInterval, goodInterval, easyInterval)
        }
        return s.recordLog(card, now)
    }


    fun initDS(s: SchedulingCards) {
        s.again.difficulty = initDifficulty(Rating.Again.value)
        s.again.stability = initStability(Rating.Again.value)
        s.hard.difficulty = initDifficulty(Rating.Hard.value)
        s.hard.stability = initStability(Rating.Hard.value)
        s.good.difficulty = initDifficulty(Rating.Good.value)
        s.good.stability = initStability(Rating.Good.value)
        s.easy.difficulty = initDifficulty(Rating.Easy.value)
        s.easy.stability = initStability(Rating.Easy.value)
    }

    fun nextDs(s: SchedulingCards, lastD: Float, lastS: Float, retrievability: Float) {
        s.again.difficulty = nextDifficulty(lastD, Rating.Again.value)
        s.again.stability = nextForgetStability(s.again.difficulty, lastS, retrievability).toFloat()
        s.hard.difficulty = nextDifficulty(lastD, Rating.Hard.value)
        s.hard.stability = nextRecallStability(s.hard.difficulty, lastS, retrievability)
        s.good.difficulty = nextDifficulty(lastD, Rating.Good.value)
        s.good.stability = nextRecallStability(s.good.difficulty, lastS, retrievability)
        s.easy.difficulty = nextDifficulty(lastD, Rating.Easy.value)
        s.easy.stability = nextRecallStability(s.easy.difficulty, lastS, retrievability)
    }

    fun initStability(r: Int): Float {
        return Math.max(p.w[0] + p.w[1] * r, 0.1f)
    }

    fun initDifficulty(r: Int): Float {
        return Math.min(Math.max(p.w[2] + p.w[3] * (r - 2), 1f), 10f)
    }

    fun nextInterval(s: Float): Int {
        val newInterval = s * Math.log(p.requestRetention.toDouble()) / Math.log(0.9)
        return Math.min(Math.max(newInterval.roundToInt(), 1), p.maximumInterval)
    }


    fun nextDifficulty(d: Float, r: Int): Float {
        val nextD = d + p.w[4] * (r - 2)
        return meanReversion(p.w[2], nextD).coerceAtLeast(1f).coerceAtMost(10f)
    }


    fun meanReversion(init: Float, current: Float): Float {
        return p.w[5] * init + (1 - p.w[5]) * current
    }


    fun nextRecallStability(d: Float, s: Float, r: Float): Float {
        return s * (1 + exp(p.w[6].toDouble()) *
                (11 - d) *
                s.toDouble().pow(p.w[7].toDouble()) *
                (exp((1 - r) * p.w[8].toDouble()) - 1)).toFloat()
    }


    fun nextForgetStability(d: Float, s: Float, r: Float): Double {
        return p.w[9] * d.toDouble().pow(p.w[10].toDouble()) * s.toDouble().pow(p.w[11].toDouble()) * exp((1 - r) * p.w[12].toDouble()).toFloat()
    }

}

fun main() {
    val fsrs = FSRS()
    val card = Card()
    val now = LocalDateTime.now()
    val schedulingCards = fsrs.repeat(card,now)
    println(schedulingCards)
}
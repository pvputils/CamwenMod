package com.example.combat.clicking

/** Adapter around the unmodified upstream time plan and human/constant timing algorithms. */
class AttackClockTodoAi {
    private val human = HumanClickTimingTodoAi()
    private var humanTiming = true
    private val plan = ClickPlanTodoAi(ClickTimingTodoAi { recent, comboMs, cps, random ->
        (if (humanTiming) human else ConstantClickTimingTodoAi)
            .nextInterval(recent, comboMs, cps, random)
    })

    fun tick(now: Long, humanTiming: Boolean, minimumCps: Int, maximumCps: Int,
             cooldownCrossing: Boolean) {
        if (this.humanTiming != humanTiming) plan.reset(now)
        this.humanTiming = humanTiming
        plan.cps = minimumCps..maximumCps
        plan.maxPerTick = 1 // Each scheduled request represents exactly one hit.
        plan.enforced = java.util.function.IntPredicate { tick -> tick == 0 && cooldownCrossing }
        plan.tick(now)
    }

    fun ready() = plan.clicksAt(0) > 0

    fun consume(attack: java.util.function.BooleanSupplier): Boolean =
        plan.consume({ true }, { attack.asBoolean }) > 0
}

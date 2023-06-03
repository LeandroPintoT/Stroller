package cl.panmu.stroller.util

import kotlin.math.pow
import kotlin.math.sqrt

class ObsMath {
    companion object {
        fun getProgressFromMul(mul: Number, maxProg: Float): Int = (sqrt(mul.toDouble()) * maxProg).toInt()

        fun getMulFromProgress(prog: Int, maxProg: Float): Number = (prog.toDouble()/maxProg).pow(2)
    }
}
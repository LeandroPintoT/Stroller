package cl.panmu.stroller.util

class Util {
    companion object {
        /**
         * Limita el valor de un número [a] entre un mínimo ([min]) y un máximo ([max]).
         * @return el número limitado.
         */
        fun clamp(a: Int, min: Int, max: Int): Int = if (a < min) min else if (a > max) max else a
        /**
         * Limita el valor de un número [a] entre un mínimo ([min]) y un máximo ([max]).
         * @return el número limitado.
         */
        fun clamp(a: Float, min: Float, max: Float): Float = if (a < min) min else if (a > max) max else a
        /**
         * Limita el valor de un número [a] entre un mínimo ([min]) y un máximo ([max]).
         * @return el número limitado.
         */
        fun clamp(a: Float, min: Int, max: Int): Float = if (a < min) min.toFloat() else if (a > max) max.toFloat() else a
        /**
         * Limita el valor de un número [a] entre un mínimo ([min]) y un máximo ([max]).
         * @return el número limitado.
         */
        fun clamp(a: Double, min: Double, max: Double): Double = if (a < min) min else if (a > max) max else a
        /**
         * Limita el valor de un número [a] entre un mínimo ([min]) y un máximo ([max]).
         * @return el número limitado.
         */
        fun clamp(a: Double, min: Int, max: Int): Double = if (a < min) min.toDouble() else if (a > max) max.toDouble() else a
        /**
         * Limita el valor de un número [a] entre un mínimo ([min]) y un máximo ([max]).
         * @return el número limitado.
         */
        fun clamp(a: Long, min: Long, max: Long): Long = if (a < min) min else if (a > max) max else a
    }
}
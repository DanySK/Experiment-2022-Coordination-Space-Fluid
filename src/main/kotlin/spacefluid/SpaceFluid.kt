package spacefluid

import com.google.common.hash.HashCode
import com.google.common.hash.HashFunction
import com.google.common.hash.Hasher
import com.google.common.hash.Hashing
import gnu.trove.impl.HashFunctions
import it.unibo.alchemist.model.implementations.nodes.ProtelisNode
import it.unibo.alchemist.model.math.BidimensionalGaussian
import it.unibo.alchemist.nextDouble
import it.unibo.alchemist.protelis.AlchemistExecutionContext
import org.apache.commons.math3.random.MersenneTwister
import org.apache.commons.math3.util.MathArrays
import org.protelis.lang.datatype.Tuple

object SpaceFluid {

    private const val amplitude = 100.0
    private const val sigma = 5.0
    private val AlchemistExecutionContext<*>.id get(): Int = (deviceUID as ProtelisNode<*>).id

    private fun AlchemistExecutionContext<*>.center(): Pair<Double, Double> = environmentAccess.let { env ->
        val (x, y) = MathArrays.ebeAdd(env.offset, env.size.map { it / 2 }.toDoubleArray()).toList()
        x to y
    }

    private fun AlchemistExecutionContext<*>.localBivariate(center: Pair<Double, Double> = center()): Double =
        center.let { (x0, y0) ->
            val (deviceX, deviceY) = devicePosition.coordinates
            BidimensionalGaussian(amplitude, x0, y0, sigma, sigma).value(deviceX, deviceY)
        }

    private fun DoubleArray.toPair() = require(size == 2) { "Illegal pair for array ${toList()}" }.let {
        this[0] to this[1]
    }

    @JvmStatic
    fun bivariate(context: AlchemistExecutionContext<*>): Double = context.localBivariate()

    @JvmStatic
    fun multiBivariate(context: AlchemistExecutionContext<*>): Double = sequenceOf(
        context.center(),
        context.environmentAccess.offset.toPair(),
        MathArrays.ebeAdd(context.environmentAccess.offset, context.environmentAccess.size).toPair()
    ).map { context.localBivariate(it) }.sum()

    @JvmStatic
    fun constant() = amplitude / 2

}
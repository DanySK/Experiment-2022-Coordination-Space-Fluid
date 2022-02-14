package it.unibo.alchemist.loader.export.extractors

import it.unibo.alchemist.loader.export.Extractor
import it.unibo.alchemist.model.implementations.molecules.SimpleMolecule
import it.unibo.alchemist.model.interfaces.Environment
import it.unibo.alchemist.model.interfaces.Molecule
import it.unibo.alchemist.model.interfaces.Node
import it.unibo.alchemist.model.interfaces.Reaction
import it.unibo.alchemist.model.interfaces.Time
import org.apache.commons.math3.stat.descriptive.UnivariateStatistic
import org.apache.commons.math3.stat.descriptive.moment.Mean
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import org.apache.commons.math3.stat.descriptive.rank.Max
import org.apache.commons.math3.stat.descriptive.rank.Min

data class Bubble(val leader: Int, val statMolecule: String, val nodes: List<Node<*>>) {

    val values by lazy { nodes.map { it.getConcentration(SimpleMolecule(statMolecule)).numeric<Double>() }.toDoubleArray() }

    inline fun <reified S : UnivariateStatistic> stat(molecule: String): Double = values.stat<S>()
}

data class Bubbles(val bubbles: List<Bubble>) {

    constructor(
        environment: Environment<*, *>,
        targetMolecule: String,
        statMolecule: String,
    ): this(
        environment.nodes
            .groupBy { it.getConcentration(SimpleMolecule(targetMolecule))?.numeric<Int>() }
            .mapNotNull { (leader, nodes) -> leader?.let { Bubble(leader, statMolecule, nodes) } }
    )

    inline fun <reified Inner : UnivariateStatistic, reified Outer : UnivariateStatistic> statOfStat(molecule: String) =
        bubbles.map { it.stat<Inner>(molecule) }.stat<Outer>()
}

class CoordDataEport : Extractor<Double> {
    override val columnNames: List<String> = completeNames

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Reaction<T>?,
        time: Time,
        step: Long
    ): Map<String, Double> = names.flatMap { name ->
        sources.flatMap { source ->
            val target = "$source-$name"
            val bubbles: Bubbles = Bubbles(environment, target, source)
            listOf(
                "$target-intra-stdev-mean" to bubbles.statOfStat<StandardDeviation, Mean>(source),
                "$target-intra-stdev-stdev" to bubbles.statOfStat<StandardDeviation, StandardDeviation>(source),
                "$target-intra-stdev-max" to bubbles.statOfStat<StandardDeviation, Max>(source),
                "$target-intra-stdev-min" to bubbles.statOfStat<StandardDeviation, Min>(source),
                "$target-inter-mean-stdev" to bubbles.statOfStat<Mean, StandardDeviation>(source),
                "$target-bubble-count" to bubbles.bubbles.size.toDouble(),
                "$target-inter-mean-stdev" to bubbles.bubbles.size.toDouble(),
            )
        }
    }.toMap()

    companion object {
        val sources = listOf("bivariate", "constant", "multi", "uniform")
        val names = listOf("mean", "value", "variance").flatMap { lead ->
            listOf("combined", "distance", "valuediff").map { metric ->
                "$lead-$metric"
            }
        }
        val completeNames = sources.flatMap { source -> names.map { "$source-$it" } }
    }
}


private inline fun <reified T : Number> Any.numeric(): T = when {
    this is T -> this
    this is Number -> when (T::class) {
        Int::class -> toInt()
        Double::class -> toDouble()
        Long::class -> toLong()
        else -> TODO()
    } as T
    else -> TODO()
}

private fun Iterable<Double>.stdev() = toList().toDoubleArray().stdev()

private fun DoubleArray.stdev() = stat<StandardDeviation>()

inline fun <reified S: UnivariateStatistic> Iterable<Double>.stat() = toList().toDoubleArray().stat<S>()

inline fun <reified S: UnivariateStatistic> DoubleArray.stat() = when (S::class) {
    StandardDeviation::class -> StandardDeviation()
    Max::class -> Max()
    Min::class -> Min()
    Mean::class -> Mean()
    else -> TODO(S::class.simpleName ?: "???")
}.evaluate(this)

private inline operator fun <reified T : Number> Node<*>.get(molecule: String): T = get(SimpleMolecule(molecule))

private inline operator fun <reified T : Number> Node<*>.get(molecule: Molecule) =
    getConcentration(molecule).numeric<T>()

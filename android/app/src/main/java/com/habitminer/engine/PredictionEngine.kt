package com.habitminer.engine

import com.habitminer.data.AppUsageEntity
import com.habitminer.domain.AppIdentityResolver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PredictionEngine
    @Inject
    constructor(
        private val appIdentityResolver: AppIdentityResolver,
    ) {
        data class Prediction(val appName: String, val confidence: Float, val reasoning: String)

        fun buildTransitionMatrix(usage: List<AppUsageEntity>): Map<String, Map<String, Map<String, Int>>> {
            val validUsage = usage.filterNot { appIdentityResolver.isLauncher(it.packageName) }
            val matrix = mutableMapOf<String, MutableMap<String, MutableMap<String, Int>>>()

            val grouped = validUsage.groupBy { "${it.dayType}_${it.timeSlot}" }
            for ((bin, usages) in grouped) {
                val binMatrix = mutableMapOf<String, MutableMap<String, Int>>()
                val sorted = usages.sortedBy { it.startTime }

                for (i in 0 until sorted.size - 1) {
                    val current = sorted[i]
                    val next = sorted[i + 1]

                    // Enforce session proximity: only count transitions within 15 minutes
                    if (next.startTime - current.endTime <= 15 * 60 * 1000L) {
                        val currentMap = binMatrix.getOrPut(current.appName) { mutableMapOf() }
                        currentMap[next.appName] = currentMap.getOrDefault(next.appName, 0) + 1
                    }
                }
                matrix[bin] = binMatrix
            }
            return matrix
        }

        private var cachedMatrix: Map<String, Map<String, Map<String, Int>>> = emptyMap()
        private var lastUsageHash: Int = 0
        private var vocabularySize: Int = 0

        fun predict(
            usage: List<AppUsageEntity>,
            currentApp: String,
            timeSlot: String,
            dayType: String,
        ): List<Prediction> {
            val validUsage = usage.filterNot { appIdentityResolver.isLauncher(it.packageName) }
            val currentHash = validUsage.hashCode()
            if (currentHash != lastUsageHash) {
                cachedMatrix = buildTransitionMatrix(validUsage)
                vocabularySize = validUsage.map { it.appName }.distinct().size
                lastUsageHash = currentHash
            }

            val bin = "${dayType}_$timeSlot"
            val binMatrix = cachedMatrix[bin] ?: return emptyList()
            val transitions = binMatrix[currentApp]

            if (transitions == null || transitions.isEmpty()) {
                // fallback: most frequent in this bin
                val counts =
                    validUsage.filter { it.timeSlot == timeSlot && it.dayType == dayType }
                        .groupingBy { it.appName }.eachCount()
                val total = counts.values.sum()
                return counts.map { (cat, count) ->
                    Prediction(cat, count.toFloat() / total, "Frequent in this time slot")
                }.sortedByDescending { it.confidence }.take(3)
            }

            val alpha = 0.1f // Laplace smoothing
            val totalTransitions = transitions.values.sum() + (vocabularySize * alpha)

            return transitions.map { (cat, count) ->
                val prob = (count + alpha) / totalTransitions
                Prediction(cat, prob, "Based on your sequence history")
            }.sortedByDescending { it.confidence }.take(3)
        }

        fun getNextPredictionText(predictions: List<Prediction>): String {
            if (predictions.isEmpty()) return "Not enough data to predict"
            val top = predictions.first()
            val percent = (top.confidence * 100).toInt()

            return "Based on your routine → ${top.appName} ($percent%)"
        }
    }

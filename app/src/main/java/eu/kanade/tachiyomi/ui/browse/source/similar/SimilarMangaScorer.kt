package eu.kanade.tachiyomi.ui.browse.source.similar

import com.aallam.similarity.NormalizedLevenshtein
import tachiyomi.domain.manga.model.Manga
import java.text.Normalizer
import java.util.Locale

object SimilarMangaScorer {

    private val normalizedLevenshtein = NormalizedLevenshtein()
    private val separators = Regex("[^\\p{L}\\p{N}]+")
    private val combiningMarks = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val ignoredWords = setOf(
        "a",
        "an",
        "and",
        "as",
        "da",
        "das",
        "de",
        "do",
        "dos",
        "e",
        "em",
        "in",
        "na",
        "no",
        "o",
        "of",
        "the",
    )

    fun score(reference: Manga, candidate: Manga): Int {
        val genreScore = overlap(reference.genre.orEmpty(), candidate.genre.orEmpty())
        val creatorScore = maxOf(
            fieldSimilarity(reference.author, candidate.author),
            fieldSimilarity(reference.artist, candidate.artist),
        )
        val titleScore = normalizedLevenshtein.similarity(
            normalize(reference.title),
            normalize(candidate.title),
        )
        val descriptionScore = tokenSimilarity(reference.description, candidate.description)

        return (genreScore * 55 + creatorScore * 20 + titleScore * 15 + descriptionScore * 10)
            .toInt()
            .coerceIn(0, 100)
    }

    internal fun normalize(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .lowercase(Locale.ROOT)
            .replace(separators, " ")
            .trim()
    }

    private fun fieldSimilarity(first: String?, second: String?): Double {
        if (first.isNullOrBlank() || second.isNullOrBlank()) return 0.0

        val firstNormalized = normalize(first)
        val secondNormalized = normalize(second)
        if (firstNormalized == secondNormalized) return 1.0

        return tokenSimilarity(first, second)
    }

    private fun overlap(first: List<String>, second: List<String>): Double {
        val firstNormalized = first.map(::normalize).filter(String::isNotBlank).toSet()
        val secondNormalized = second.map(::normalize).filter(String::isNotBlank).toSet()
        if (firstNormalized.isEmpty() || secondNormalized.isEmpty()) return 0.0

        return firstNormalized.intersect(secondNormalized).size.toDouble() /
            maxOf(firstNormalized.size, secondNormalized.size)
    }

    private fun tokenSimilarity(first: String?, second: String?): Double {
        if (first.isNullOrBlank() || second.isNullOrBlank()) return 0.0

        val firstTokens = tokens(first)
        val secondTokens = tokens(second)
        if (firstTokens.isEmpty() || secondTokens.isEmpty()) return 0.0

        return firstTokens.intersect(secondTokens).size.toDouble() /
            maxOf(firstTokens.size, secondTokens.size)
    }

    private fun tokens(value: String): Set<String> {
        return normalize(value)
            .split(' ')
            .filter { it.length > 1 && it !in ignoredWords }
            .toSet()
    }
}

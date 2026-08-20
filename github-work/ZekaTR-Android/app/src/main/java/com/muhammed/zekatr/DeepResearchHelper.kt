package com.muhammed.zekatr

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object DeepResearchHelper {
    data class Report(val query: String, val sources: List<WebSearchHelper.SearchResult>)
    fun run(query: String): Report {
        val subQueries = listOf(query, "$query official documentation", "$query latest developments", "$query independent analysis")
        val pool = Executors.newFixedThreadPool(4)
        val futures = subQueries.map { q -> pool.submit<WebSearchHelper.SearchResult?> { WebSearchHelper.search(q) } }
        val results = futures.mapNotNull { runCatching { it.get(15, TimeUnit.SECONDS) }.getOrNull() }.distinctBy { it.sourceUrl ?: it.title }
        pool.shutdownNow()
        return Report(query, results.take(8))
    }
}

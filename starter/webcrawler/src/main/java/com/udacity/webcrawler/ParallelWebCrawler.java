package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
	private final Clock clock;
	private final Duration timeout;
	private final int popularWordCount;
	private final ForkJoinPool pool;
	private final PageParserFactory parserFactory;
	private final int maxDepth;
	private final List<Pattern> ignoreUrls;

	@Inject
	ParallelWebCrawler(
			Clock clock,
			PageParserFactory parserFactory,
			@MaxDepth int maxDepth,
			@Timeout Duration timeout,
			@PopularWordCount int popularWordCount,
			@TargetParallelism int threadCount,
			@IgnoredUrls List<Pattern> ignoreUrls) {
		this.clock = clock;
		this.timeout = timeout;
		this.popularWordCount = popularWordCount;
		this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
		this.parserFactory = parserFactory;
		this.maxDepth = maxDepth;
		this.ignoreUrls = ignoreUrls;
	}

	/**
	 * Ref: <a href="https://knowledge.udacity.com/questions/1008166">hint for step 4</a>
	 * @param startingUrls list of URLs used to download and parsing html.
	 * @return result of data crawled
	 */
	@Override
	public CrawlResult crawl(List<String> startingUrls) {
		Instant deadline = clock.instant().plus(timeout);
		ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();
		// If URL is visited, the method will skip
		ConcurrentSkipListSet<String> concurrentSkipListSet = new ConcurrentSkipListSet<>();
		for (String url : startingUrls) {
			pool.invoke(new CrawlInternalParallel(url, deadline, maxDepth, clock, parserFactory, concurrentMap, concurrentSkipListSet, ignoreUrls));
		}
		if (concurrentMap.isEmpty()) {
			return new CrawlResult.Builder()
					.setWordCounts(concurrentMap)
					.setUrlsVisited(concurrentSkipListSet.size())
					.build();
		}
		return new CrawlResult.Builder()
				.setWordCounts(WordCounts.sort(concurrentMap, popularWordCount))
				.setUrlsVisited(concurrentSkipListSet.size()).build();
	}

	@Override
	public int getMaxParallelism() {
		return Runtime.getRuntime().availableProcessors();
	}
}

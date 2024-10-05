package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

public class CrawlInternalParallel extends RecursiveTask<Boolean> {
	private final String url;
	private final Instant deadline;
	private final int maxDepth;
	private final Clock clock;
	private final PageParserFactory parserFactory;
	private final ConcurrentHashMap<String, Integer> counts;
	private final ConcurrentSkipListSet<String> visitedUrls;
	private final List<Pattern> ignoredUrls;

	public CrawlInternalParallel(String url, Instant deadline, int maxDepth, Clock clock, PageParserFactory parserFactory,
	                             ConcurrentHashMap<String, Integer> counts, ConcurrentSkipListSet<String> visitedUrls, List<Pattern> ignoredUrls) {
		this.url = url;
		this.deadline = deadline;
		this.maxDepth = maxDepth;
		this.clock = clock;
		this.parserFactory = parserFactory;
		this.counts = counts;
		this.visitedUrls = visitedUrls;
		this.ignoredUrls = ignoredUrls;
	}

	@Override
	protected Boolean compute() {
		if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
			return false;
		}

		for (Pattern urlPattern : ignoredUrls) {
			if (urlPattern.matcher(url).matches()) {
				return false;
			}
		}

		/*if (visitedUrls.contains(url)) {
			return false;
		} else {
			visitedUrls.add(url);
		}*/

		// Make thread-safe
		if (!visitedUrls.add(url)) {
			return false;
		}

		PageParser.Result result = parserFactory.get(url).parse();

		for (ConcurrentHashMap.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
			counts.compute(e.getKey(), (k, v) -> (v == null) ? e.getValue() : e.getValue() + v);
		}

		List<CrawlInternalParallel> subtasks = new ArrayList<>();
		for (String link : result.getLinks()) {
			subtasks.add(new CrawlInternalParallel(link, deadline, maxDepth - 1, clock, parserFactory, counts,
					visitedUrls, ignoredUrls));
		}
		invokeAll(subtasks);
		return true;
	}
}

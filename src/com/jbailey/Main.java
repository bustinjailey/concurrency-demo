package com.jbailey;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Knows how to get a string of HTTP content from a URL
 */
class FetchUrl {
    public static AtomicInteger startedRequestCounter = new AtomicInteger();

    public static String fetch(String url) throws IOException {
        System.out.println(startedRequestCounter.incrementAndGet());

        InputStream in = new URL(url).openStream();
        String response;
        try (Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.toString())) {
            scanner.useDelimiter("\\A");
            response = scanner.hasNext() ? scanner.next() : "";
        } finally {
            in.close();
        }
        return response;
    }
}

/**
 * A class to support parallel requests to fetch URLs
 */
class FetchUrlTask implements Callable<String> {
    private final String url;

    public FetchUrlTask(String urlToFetch) {
        this.url = urlToFetch;
    }

    @Override
    public String call() throws Exception {
        long startTime = System.nanoTime();
        String response = FetchUrl.fetch(url);
        long elapsed = System.nanoTime() - startTime;

        // Get a single short line from the response just to keep console output short
        response = response.replaceAll("\\n", "").substring(0, 100);
        System.out.printf("(%dms) %s: %s%n", TimeUnit.NANOSECONDS.toMillis(elapsed), this.url, response);
        return response;
    }
}

/**
 * Use a thread pool to fetch a bunch of content from URLs in parallel
 */
class FetchUrlParallel {
    public static void fetch(String[] urlsToFetch) throws InterruptedException {
        long startTime = System.nanoTime();
        final ExecutorService threadPool = Executors.newFixedThreadPool(20);

        List<FetchUrlTask> tasks = new ArrayList<>();

        for (String url : urlsToFetch) {
            tasks.add(new FetchUrlTask(url));
        }
        threadPool.invokeAll(tasks);

        long elapsed = System.nanoTime() - startTime;
        System.out.println("Total time (parallel): " + TimeUnit.NANOSECONDS.toMillis(elapsed) + "ms");
        threadPool.shutdown();
    }
}

/**
 * Fetch content from a list of URLs serially
 */
class FetchUrlSerial {
    public static void fetch(String[] urlsToFetch) throws IOException {
        long overallStartTime = System.nanoTime();
        for (String url : urlsToFetch) {
            long startTime = System.nanoTime();
            String response = FetchUrl.fetch(url);
            // Get a single short line from the response just to keep console output short
            response = response.replaceAll("\\n", "").substring(0, 100);
            long elapsed = System.nanoTime() - startTime;
            System.out.printf("(%dms) %s: %s%n", TimeUnit.NANOSECONDS.toMillis(elapsed), url, response);
        }
        long overallElapsed = System.nanoTime() - overallStartTime;
        System.out.println("Total time (serial): " + TimeUnit.NANOSECONDS.toMillis(overallElapsed) + "ms");

    }
}

/**
 * Makes HTTP requests against a list of URLs using a thread pool and serially, then prints the response from each
 */
public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        String[] urlsToFetch = {"https://www.google.com", "https://www.github.com", "https://www.cvent.com"};

        FetchUrlSerial.fetch(urlsToFetch);
        FetchUrlParallel.fetch(urlsToFetch);
        FetchUrlSerial.fetch(urlsToFetch);
        FetchUrlParallel.fetch(urlsToFetch);
    }
}

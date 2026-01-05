package com.shockwave.pdfium.search;

import androidx.annotation.NonNull;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages text search operations across an entire PDF document.
 * This class provides convenient methods to search through all pages and navigate results.
 *
 * <p>Usage example:
 * <pre>
 * DocumentSearch docSearch = new DocumentSearch(pdfiumCore, document);
 * try {
 *     List&lt;SearchResult&gt; results = docSearch.searchAll("search term");
 *     System.out.println("Found " + results.size() + " results across all pages");
 *
 *     for (SearchResult result : results) {
 *         System.out.println("Match on page " + result.getPageIndex() +
 *                          " at position " + result.getCharIndex());
 *     }
 * } finally {
 *     docSearch.close();
 * }
 * </pre>
 */
public class DocumentSearch implements AutoCloseable {

    private final PdfiumCore pdfiumCore;
    private final PdfDocument document;
    private final int pageCount;
    private final List<TextSearch> activeSearches;

    /**
     * Creates a new DocumentSearch instance for an entire PDF document.
     *
     * @param pdfiumCore The PdfiumCore instance
     * @param document   The PDF document to search
     */
    public DocumentSearch(@NonNull PdfiumCore pdfiumCore, @NonNull PdfDocument document) {
        this.pdfiumCore = pdfiumCore;
        this.document = document;
        this.pageCount = pdfiumCore.getPageCount(document);
        this.activeSearches = new ArrayList<>();
    }

    /**
     * Searches for all occurrences of a term across all pages in the document.
     *
     * @param searchTerm The text to search for
     * @return List of all search results found in the document, ordered by page number
     */
    @NonNull
    public List<SearchResult> searchAll(@NonNull String searchTerm) {
        return searchAll(searchTerm, 0);
    }

    /**
     * Searches for all occurrences of a term across all pages with specified flags.
     *
     * @param searchTerm The text to search for
     * @param flags      Search flags (e.g., TextSearch.FLAG_MATCH_CASE)
     * @return List of all search results found in the document, ordered by page number
     */
    @NonNull
    public List<SearchResult> searchAll(@NonNull String searchTerm, int flags) {
        List<SearchResult> allResults = new ArrayList<>();

        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            List<SearchResult> pageResults = searchPage(pageIndex, searchTerm, flags);
            allResults.addAll(pageResults);
        }

        return allResults;
    }

    /**
     * Searches for occurrences of a term on a specific page.
     *
     * @param pageIndex  The page index to search (0-based)
     * @param searchTerm The text to search for
     * @param flags      Search flags
     * @return List of search results found on the specified page
     */
    @NonNull
    public List<SearchResult> searchPage(int pageIndex, @NonNull String searchTerm, int flags) {
        if (pageIndex < 0 || pageIndex >= pageCount) {
            return new ArrayList<>();
        }

        TextSearch textSearch = new TextSearch(pdfiumCore, document, pageIndex);
        try {
            return textSearch.searchAll(searchTerm, flags);
        } finally {
            textSearch.close();
        }
    }

    /**
     * Searches for occurrences of a term on a specific page.
     *
     * @param pageIndex  The page index to search (0-based)
     * @param searchTerm The text to search for
     * @return List of search results found on the specified page
     */
    @NonNull
    public List<SearchResult> searchPage(int pageIndex, @NonNull String searchTerm) {
        return searchPage(pageIndex, searchTerm, 0);
    }

    /**
     * Searches for a term across a range of pages.
     *
     * @param startPage  The starting page index (inclusive, 0-based)
     * @param endPage    The ending page index (inclusive, 0-based)
     * @param searchTerm The text to search for
     * @param flags      Search flags
     * @return List of all search results found in the page range
     */
    @NonNull
    public List<SearchResult> searchRange(int startPage, int endPage, @NonNull String searchTerm, int flags) {
        List<SearchResult> results = new ArrayList<>();

        int start = Math.max(0, startPage);
        int end = Math.min(pageCount - 1, endPage);

        for (int pageIndex = start; pageIndex <= end; pageIndex++) {
            List<SearchResult> pageResults = searchPage(pageIndex, searchTerm, flags);
            results.addAll(pageResults);
        }

        return results;
    }

    /**
     * Searches for a term across a range of pages.
     *
     * @param startPage  The starting page index (inclusive, 0-based)
     * @param endPage    The ending page index (inclusive, 0-based)
     * @param searchTerm The text to search for
     * @return List of all search results found in the page range
     */
    @NonNull
    public List<SearchResult> searchRange(int startPage, int endPage, @NonNull String searchTerm) {
        return searchRange(startPage, endPage, searchTerm, 0);
    }

    /**
     * Creates a TextSearch instance for a specific page.
     * The caller is responsible for closing the returned TextSearch when done.
     *
     * @param pageIndex The page index (0-based)
     * @return A new TextSearch instance for the specified page
     */
    @NonNull
    public TextSearch createPageSearch(int pageIndex) {
        TextSearch textSearch = new TextSearch(pdfiumCore, document, pageIndex);
        activeSearches.add(textSearch);
        return textSearch;
    }

    /**
     * Gets the total number of pages in the document.
     *
     * @return The page count
     */
    public int getPageCount() {
        return pageCount;
    }

    /**
     * Closes all active searches and releases resources.
     */
    @Override
    public void close() {
        for (TextSearch search : activeSearches) {
            try {
                search.close();
            } catch (Exception e) {
                // Log but continue closing others
            }
        }
        activeSearches.clear();
    }
}

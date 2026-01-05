package com.shockwave.pdfium.search;

import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages text search operations within a PDF page.
 * This class handles the lifecycle of a search session and provides methods
 * to find and navigate through search results.
 *
 * <p>Usage example:
 * <pre>
 * TextSearch search = new TextSearch(pdfiumCore, document, pageIndex);
 * try {
 *     List&lt;SearchResult&gt; results = search.searchAll("search term");
 *     for (SearchResult result : results) {
 *         // Process each result
 *         List&lt;RectF&gt; rects = result.getBoundingRects();
 *         // Use rects for highlighting
 *     }
 * } finally {
 *     search.close();
 * }
 * </pre>
 */
public class TextSearch implements AutoCloseable {
    
    /**
     * Search flag: Match case
     */
    public static final int FLAG_MATCH_CASE = 0x00000001;
    
    /**
     * Search flag: Match whole word
     */
    public static final int FLAG_MATCH_WHOLE_WORD = 0x00000002;
    
    /**
     * Search flag: Search consecutive (from current position)
     */
    public static final int FLAG_CONSECUTIVE = 0x00000004;

    private final PdfiumCore pdfiumCore;
    private final PdfDocument document;
    private final int pageIndex;
    private long pagePtr = 0;
    private long textPagePtr = 0;
    private long searchHandlePtr = 0;
    private String currentSearchTerm = null;
    private int currentFlags = 0;

    /**
     * Creates a new TextSearch instance for a specific page.
     *
     * @param pdfiumCore The PdfiumCore instance
     * @param document   The PDF document
     * @param pageIndex  The page index to search (0-based)
     */
    public TextSearch(@NonNull PdfiumCore pdfiumCore, @NonNull PdfDocument document, int pageIndex) {
        this.pdfiumCore = pdfiumCore;
        this.document = document;
        this.pageIndex = pageIndex;
    }

    /**
     * Searches for all occurrences of a term on the page.
     *
     * @param searchTerm The text to search for
     * @return List of all search results found on the page
     */
    @NonNull
    public List<SearchResult> searchAll(@NonNull String searchTerm) {
        return searchAll(searchTerm, 0);
    }

    /**
     * Searches for all occurrences of a term on the page with specified flags.
     *
     * @param searchTerm The text to search for
     * @param flags      Search flags (e.g., FLAG_MATCH_CASE | FLAG_MATCH_WHOLE_WORD)
     * @return List of all search results found on the page
     */
    @NonNull
    public List<SearchResult> searchAll(@NonNull String searchTerm, int flags) {
        List<SearchResult> results = new ArrayList<>();
        
        if (searchTerm.isEmpty()) {
            return results;
        }

        ensureTextPageLoaded();
        startSearch(searchTerm, flags);

        try {
            while (findNext()) {
                SearchResult result = getCurrentResult();
                if (result != null) {
                    results.add(result);
                }
            }
        } finally {
            endSearch();
        }

        return results;
    }

    /**
     * Starts a new search session.
     *
     * @param searchTerm The text to search for
     * @param flags      Search flags
     */
    public void startSearch(@NonNull String searchTerm, int flags) {
        endSearch(); // End any existing search
        ensureTextPageLoaded();

        this.currentSearchTerm = searchTerm;
        this.currentFlags = flags;

        long keyStrPtr = PdfiumCore.nativeGetStringChars(searchTerm);
        try {
            searchHandlePtr = pdfiumCore.nativeFindTextPageStart(textPagePtr, keyStrPtr, flags, 0);
        } finally {
            PdfiumCore.nativeReleaseStringChars(keyStrPtr);
        }
    }

    /**
     * Finds the next occurrence of the search term.
     *
     * @return true if another result was found, false otherwise
     */
    public boolean findNext() {
        if (searchHandlePtr == 0) {
            return false;
        }
        return pdfiumCore.nativeFindTextPageNext(searchHandlePtr);
    }

    /**
     * Finds the previous occurrence of the search term.
     *
     * @return true if a previous result was found, false otherwise
     */
    public boolean findPrev() {
        if (searchHandlePtr == 0) {
            return false;
        }
        return pdfiumCore.nativeFindTextPagePrev(searchHandlePtr);
    }

    /**
     * Gets the current search result.
     *
     * @return The current SearchResult, or null if no search is active or no result found
     */
    @Nullable
    public SearchResult getCurrentResult() {
        if (searchHandlePtr == 0) {
            return null;
        }

        int charIndex = pdfiumCore.nativeGetFindIdx(searchHandlePtr);
        int charCount = pdfiumCore.nativeGetFindLength(searchHandlePtr);

        if (charIndex < 0 || charCount <= 0) {
            return null;
        }

        SearchResult result = new SearchResult(charIndex, charCount, pageIndex);
        
        // Get bounding rectangles for the result
        if (textPagePtr != 0) {
            int rectCount = pdfiumCore.nativeCountRects(textPagePtr, charIndex, charCount);
            if (rectCount > 0) {
                // Note: Getting actual rect coordinates requires page dimensions
                // This is a simplified version. Full implementation would need page width/height
                // and call appropriate native methods to get actual rectangles
            }
        }

        // Get the matched text
        if (currentSearchTerm != null) {
            result.setMatchedText(currentSearchTerm);
        }

        return result;
    }

    /**
     * Gets bounding rectangles for a character range.
     * 
     * Note: This method currently returns an empty list. Full implementation requires
     * integration with PdfiumCore's getTextRects method or similar coordinate conversion
     * from page coordinates to device coordinates with proper offset handling.
     * 
     * TODO: Implement proper rectangle retrieval using:
     * - FPDFText_GetRect to get page coordinates
     * - FPDF_PageToDevice for coordinate conversion
     * - Proper offset and size calculations
     *
     * @param charIndex  Starting character index
     * @param charCount  Number of characters
     * @param pageWidth  Page width in pixels
     * @param pageHeight Page height in pixels
     * @return List of bounding rectangles (currently empty - to be implemented)
     */
    @NonNull
    public List<RectF> getBoundingRects(int charIndex, int charCount, int pageWidth, int pageHeight) {
        List<RectF> rects = new ArrayList<>();
        
        if (textPagePtr == 0) {
            return rects;
        }

        // TODO: Full implementation needed
        // Would require calling native methods to:
        // 1. Get rect count: pdfiumCore.nativeCountRects(textPagePtr, charIndex, charCount)
        // 2. For each rect, get coordinates and convert to device coordinates
        // 3. Apply proper offsets and transformations
        // See PdfiumCore.getTextRects for reference implementation
        
        return rects;
    }

    /**
     * Ends the current search session and releases search-related resources.
     */
    public void endSearch() {
        if (searchHandlePtr != 0) {
            pdfiumCore.nativeFindTextPageEnd(searchHandlePtr);
            searchHandlePtr = 0;
        }
        currentSearchTerm = null;
    }

    /**
     * Closes the text search and releases all resources including the text page.
     * Note: The page itself is managed by the PdfDocument and will be closed when
     * closeDocument() is called.
     */
    @Override
    public void close() {
        endSearch();
        if (textPagePtr != 0) {
            pdfiumCore.closeTextPage(textPagePtr);
            textPagePtr = 0;
        }
        pagePtr = 0; // Page lifecycle is managed by PdfDocument
    }

    private void ensureTextPageLoaded() {
        if (textPagePtr == 0) {
            // Load the page first (will be registered in document)
            pagePtr = pdfiumCore.openPage(document, pageIndex);
            if (pagePtr != 0) {
                textPagePtr = pdfiumCore.nativeLoadTextPage(pagePtr);
            }
        }
    }

    /**
     * Gets the current search term.
     *
     * @return The current search term, or null if no search is active
     */
    @Nullable
    public String getCurrentSearchTerm() {
        return currentSearchTerm;
    }

    /**
     * Gets the page index this search instance is operating on.
     *
     * @return The page index (0-based)
     */
    public int getPageIndex() {
        return pageIndex;
    }
}

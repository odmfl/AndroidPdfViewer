package com.shockwave.pdfium.search;

import android.graphics.RectF;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single search result in a PDF document.
 * Contains the result index, character count, and bounding rectangles for highlighting.
 */
public class SearchResult {
    private final int charIndex;
    private final int charCount;
    private final List<RectF> rects;
    private final int pageIndex;
    private String matchedText;

    /**
     * Creates a new SearchResult.
     *
     * @param charIndex The starting character index of the match in the page
     * @param charCount The number of characters in the match
     * @param pageIndex The page index where this result was found
     */
    public SearchResult(int charIndex, int charCount, int pageIndex) {
        this.charIndex = charIndex;
        this.charCount = charCount;
        this.pageIndex = pageIndex;
        this.rects = new ArrayList<>();
    }

    /**
     * Gets the starting character index of the search result.
     *
     * @return The character index
     */
    public int getCharIndex() {
        return charIndex;
    }

    /**
     * Gets the number of characters in the search result.
     *
     * @return The character count
     */
    public int getCharCount() {
        return charCount;
    }

    /**
     * Gets the page index where this result was found.
     *
     * @return The page index (0-based)
     */
    public int getPageIndex() {
        return pageIndex;
    }

    /**
     * Gets the bounding rectangles for this search result.
     * Multiple rectangles may be returned if the text spans multiple lines.
     *
     * @return List of bounding rectangles
     */
    @NonNull
    public List<RectF> getBoundingRects() {
        return rects;
    }

    /**
     * Sets the bounding rectangles for this search result.
     *
     * @param rects List of bounding rectangles
     */
    public void setBoundingRects(@NonNull List<RectF> rects) {
        this.rects.clear();
        this.rects.addAll(rects);
    }

    /**
     * Adds a bounding rectangle to this search result.
     *
     * @param rect The rectangle to add
     */
    public void addBoundingRect(@NonNull RectF rect) {
        this.rects.add(rect);
    }

    /**
     * Gets the matched text.
     *
     * @return The matched text, or null if not set
     */
    public String getMatchedText() {
        return matchedText;
    }

    /**
     * Sets the matched text.
     *
     * @param matchedText The matched text
     */
    public void setMatchedText(String matchedText) {
        this.matchedText = matchedText;
    }

    @NonNull
    @Override
    public String toString() {
        return "SearchResult{" +
                "pageIndex=" + pageIndex +
                ", charIndex=" + charIndex +
                ", charCount=" + charCount +
                ", matchedText='" + matchedText + '\'' +
                ", rects=" + rects.size() +
                '}';
    }
}

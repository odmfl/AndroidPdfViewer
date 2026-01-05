# Android PDF Viewer - Text Search API Documentation

## Overview

The Android PDF Viewer library provides powerful text search capabilities through a clean, intuitive API. The search functionality is built on top of PDFium's native text search APIs, providing fast and accurate text searching across PDF documents.

## Core Classes

### SearchResult

Represents a single search result within a PDF document.

```java
public class SearchResult {
    public int getCharIndex();        // Starting character index
    public int getCharCount();        // Number of characters in match
    public int getPageIndex();        // Page where result was found (0-based)
    public List<RectF> getBoundingRects();  // Rectangles for highlighting
    public String getMatchedText();   // The matched text string
}
```

### TextSearch

Manages text search operations within a single PDF page.

```java
public class TextSearch implements AutoCloseable {
    // Search flags
    public static final int FLAG_MATCH_CASE = 0x00000001;
    public static final int FLAG_MATCH_WHOLE_WORD = 0x00000002;
    public static final int FLAG_CONSECUTIVE = 0x00000004;
    
    // Constructor
    public TextSearch(PdfiumCore pdfiumCore, PdfDocument document, int pageIndex);
    
    // Search methods
    public List<SearchResult> searchAll(String searchTerm);
    public List<SearchResult> searchAll(String searchTerm, int flags);
    public void startSearch(String searchTerm, int flags);
    public boolean findNext();
    public SearchResult getCurrentResult();
    public void endSearch();
}
```

### DocumentSearch

Manages text search operations across an entire PDF document.

```java
public class DocumentSearch implements AutoCloseable {
    // Constructor
    public DocumentSearch(PdfiumCore pdfiumCore, PdfDocument document);
    
    // Search methods
    public List<SearchResult> searchAll(String searchTerm);
    public List<SearchResult> searchAll(String searchTerm, int flags);
    public List<SearchResult> searchPage(int pageIndex, String searchTerm);
    public List<SearchResult> searchRange(int startPage, int endPage, String searchTerm);
    public TextSearch createPageSearch(int pageIndex);
    public int getPageCount();
}
```

## Usage Examples

### Basic Search on a Single Page

```java
// Open PDF document
PdfiumCore pdfiumCore = new PdfiumCore(context);
PdfDocument document = pdfiumCore.newDocument(parcelFileDescriptor);

// Search on page 0
TextSearch search = new TextSearch(pdfiumCore, document, 0);
try {
    List<SearchResult> results = search.searchAll("Android");
    
    for (SearchResult result : results) {
        Log.d(TAG, "Found match at position " + result.getCharIndex());
        Log.d(TAG, "Matched text: " + result.getMatchedText());
        
        // Get bounding rectangles for highlighting
        List<RectF> rects = result.getBoundingRects();
        for (RectF rect : rects) {
            // Draw highlight rectangle
        }
    }
} finally {
    search.close();
}
```

### Case-Sensitive Search

```java
TextSearch search = new TextSearch(pdfiumCore, document, pageIndex);
try {
    // Search with case sensitivity
    List<SearchResult> results = search.searchAll(
        "Android",
        TextSearch.FLAG_MATCH_CASE
    );
} finally {
    search.close();
}
```

### Whole Word Search

```java
TextSearch search = new TextSearch(pdfiumCore, document, pageIndex);
try {
    // Search for whole words only
    List<SearchResult> results = search.searchAll(
        "PDF",
        TextSearch.FLAG_MATCH_WHOLE_WORD
    );
} finally {
    search.close();
}
```

### Combined Search Flags

```java
TextSearch search = new TextSearch(pdfiumCore, document, pageIndex);
try {
    // Case-sensitive, whole word search
    List<SearchResult> results = search.searchAll(
        "Android",
        TextSearch.FLAG_MATCH_CASE | TextSearch.FLAG_MATCH_WHOLE_WORD
    );
} finally {
    search.close();
}
```

### Search Across All Pages

```java
DocumentSearch docSearch = new DocumentSearch(pdfiumCore, document);
try {
    List<SearchResult> allResults = docSearch.searchAll("Android");
    
    Log.d(TAG, "Found " + allResults.size() + " results across all pages");
    
    // Group results by page
    Map<Integer, List<SearchResult>> resultsByPage = new HashMap<>();
    for (SearchResult result : allResults) {
        int page = result.getPageIndex();
        if (!resultsByPage.containsKey(page)) {
            resultsByPage.put(page, new ArrayList<>());
        }
        resultsByPage.get(page).add(result);
    }
    
    // Display results
    for (Map.Entry<Integer, List<SearchResult>> entry : resultsByPage.entrySet()) {
        Log.d(TAG, "Page " + entry.getKey() + ": " + entry.getValue().size() + " matches");
    }
} finally {
    docSearch.close();
}
```

### Search Within a Page Range

```java
DocumentSearch docSearch = new DocumentSearch(pdfiumCore, document);
try {
    // Search only in pages 0-5
    List<SearchResult> results = docSearch.searchRange(0, 5, "Android");
    
    Log.d(TAG, "Found " + results.size() + " results in pages 0-5");
} finally {
    docSearch.close();
}
```

### Interactive Search Session

```java
TextSearch search = new TextSearch(pdfiumCore, document, pageIndex);
try {
    // Start search session
    search.startSearch("Android", 0);
    
    // Iterate through results manually
    while (search.findNext()) {
        SearchResult result = search.getCurrentResult();
        if (result != null) {
            Log.d(TAG, "Found at: " + result.getCharIndex());
            
            // Optionally break after first result
            break;
        }
    }
    
    // End search when done
    search.endSearch();
} finally {
    search.close();
}
```

### Search with Result Highlighting

```java
TextSearch search = new TextSearch(pdfiumCore, document, pageIndex);
try {
    List<SearchResult> results = search.searchAll("Android");
    
    // Get page dimensions
    int pageWidth = pdfiumCore.getPageWidth(document, pageIndex);
    int pageHeight = pdfiumCore.getPageHeight(document, pageIndex);
    
    for (SearchResult result : results) {
        // Get bounding rectangles for highlighting
        List<RectF> rects = search.getBoundingRects(
            result.getCharIndex(),
            result.getCharCount(),
            pageWidth,
            pageHeight
        );
        
        // Draw highlights
        for (RectF rect : rects) {
            // Use rect coordinates to draw on canvas
            canvas.drawRect(rect, highlightPaint);
        }
    }
} finally {
    search.close();
}
```

### Asynchronous Search (Recommended for Large Documents)

```java
ExecutorService executor = Executors.newSingleThreadExecutor();
executor.execute(() -> {
    DocumentSearch docSearch = new DocumentSearch(pdfiumCore, document);
    try {
        List<SearchResult> results = docSearch.searchAll("Android");
        
        // Update UI on main thread
        runOnUiThread(() -> {
            updateSearchResults(results);
        });
    } finally {
        docSearch.close();
    }
});
```

### Search with Progress Callback

```java
ExecutorService executor = Executors.newSingleThreadExecutor();
executor.execute(() -> {
    DocumentSearch docSearch = new DocumentSearch(pdfiumCore, document);
    try {
        int totalPages = docSearch.getPageCount();
        List<SearchResult> allResults = new ArrayList<>();
        
        for (int page = 0; page < totalPages; page++) {
            List<SearchResult> pageResults = docSearch.searchPage(page, "Android");
            allResults.addAll(pageResults);
            
            // Update progress
            int finalPage = page;
            runOnUiThread(() -> {
                updateProgress((finalPage + 1) * 100 / totalPages);
            });
        }
        
        // Update UI with final results
        runOnUiThread(() -> {
            updateSearchResults(allResults);
        });
    } finally {
        docSearch.close();
    }
});
```

## Memory Management

### Proper Resource Cleanup

Always close search objects to free native resources:

```java
// Using try-with-resources (recommended)
try (TextSearch search = new TextSearch(pdfiumCore, document, pageIndex)) {
    List<SearchResult> results = search.searchAll("Android");
    // Use results
}

// Or manual cleanup
TextSearch search = null;
try {
    search = new TextSearch(pdfiumCore, document, pageIndex);
    List<SearchResult> results = search.searchAll("Android");
    // Use results
} finally {
    if (search != null) {
        search.close();
    }
}
```

### Reusing Search Objects

You can reuse a TextSearch object for multiple searches on the same page:

```java
TextSearch search = new TextSearch(pdfiumCore, document, pageIndex);
try {
    // First search
    List<SearchResult> results1 = search.searchAll("Android");
    
    // Second search (automatically cleans up previous search)
    List<SearchResult> results2 = search.searchAll("PDF");
} finally {
    search.close();
}
```

## Best Practices

### 1. Use DocumentSearch for Multi-Page Searches

When searching across multiple pages, use `DocumentSearch` instead of creating multiple `TextSearch` instances:

```java
// Good
DocumentSearch docSearch = new DocumentSearch(pdfiumCore, document);
List<SearchResult> results = docSearch.searchAll("term");

// Avoid
for (int i = 0; i < pageCount; i++) {
    TextSearch search = new TextSearch(pdfiumCore, document, i);
    // ...
}
```

### 2. Perform Searches on Background Threads

Text search can be CPU-intensive for large documents. Always perform searches on background threads:

```java
ExecutorService executor = Executors.newSingleThreadExecutor();
executor.execute(() -> {
    // Perform search
    List<SearchResult> results = docSearch.searchAll("term");
    
    // Update UI on main thread
    runOnUiThread(() -> displayResults(results));
});
```

### 3. Limit Search Results

For very large documents, consider limiting the number of results:

```java
DocumentSearch docSearch = new DocumentSearch(pdfiumCore, document);
List<SearchResult> results = docSearch.searchAll("term");

// Limit to first 100 results
if (results.size() > 100) {
    results = results.subList(0, 100);
}
```

### 4. Cache Search Results

If users are likely to navigate through search results multiple times, cache the results:

```java
private Map<String, List<SearchResult>> searchCache = new HashMap<>();

public List<SearchResult> cachedSearch(String term) {
    if (searchCache.containsKey(term)) {
        return searchCache.get(term);
    }
    
    List<SearchResult> results = documentSearch.searchAll(term);
    searchCache.put(term, results);
    return results;
}
```

## Error Handling

### Handling Invalid Page Indices

```java
DocumentSearch docSearch = new DocumentSearch(pdfiumCore, document);
int pageIndex = 100; // Might be out of bounds

if (pageIndex >= 0 && pageIndex < docSearch.getPageCount()) {
    List<SearchResult> results = docSearch.searchPage(pageIndex, "term");
} else {
    Log.e(TAG, "Invalid page index: " + pageIndex);
}
```

### Handling Empty Documents

```java
DocumentSearch docSearch = new DocumentSearch(pdfiumCore, document);
if (docSearch.getPageCount() == 0) {
    Log.w(TAG, "Document has no pages");
    return;
}

List<SearchResult> results = docSearch.searchAll("term");
```

## Performance Considerations

- Text search performance depends on document size and complexity
- Typical search times:
  - Small documents (< 10 pages): < 100ms per page
  - Medium documents (10-100 pages): 100-500ms per page
  - Large documents (> 100 pages): 500ms-2s per page
- Always perform searches on background threads
- Consider implementing pagination for very large result sets
- Use search flags judiciously (they may increase search time)

## Integration with PDFView

The text search API can be integrated with the `PDFView` component for visual search results:

```java
// Perform search
List<SearchResult> results = documentSearch.searchAll("Android");

// Highlight results in PDFView
for (SearchResult result : results) {
    // Convert search result to PDFView highlight
    pdfView.addHighlight(
        result.getPageIndex(),
        result.getCharIndex(),
        result.getCharCount()
    );
}
```

See the sample application for a complete working example.

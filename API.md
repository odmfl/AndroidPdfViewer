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
    public boolean findPrev();
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

### Bidirectional Search (Forward and Backward)

```java
TextSearch search = new TextSearch(pdfiumCore, document, pageIndex);
try {
    // Start search session
    search.startSearch("Android", 0);
    
    // Find first result
    if (search.findNext()) {
        SearchResult result = search.getCurrentResult();
        Log.d(TAG, "First result at: " + result.getCharIndex());
    }
    
    // Navigate forward
    if (search.findNext()) {
        SearchResult result = search.getCurrentResult();
        Log.d(TAG, "Next result at: " + result.getCharIndex());
    }
    
    // Navigate backward
    if (search.findPrev()) {
        SearchResult result = search.getCurrentResult();
        Log.d(TAG, "Previous result at: " + result.getCharIndex());
    }
    
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

The Android PDF Viewer library provides built-in integration between the text search API and the `PDFView` component, automatically handling visual highlighting of search results.

### Built-in Search with Visual Highlighting

The `PDFView` class includes integrated search functionality that automatically highlights results:

```java
// Initialize PDFView
PDFView pdfView = findViewById(R.id.pdfView);

// Configure PDF with search listener
pdfView.fromAsset("sample.pdf")
    .onSearchMatch((page, totalMatched, word) -> {
        // Called when matches are found on a page
        Log.d(TAG, "Found " + totalMatched + " matches on page " + page);
    })
    .load();

// Perform search - automatically highlights results
pdfView.search("Android");

// Navigate through results
pdfView.navigateToNextSearchItem();
pdfView.navigateToPreviousSearchItem();

// Clear search and remove highlights
pdfView.clearSearch();
```

### Search Result Highlighting

Search results are automatically highlighted with two different colors:
- **Focused result**: Highlighted in a distinct color (current search position)
- **Other results**: Highlighted in a semi-transparent color

The highlighting is performed by the `PDocSelection` view layer, which:
1. Retrieves search results from `SearchRecord` objects
2. Converts page coordinates to screen coordinates
3. Draws highlight rectangles with appropriate paints
4. Updates highlights when zooming or scrolling

### Customizing Search Highlight Colors

While the default search highlighting is built-in, you can customize the highlight appearance through the `PDocSelection` component:

```java
// Access the selection view
PDocSelection selectionView = findViewById(R.id.docSelection);

// The search highlight paints can be customized in your layout XML or programmatically
// See PDocSelection class for available customization options
```

### Search Navigation

The library provides convenient methods for navigating search results:

```java
// Navigate to next result
boolean hasNext = pdfView.navigateToNextSearchItem();

// Navigate to previous result
boolean hasPrev = pdfView.navigateToPreviousSearchItem();

// Navigate to specific search result by index
boolean success = pdfView.navigateToSearchItem(pageIndex, searchItemIndex);

// Get current search position
int currentIndex = pdfView.getCurrentFocusedSearchItemIndex();
```

### Complete Search Example with UI

```java
public class MainActivity extends AppCompatActivity {
    private PDFView pdfView;
    private SearchView searchView;
    private TextView resultCounter;
    private int currentSearchIndex = 0;
    private int totalSearchResults = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        pdfView = findViewById(R.id.pdfView);
        searchView = findViewById(R.id.searchView);
        resultCounter = findViewById(R.id.resultCounter);
        
        // Setup PDF with search listener
        pdfView.fromAsset("sample.pdf")
            .onSearchMatch((page, totalMatched, word) -> {
                totalSearchResults += totalMatched;
                updateResultCounter();
            })
            .load();
        
        // Setup search input
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                // Optional: implement debounced real-time search
                return false;
            }
        });
        
        // Setup navigation buttons
        findViewById(R.id.prevButton).setOnClickListener(v -> {
            if (pdfView.navigateToPreviousSearchItem()) {
                currentSearchIndex--;
                updateResultCounter();
            }
        });
        
        findViewById(R.id.nextButton).setOnClickListener(v -> {
            if (pdfView.navigateToNextSearchItem()) {
                currentSearchIndex++;
                updateResultCounter();
            }
        });
    }
    
    private void performSearch(String query) {
        // Reset counters
        currentSearchIndex = 0;
        totalSearchResults = 0;
        
        // Perform search (automatically highlights results)
        pdfView.search(query);
        
        // Update UI
        updateResultCounter();
    }
    
    private void updateResultCounter() {
        if (totalSearchResults == 0) {
            resultCounter.setText("No results");
        } else {
            resultCounter.setText(String.format("%d of %d", 
                currentSearchIndex + 1, totalSearchResults));
        }
    }
}
```

### Layout XML for Search UI

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <!-- Search bar -->
    <androidx.appcompat.widget.SearchView
        android:id="@+id/searchView"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />
    
    <!-- PDF View -->
    <com.github.barteksc.pdfviewer.PDFView
        android:id="@+id/pdfView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/searchView"
        app:layout_constraintBottom_toTopOf="@id/searchNavigation"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />
    
    <!-- Selection overlay for highlighting -->
    <com.github.barteksc.pdfviewer.PDocSelection
        android:id="@+id/docSelection"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="@id/pdfView"
        app:layout_constraintBottom_toBottomOf="@id/pdfView"
        app:layout_constraintStart_toStartOf="@id/pdfView"
        app:layout_constraintEnd_toEndOf="@id/pdfView" />
    
    <!-- Search navigation -->
    <LinearLayout
        android:id="@+id/searchNavigation"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:visibility="gone"
        app:layout_constraintBottom_toBottomOf="parent">
        
        <Button
            android:id="@+id/prevButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Previous" />
        
        <TextView
            android:id="@+id/resultCounter"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:gravity="center"
            android:text="0 of 0" />
        
        <Button
            android:id="@+id/nextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Next" />
    </LinearLayout>
</androidx.constraintlayout.widget.ConstraintLayout>
```

### Advanced: Using Low-Level Search API with PDFView

For more control, you can use the low-level search API alongside PDFView:

```java
// Get PdfiumCore and document from PDFView
PdfiumCore pdfiumCore = pdfView.getPdfiumCore();
PdfDocument document = pdfView.getPdfDocument();

// Use low-level search API
DocumentSearch docSearch = new DocumentSearch(pdfiumCore, document);
try {
    List<SearchResult> results = docSearch.searchAll("Android");
    
    // Process results and update PDFView manually if needed
    for (SearchResult result : results) {
        Log.d(TAG, "Found on page " + result.getPageIndex() 
            + " at position " + result.getCharIndex());
    }
} finally {
    docSearch.close();
}
```

See the sample application (`MainActivity.java` and `SearchExampleActivity.java`) for complete working examples with full UI integration.

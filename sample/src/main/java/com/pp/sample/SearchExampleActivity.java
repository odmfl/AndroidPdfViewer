package com.pp.sample;

import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.shockwave.pdfium.search.DocumentSearch;
import com.shockwave.pdfium.search.SearchResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Example activity demonstrating the text search API.
 * This is a simplified example showing how to use the search functionality.
 */
public class SearchExampleActivity extends AppCompatActivity {

    private static final String TAG = "SearchExampleActivity";

    private EditText searchInput;
    private Button searchButton;
    private Button prevButton;
    private Button nextButton;
    private TextView resultsText;
    private TextView statusText;
    private ProgressBar progressBar;

    private PdfiumCore pdfiumCore;
    private PdfDocument pdfDocument;
    private List<SearchResult> currentResults = new ArrayList<>();
    private int currentResultIndex = -1;
    
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Note: In a real app, you would use a proper layout XML file
        // This is a simplified example for demonstration purposes
        setupSimpleUI();
        
        // Initialize PdfiumCore
        pdfiumCore = new PdfiumCore(this);
        
        // Load a sample PDF from assets
        loadSamplePDF();
        
        setupListeners();
    }

    private void setupSimpleUI() {
        // In a real app, this would be done via XML layout
        // This is just for demonstration
        setTitle("Text Search Example");
        
        // You would normally inflate a proper layout here
        // For this example, we're just showing the logic
    }

    private void loadSamplePDF() {
        executorService.execute(() -> {
            try {
                // Copy sample PDF from assets to cache
                File cacheFile = new File(getCacheDir(), "sample.pdf");
                if (!cacheFile.exists()) {
                    try (InputStream input = getAssets().open("sample.pdf");
                         FileOutputStream output = new FileOutputStream(cacheFile)) {
                        byte[] buffer = new byte[4096];
                        int read;
                        while ((read = input.read(buffer)) != -1) {
                            output.write(buffer, 0, read);
                        }
                    }
                }
                
                // Open PDF document
                ParcelFileDescriptor fd = ParcelFileDescriptor.open(
                    cacheFile,
                    ParcelFileDescriptor.MODE_READ_ONLY
                );
                
                pdfDocument = pdfiumCore.newDocument(fd);
                
                runOnUiThread(() -> {
                    if (statusText != null) {
                        int pageCount = pdfiumCore.getPageCount(pdfDocument);
                        statusText.setText("PDF loaded: " + pageCount + " pages");
                    }
                    if (searchButton != null) {
                        searchButton.setEnabled(true);
                    }
                });
                
            } catch (IOException e) {
                Log.e(TAG, "Error loading PDF", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading PDF: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setupListeners() {
        if (searchButton != null) {
            searchButton.setOnClickListener(v -> performSearch());
        }
        
        if (nextButton != null) {
            nextButton.setOnClickListener(v -> navigateToResult(1));
        }
        
        if (prevButton != null) {
            prevButton.setOnClickListener(v -> navigateToResult(-1));
        }
    }

    private void performSearch() {
        if (searchInput == null) return;
        
        String searchTerm = searchInput.getText().toString().trim();
        
        if (TextUtils.isEmpty(searchTerm)) {
            Toast.makeText(this, "Please enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (pdfDocument == null) {
            Toast.makeText(this, "PDF not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show progress
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (searchButton != null) {
            searchButton.setEnabled(false);
        }
        
        // Perform search on background thread
        executorService.execute(() -> {
            try {
                DocumentSearch docSearch = new DocumentSearch(pdfiumCore, pdfDocument);
                try {
                    // Search entire document
                    List<SearchResult> results = docSearch.searchAll(searchTerm);
                    
                    runOnUiThread(() -> {
                        currentResults = results;
                        currentResultIndex = results.isEmpty() ? -1 : 0;
                        
                        displayResults();
                        updateNavigationButtons();
                        
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        if (searchButton != null) {
                            searchButton.setEnabled(true);
                        }
                    });
                } finally {
                    docSearch.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during search", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Search error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    if (searchButton != null) {
                        searchButton.setEnabled(true);
                    }
                });
            }
        });
    }

    private void displayResults() {
        if (resultsText == null) return;
        
        if (currentResults.isEmpty()) {
            resultsText.setText("No results found");
            return;
        }
        
        // Display results summary
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(currentResults.size()).append(" results\n\n");
        
        // Group by page
        int currentPage = -1;
        int pageCount = 0;
        
        for (int i = 0; i < Math.min(currentResults.size(), 50); i++) {
            SearchResult result = currentResults.get(i);
            
            if (result.getPageIndex() != currentPage) {
                if (currentPage != -1) {
                    sb.append("\n");
                }
                currentPage = result.getPageIndex();
                pageCount = 0;
                sb.append("Page ").append(currentPage + 1).append(":\n");
            }
            
            pageCount++;
            sb.append("  ").append(pageCount).append(". Position ")
              .append(result.getCharIndex()).append("\n");
        }
        
        if (currentResults.size() > 50) {
            sb.append("\n... and ").append(currentResults.size() - 50)
              .append(" more results");
        }
        
        resultsText.setText(sb.toString());
    }

    private void navigateToResult(int direction) {
        if (currentResults.isEmpty()) {
            return;
        }
        
        currentResultIndex += direction;
        
        // Wrap around
        if (currentResultIndex < 0) {
            currentResultIndex = currentResults.size() - 1;
        } else if (currentResultIndex >= currentResults.size()) {
            currentResultIndex = 0;
        }
        
        updateNavigationButtons();
        highlightCurrentResult();
    }

    private void updateNavigationButtons() {
        boolean hasResults = !currentResults.isEmpty();
        
        if (prevButton != null) {
            prevButton.setEnabled(hasResults);
        }
        if (nextButton != null) {
            nextButton.setEnabled(hasResults);
        }
        
        if (statusText != null && hasResults) {
            statusText.setText(String.format("Result %d of %d",
                currentResultIndex + 1, currentResults.size()));
        }
    }

    private void highlightCurrentResult() {
        if (currentResultIndex < 0 || currentResultIndex >= currentResults.size()) {
            return;
        }
        
        SearchResult result = currentResults.get(currentResultIndex);
        
        // Log the current result
        Log.d(TAG, "Highlighting result on page " + result.getPageIndex() +
                   " at position " + result.getCharIndex());
        
        // In a real implementation with PDFView, you would:
        // 1. Navigate to the page containing the result
        // 2. Draw a highlight rectangle at the result's position
        // 3. Scroll to make the result visible
        
        // Example (pseudo-code):
        // pdfView.jumpToPage(result.getPageIndex());
        // List<RectF> rects = result.getBoundingRects();
        // for (RectF rect : rects) {
        //     pdfView.addHighlight(rect);
        // }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up
        executorService.shutdown();
        
        if (pdfDocument != null) {
            pdfiumCore.closeDocument(pdfDocument);
        }
    }
}

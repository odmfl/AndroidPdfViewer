# PDF Coordinate Transformation Guide

This document explains how coordinate transformation works in AndroidPdfViewer, particularly for text search highlighting and text selection across different FitPolicy modes and zoom levels.

## Coordinate Systems

AndroidPdfViewer deals with three different coordinate systems:

### 1. **PDF Page Coordinates (Native/Original)**
- The coordinate system used by the native PDFium library
- Based on the original, unscaled page dimensions
- Origin at top-left of the page
- Units are in PDF points (1/72 inch)
- **Example**: A page might be 612 × 792 points (8.5" × 11" at 72 DPI)

### 2. **Scaled Page Coordinates**
- Coordinates after applying FitPolicy scaling
- Pages are scaled to fit the view based on FitPolicy (WIDTH, HEIGHT, or BOTH)
- Different pages may have different scale factors
- **Example**: Same page might be scaled to 800 × 1067 pixels to fit view width

### 3. **View Coordinates**
- Final screen coordinates after applying:
  - FitPolicy scaling
  - User zoom level
  - Page positioning (offsets and spacing)
  - Scroll offsets
- **Example**: After 2x zoom, the scaled page becomes 1600 × 2134 pixels

## FitPolicy Modes

The `FitPolicy` enum determines how pages are scaled to fit the view:

### FitPolicy.WIDTH
- Scales pages to fit the view width
- Height is calculated maintaining aspect ratio
- All pages use the same width ratio
- **Calculation**: `scale = viewWidth / originalPageWidth`

### FitPolicy.HEIGHT
- Scales pages to fit the view height
- Width is calculated maintaining aspect ratio
- All pages use the same height ratio
- **Calculation**: `scale = viewHeight / originalPageHeight`

### FitPolicy.BOTH
- Scales pages to fit both width and height constraints
- Uses the **smaller** of the two scale factors
- Ensures entire page fits in view
- **Calculation**: 
  ```java
  widthScale = viewWidth / originalPageWidth
  heightScale = viewHeight / originalPageHeight
  scale = min(widthScale, heightScale)
  ```

## Coordinate Transformation Pipeline

### For Search Highlighting and Text Selection

1. **Native PDFium Call** (PDF Page Coordinates)
   ```java
   // MUST use original page size
   Size originalSize = pdfFile.getOriginalPageSize(pageIndex);
   pdfiumCore.getTextRects(pagePtr, 0, 0, originalSize, rectList, ...);
   // Returns rectangles in original PDF coordinates
   ```

2. **Scale to Fit Policy** (Scaled Page Coordinates)
   ```java
   SizeF scaledSize = pdfFile.getPageSize(pageIndex);
   float scaleX = scaledSize.getWidth() / originalSize.getWidth();
   float scaleY = scaledSize.getHeight() / originalSize.getHeight();
   
   for (RectF rect : rectList) {
       rect.left *= scaleX;
       rect.top *= scaleY;
       rect.right *= scaleX;
       rect.bottom *= scaleY;
   }
   ```

3. **Apply Zoom and Positioning** (View Coordinates)
   ```java
   void sourceToViewRectFFSearch(RectF sRect, RectF vTarget, int page) {
       float zoom = getZoom();
       int pageX = getPageX(page);  // Accounts for horizontal centering
       int pageY = getPageY(page);  // Accounts for page offsets and spacing
       float offsetX = getCurrentXOffset();  // Scroll offset
       float offsetY = getCurrentYOffset();  // Scroll offset
       
       vTarget.set(
           sRect.left * zoom + pageX + offsetX,
           sRect.top * zoom + pageY + offsetY,
           sRect.right * zoom + pageX + offsetX,
           sRect.bottom * zoom + pageY + offsetY
       );
   }
   ```

## Implementation Details

### PageSizeCalculator

The `PageSizeCalculator` class handles FitPolicy calculations:

```java
public class PageSizeCalculator {
    private FitPolicy fitPolicy;
    private float widthRatio;   // Scale factor for WIDTH mode
    private float heightRatio;  // Scale factor for HEIGHT mode
    
    public SizeF calculate(Size pageSize) {
        switch (fitPolicy) {
            case WIDTH:
                return fitWidth(pageSize, maxWidth);
            case HEIGHT:
                return fitHeight(pageSize, maxHeight);
            case BOTH:
                return fitBoth(pageSize, maxWidth, maxHeight);
        }
    }
    
    private SizeF fitBoth(Size pageSize, float maxWidth, float maxHeight) {
        float w = pageSize.getWidth(), h = pageSize.getHeight();
        float ratio = w / h;
        w = maxWidth;
        h = maxWidth / ratio;
        if (h > maxHeight) {
            h = maxHeight;
            w = maxHeight * ratio;
        }
        return new SizeF(w, h);
    }
}
```

### PdfFile Methods

Key methods for coordinate transformation:

```java
public class PdfFile {
    // Returns original page size (for native calls)
    public Size getOriginalPageSize(int pageIndex);
    
    // Returns scaled page size (after FitPolicy)
    public SizeF getPageSize(int pageIndex);
    
    // Returns scaled page size with zoom applied
    public SizeF getScaledPageSize(int pageIndex, float zoom);
    
    // Primary offset (Y for vertical, X for horizontal scroll)
    public float getPageOffset(int pageIndex, float zoom);
    
    // Secondary offset (X for vertical, Y for horizontal scroll)
    // Handles page centering
    public float getSecondaryPageOffset(int pageIndex, float zoom);
}
```

## Common Issues and Solutions

### Issue 1: Highlights Don't Match Text with FitPolicy.BOTH

**Problem**: Text highlights appear offset from actual text when using FitPolicy.BOTH or FitPolicy.HEIGHT.

**Cause**: Passing scaled page size to native PDFium methods instead of original page size.

**Solution**: Always use `getOriginalPageSize()` for native calls, then scale the results:
```java
Size originalSize = pdfFile.getOriginalPageSize(pageIndex);
pdfiumCore.nativeGetRect(pagePtr, 0, 0, 
    originalSize.getWidth(), originalSize.getHeight(), ...);

// Then scale results
SizeF scaledSize = pdfFile.getPageSize(pageIndex);
float scaleX = scaledSize.getWidth() / originalSize.getWidth();
float scaleY = scaledSize.getHeight() / originalSize.getHeight();
rect.left *= scaleX;
rect.top *= scaleY;
rect.right *= scaleX;
rect.bottom *= scaleY;
```

### Issue 2: Highlights Don't Update on Zoom

**Problem**: Highlights remain in wrong position when user zooms in/out.

**Cause**: Highlights are drawn once and not recalculated on zoom changes.

**Solution**: Invalidate the selection view when zoom changes:
```java
public void zoomCenteredTo(float zoom, PointF pivot) {
    float dZoom = zoom / this.zoom;
    zoomTo(zoom);
    // ... position calculations ...
    moveTo(baseX, baseY);
    
    // Trigger redraw of highlights
    if (selectionPaintView != null) {
        selectionPaintView.invalidate();
    }
}
```

The `sourceToViewRectFFSearch()` method automatically accounts for current zoom level, so highlights will be positioned correctly when redrawn.

### Issue 3: Highlights Offset with Page Spacing

**Problem**: Highlights don't align when autoSpacing or manual spacing is enabled.

**Cause**: Page offsets not correctly calculated.

**Solution**: The `getPageY()` and `getPageX()` methods in PDFView automatically account for spacing through `PdfFile.getPageOffset()` which includes spacing calculations. Ensure you're using these methods rather than calculating offsets manually.

## Zoom Behavior

When user zooms:

1. **Current zoom level changes**: `PDFView.zoom` is updated
2. **Page offsets are recalculated**: `moveTo()` adjusts view position
3. **Highlights are redrawn**: `selectionPaintView.invalidate()` triggers redraw
4. **Coordinate transformation applies new zoom**: `sourceToViewRectFFSearch()` uses new zoom

The zoom is applied **after** FitPolicy scaling:
```
Final Scale = FitPolicy Scale × User Zoom
```

For example, with FitPolicy.BOTH:
- Page originally 600 × 800 points
- FitPolicy scales to 400 × 533 pixels (0.67× scale)
- User zooms to 2.0×
- Final size: 800 × 1066 pixels (0.67 × 2.0 = 1.33× overall)

## Page Positioning

Pages are positioned considering:

### Primary Direction (Scroll Direction)
- **Vertical scrolling**: Pages stacked vertically with Y offsets
- **Horizontal scrolling**: Pages arranged horizontally with X offsets
- Calculated by `PdfFile.getPageOffset()`

### Secondary Direction (Centering)
- **Vertical scrolling**: Pages centered horizontally if narrower than max width
- **Horizontal scrolling**: Pages centered vertically if shorter than max height
- Calculated by `PdfFile.getSecondaryPageOffset()`

### Spacing
- Fixed spacing: `spacingPx` between all pages
- Auto spacing: Calculated to center each page in view
- Top/bottom spacing: Additional padding for first/last pages

## Testing Different Configurations

To test coordinate transformation:

```java
pdfView.fromAsset("test.pdf")
    .pageFitPolicy(FitPolicy.BOTH)  // Test BOTH, WIDTH, HEIGHT
    .swipeHorizontal(true)           // Test horizontal scrolling
    .pageSnap(true)                  // Test with page snapping
    .autoSpacing(true)               // Test with auto spacing
    .pageFling(true)                 // Test with fling animation
    .spacing(10)                     // Test with manual spacing
    .load();

// Search and verify highlights align
pdfView.search("test");

// Zoom and verify highlights update correctly
pdfView.zoomWithAnimation(2.0f);
```

## Coordinate Scaling Implementation

### CoordinateScaler Utility

The `CoordinateScaler` class provides a clean API for transforming coordinates from original PDF space to FitPolicy-scaled space:

```java
CoordinateScaler scaler = new CoordinateScaler(pdfView);

// Scale a rectangle in place
RectF rect = new RectF(...);  // Rectangle in original PDF coordinates
scaler.scaleRect(pageIndex, rect);  // Now in scaled coordinates

// Or create a scaled copy
RectF originalRect = new RectF(...);
RectF scaledRect = scaler.scaleRectCopy(pageIndex, originalRect);

// Check if scaling is needed
if (scaler.needsScaling(pageIndex)) {
    // Handle differently scaled pages
}

// Get scale factors
float[] scales = scaler.getScaleFactors(pageIndex);
float scaleX = scales[0];
float scaleY = scales[1];
```

### Verified Coordinate Paths

All code paths that retrieve text rectangles from PDFium have been verified to properly scale coordinates:

1. **Search Highlighting** (`PDFView.getRectForRecordItem()`)
   - Uses `pdfFile.getOriginalPageSize()` for native call
   - Scales results to FitPolicy coordinates
   - Stores scaled rectangles in `SearchRecordItem.rectFS`

2. **Persistent Highlights** (`PDocSelection.loadHighlightsForPage()`)
   - Uses `pdfFile.getOriginalPageSize()` for native call
   - Scales results to FitPolicy coordinates
   - Stores scaled rectangles for rendering

3. **Text Selection** (`DragPinchManager.getSelRects()`)
   - Uses `pdfFile.getOriginalPageSize()` for native call
   - Scales results to FitPolicy coordinates
   - Returns scaled rectangles for selection handles

All three paths follow the same pattern:
```java
// 1. Get original size for native call
Size originalSize = pdfFile.getOriginalPageSize(pageIndex);

// 2. Call PDFium with original dimensions
pdfiumCore.getTextRects(..., originalSize, ...);

// 3. Scale results to FitPolicy coordinates
SizeF scaledSize = pdfFile.getPageSize(pageIndex);
float scaleX = scaledSize.getWidth() / originalSize.getWidth();
float scaleY = scaledSize.getHeight() / originalSize.getHeight();
for (RectF rect : rects) {
    rect.left *= scaleX;
    rect.top *= scaleY;
    rect.right *= scaleX;
    rect.bottom *= scaleY;
}
```

## References

### Key Classes
- `PDFView.java` - Main view and coordinate transformation entry points
- `PdfFile.java` - Page sizing and offset calculations
- `PageSizeCalculator.java` - FitPolicy calculations
- `PDocSelection.java` - Highlight rendering
- `DragPinchManager.java` - Text selection
- `CoordinateTransformer.java` - Utility for coordinate transformation (scaled → view)
- `CoordinateScaler.java` - Utility for coordinate scaling (original → scaled)

### Key Methods
- `PDFView.sourceToViewRectFFSearch()` - Transform search rectangles to view coordinates
- `PDFView.getRectForRecordItem()` - Get and scale search result rectangles
- `PdfFile.getOriginalPageSize()` - Get unscaled page dimensions
- `PdfFile.getPageSize()` - Get FitPolicy-scaled page dimensions
- `PdfFile.getPageOffset()` - Get page position in scroll direction
- `PdfFile.getSecondaryPageOffset()` - Get page centering offset
- `CoordinateScaler.scaleRect()` - Scale rectangle from original to scaled coordinates

### Native Methods
- `PdfiumCore.getTextRects()` - Get text selection rectangles
- `PdfiumCore.nativeGetRect()` - Get individual search result rectangle
- `PdfiumCore.nativeCountRects()` - Count rectangles for text range

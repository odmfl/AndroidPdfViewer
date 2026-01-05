/**
 * Copyright 2016 Bartosz Schiller
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.barteksc.pdfviewer.util;

import android.graphics.RectF;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.PdfFile;
import com.shockwave.pdfium.util.Size;
import com.shockwave.pdfium.util.SizeF;

/**
 * Utility class for coordinate transformation between different coordinate systems.
 * 
 * PDF Viewer uses three coordinate systems:
 * 1. PDF Page Coordinates - Original coordinates from PDFium (unscaled)
 * 2. Scaled Page Coordinates - After applying FitPolicy scaling
 * 3. View Coordinates - After applying zoom, positioning, and scroll offsets
 * 
 * This class handles transformations between these coordinate systems.
 */
public class CoordinateScaler {
    
    private final PDFView pdfView;
    private final PdfFile pdfFile;
    
    /**
     * Creates a CoordinateScaler for the given PDFView.
     * 
     * @param pdfView The PDF viewer instance
     */
    public CoordinateScaler(PDFView pdfView) {
        this.pdfView = pdfView;
        this.pdfFile = pdfView.getPdfFile();
    }
    
    /**
     * Scale a rectangle from original PDF coordinates to FitPolicy-scaled coordinates.
     * This modifies the rectangle in place.
     * 
     * The scaling accounts for FitPolicy (WIDTH, HEIGHT, or BOTH) which determines
     * how pages are scaled to fit the view. Different pages may have different scale
     * factors depending on their aspect ratios and the current FitPolicy.
     * 
     * @param pageIndex The page index (0-based)
     * @param rect The rectangle in original PDF coordinates (will be modified)
     */
    public void scaleRect(int pageIndex, RectF rect) {
        if (pdfFile == null || rect == null) {
            return;
        }
        
        // Get original page size (as PDFium knows it)
        Size originalSize = pdfFile.getOriginalPageSize(pageIndex);
        if (originalSize == null || originalSize.isEmpty()) {
            return;
        }
        
        // Get FitPolicy-scaled page size (as view displays it)
        SizeF scaledSize = pdfFile.getPageSize(pageIndex);
        if (scaledSize == null || scaledSize.getWidth() == 0 || scaledSize.getHeight() == 0) {
            return;
        }
        
        // Calculate scale factors
        float scaleX = scaledSize.getWidth() / originalSize.getWidth();
        float scaleY = scaledSize.getHeight() / originalSize.getHeight();
        
        // Apply scaling
        rect.left *= scaleX;
        rect.top *= scaleY;
        rect.right *= scaleX;
        rect.bottom *= scaleY;
    }
    
    /**
     * Scale a new copy of a rectangle from original PDF coordinates to FitPolicy-scaled coordinates.
     * This creates a new rectangle and leaves the original unchanged.
     * 
     * @param pageIndex The page index (0-based)
     * @param rect The rectangle in original PDF coordinates (will NOT be modified)
     * @return A new rectangle in scaled coordinates, or a copy of the original if scaling fails
     */
    public RectF scaleRectCopy(int pageIndex, RectF rect) {
        if (rect == null) {
            return new RectF();
        }
        
        RectF scaledRect = new RectF(rect);
        scaleRect(pageIndex, scaledRect);
        return scaledRect;
    }
    
    /**
     * Check if a rectangle needs scaling from original to scaled coordinates.
     * This is useful for determining if coordinates are in the original or scaled space.
     * 
     * @param pageIndex The page index (0-based)
     * @return true if scaling is needed (scale factors are not 1.0), false otherwise
     */
    public boolean needsScaling(int pageIndex) {
        if (pdfFile == null) {
            return false;
        }
        
        Size originalSize = pdfFile.getOriginalPageSize(pageIndex);
        if (originalSize == null || originalSize.isEmpty()) {
            return false;
        }
        
        SizeF scaledSize = pdfFile.getPageSize(pageIndex);
        if (scaledSize == null || scaledSize.getWidth() == 0 || scaledSize.getHeight() == 0) {
            return false;
        }
        
        float scaleX = scaledSize.getWidth() / originalSize.getWidth();
        float scaleY = scaledSize.getHeight() / originalSize.getHeight();
        
        // Check if scale factors are approximately 1.0 (allowing for floating point errors)
        return Math.abs(scaleX - 1.0f) > 0.001f || Math.abs(scaleY - 1.0f) > 0.001f;
    }
    
    /**
     * Get the scale factors for a given page.
     * 
     * @param pageIndex The page index (0-based)
     * @return A two-element array [scaleX, scaleY], or [1.0, 1.0] if calculation fails
     */
    public float[] getScaleFactors(int pageIndex) {
        float[] scales = new float[]{1.0f, 1.0f};
        
        if (pdfFile == null) {
            return scales;
        }
        
        Size originalSize = pdfFile.getOriginalPageSize(pageIndex);
        if (originalSize == null || originalSize.isEmpty()) {
            return scales;
        }
        
        SizeF scaledSize = pdfFile.getPageSize(pageIndex);
        if (scaledSize == null || scaledSize.getWidth() == 0 || scaledSize.getHeight() == 0) {
            return scales;
        }
        
        scales[0] = scaledSize.getWidth() / originalSize.getWidth();
        scales[1] = scaledSize.getHeight() / originalSize.getHeight();
        
        return scales;
    }
}

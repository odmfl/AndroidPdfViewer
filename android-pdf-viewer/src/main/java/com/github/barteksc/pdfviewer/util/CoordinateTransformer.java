/**
 * Copyright 2017 Bartosz Schiller
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

import androidx.annotation.NonNull;

import com.github.barteksc.pdfviewer.PDFView;
import com.shockwave.pdfium.util.SizeF;

/**
 * CoordinateTransformer handles transformation of PDF page coordinates to view coordinates.
 * It accounts for:
 * - FitPolicy mode (WIDTH, HEIGHT, BOTH)
 * - Current zoom level
 * - Page position and spacing
 * - Scroll offsets
 * - Horizontal/vertical scrolling mode
 */
public class CoordinateTransformer {
    
    private final PDFView pdfView;
    
    public CoordinateTransformer(PDFView pdfView) {
        this.pdfView = pdfView;
    }
    
    /**
     * Transform page coordinates to view coordinates accounting for all factors:
     * FitPolicy, zoom, page position, spacing, and scroll offsets.
     * 
     * @param pageIndex The page index
     * @param pageRect The rectangle in page coordinates
     * @param outViewRect Output rectangle in view coordinates
     */
    public void pageToViewRect(int pageIndex, @NonNull RectF pageRect, @NonNull RectF outViewRect) {
        // Check if PDF is loaded
        SizeF pageSize = pdfView.getPageSize(pageIndex);
        if (pageSize == null || (pageSize.getWidth() == 0 && pageSize.getHeight() == 0)) {
            outViewRect.set(pageRect);
            return;
        }
        
        // Get current zoom level
        float zoom = pdfView.getZoom();
        
        // Get page position in the document
        int pageX = pdfView.getPageX(pageIndex);
        int pageY = pdfView.getPageY(pageIndex);
        
        // Get current scroll offsets
        float offsetX = pdfView.getCurrentXOffset();
        float offsetY = pdfView.getCurrentYOffset();
        
        // Transform the rectangle from page coordinates to view coordinates
        // The page coordinates are already in the scaled coordinate system based on FitPolicy,
        // so we just need to apply zoom and add offsets
        outViewRect.set(
            pageRect.left * zoom + pageX + offsetX,
            pageRect.top * zoom + pageY + offsetY,
            pageRect.right * zoom + pageX + offsetX,
            pageRect.bottom * zoom + pageY + offsetY
        );
    }
}

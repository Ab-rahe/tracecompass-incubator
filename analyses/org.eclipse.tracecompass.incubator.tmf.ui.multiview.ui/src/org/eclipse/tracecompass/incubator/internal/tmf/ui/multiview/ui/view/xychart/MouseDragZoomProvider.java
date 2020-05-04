/**********************************************************************
 * Copyright (c) 2020 Draeger, Auriga
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.xychart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.ITmfChartTimeProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.TmfBaseProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.TmfMouseDragZoomProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.TmfXYChartViewer;
import org.swtchart.IAxis;
import org.swtchart.ICustomPaintListener;
import org.swtchart.IPlotArea;

/**
 * Enables context menu if no dragging was detected. Is a copy of
 * {@link TmfMouseDragZoomProvider}. The difference from the original is that the
 * mouseUp method adds the possiblity to show the menu when right-clicking in
 * the viewer.
 *
 * @author Ivan Grinenko
 *
 */
public class MouseDragZoomProvider extends TmfBaseProvider
        implements MouseListener, MouseMoveListener, ICustomPaintListener {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------
    /** Cached start time */
    private long fStartTime;
    /** Cached end time */
    private long fEndTime;
    /** Flag indicating that an update is ongoing */
    private boolean fIsUpdate;
    private ActionsChartMultiViewer fChartViewer;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------
    /**
     * Default constructor
     *
     * @param tmfChartViewer
     *            the chart viewer reference.
     */
    public MouseDragZoomProvider(ActionsChartMultiViewer tmfChartViewer) {
        super(tmfChartViewer.getChartViewer());
        fChartViewer = tmfChartViewer;
        register();
    }

    // ------------------------------------------------------------------------
    // TmfBaseProvider
    // ------------------------------------------------------------------------
    @Override
    public void register() {
        getChart().getPlotArea().addMouseListener(this);
        getChart().getPlotArea().addMouseMoveListener(this);
        ((IPlotArea) getChart().getPlotArea()).addCustomPaintListener(this);
    }

    @Override
    public void deregister() {
        if ((getChartViewer().getControl() != null) && !getChartViewer().getControl().isDisposed()) {
            getChart().getPlotArea().removeMouseListener(this);
            getChart().getPlotArea().removeMouseMoveListener(this);
            ((IPlotArea) getChart().getPlotArea()).removeCustomPaintListener(this);
        }
    }

    @Override
    public void refresh() {
        // nothing to do
    }

    // ------------------------------------------------------------------------
    // MouseListener
    // ------------------------------------------------------------------------
    @Override
    public void mouseDoubleClick(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseDown(MouseEvent e) {
        if ((getChartViewer().getWindowDuration() != 0) && (e.button == 3)) {
            IAxis xAxis = getChart().getAxisSet().getXAxis(0);
            fStartTime = limitXDataCoordinate(xAxis.getDataCoordinate(e.x));
            fEndTime = fStartTime;
            fIsUpdate = true;
        }
    }

    @Override
    public void mouseUp(MouseEvent e) {
        if ((fIsUpdate) && (fStartTime != fEndTime)) {
            if (fStartTime > fEndTime) {
                long tmp = fStartTime;
                fStartTime = fEndTime;
                fEndTime = tmp;
            }
            ITmfChartTimeProvider viewer = getChartViewer();
            viewer.updateWindow(fStartTime + viewer.getTimeOffset(), fEndTime + viewer.getTimeOffset());
        } else {
            if (e.button == 3) {
                fChartViewer.showMenu();
            }
        }

        if (fIsUpdate) {
            getChart().redraw();
        }
        fIsUpdate = false;
    }

    // ------------------------------------------------------------------------
    // MouseMoveListener
    // ------------------------------------------------------------------------
    @Override
    public void mouseMove(MouseEvent e) {
        if (fIsUpdate) {
            IAxis xAxis = getChart().getAxisSet().getXAxis(0);
            fEndTime = limitXDataCoordinate(xAxis.getDataCoordinate(e.x));

            ITmfChartTimeProvider viewer = getChartViewer();
            if (viewer instanceof TmfXYChartViewer) {
                TmfXYChartViewer xyChartViewer = (TmfXYChartViewer) viewer;
                xyChartViewer.updateStatusLine(fStartTime, fEndTime, limitXDataCoordinate(xAxis.getDataCoordinate(e.x)));
            }

            getChart().redraw();
        }
    }

    // ------------------------------------------------------------------------
    // ICustomPaintListener
    // ------------------------------------------------------------------------
    @Override
    public void paintControl(PaintEvent e) {
        if (fIsUpdate && (fStartTime != fEndTime)) {
            IAxis xAxis = getChart().getAxisSet().getXAxis(0);
            int startX = xAxis.getPixelCoordinate(fStartTime);
            int endX = xAxis.getPixelCoordinate(fEndTime);

            e.gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND));
            if (fStartTime < fEndTime) {
                e.gc.fillRectangle(startX, 0, endX - startX, e.height);
            } else {
                e.gc.fillRectangle(endX, 0, startX - endX, e.height);
            }
            e.gc.drawLine(startX, 0, startX, e.height);
            e.gc.drawLine(endX, 0, endX, e.height);
        }
    }

    @Override
    public boolean drawBehindSeries() {
        return true;
    }

    /**
     * Returns the current or default display.
     *
     * @return the current or default display
     */
    protected static Display getDisplay() {
        Display display = Display.getCurrent();
        // may be null if outside the UI thread
        if (display == null) {
            display = Display.getDefault();
        }
        return display;
    }

}

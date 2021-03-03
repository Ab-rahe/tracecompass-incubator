/*******************************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.kernel.core.tests.swslatencytest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import java.io.File;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.kernel.core.swslatency.SWSLatencyAnalysis;
import org.eclipse.tracecompass.incubator.kernel.core.tests.ActivatorTest;
import org.eclipse.tracecompass.analysis.os.linux.core.tests.stubs.trace.TmfXmlKernelTraceStub;
import org.eclipse.tracecompass.analysis.os.linux.core.tid.TidAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for the {@link SWSLatencyAnalysis} class
 *
 * @author Abdellah Rahmani
 */
@SuppressWarnings("restriction")
public class SWSLatencyTest {

    private static final String SWS_USAGE_FILE = "testfiles/sws_analysis.xml";
    private IKernelTrace fTrace;
    private SWSLatencyAnalysis fModule;

    private static void deleteSuppFiles(ITmfTrace trace) {
        /* Remove supplementary files */
        if (trace != null) {
            File suppDir = new File(TmfTraceManager.getSupplementaryFileDir(trace));
            for (File file : suppDir.listFiles()) {
                file.delete();
            }

        }
    }

    /**
     * Setup the trace for the tests
     */
    @Before
    public void setUp() {
        IKernelTrace trace = new TmfXmlKernelTraceStub();
        IPath filePath = ActivatorTest.getAbsoluteFilePath(SWS_USAGE_FILE);
        IStatus status = trace.validate(null, filePath.toOSString());
        if (!status.isOK()) {
            fail(status.getException().getMessage());
        }
        try {
            trace.initTrace(null, filePath.toOSString(), TmfEvent.class);
        } catch (TmfTraceException e) {
            fail(e.getMessage());
        }
        deleteSuppFiles(trace);
        ((TmfTrace) trace).traceOpened(new TmfTraceOpenedSignal(this, trace, null));
        /*
         * FIXME: Make sure this analysis is finished before running the
         * SWSLtencyAnalysis This block can be removed once analysis dependency
         * and request precedence is implemented
         */
        IAnalysisModule module = null;
        for (IAnalysisModule mod : TmfTraceUtils.getAnalysisModulesOfClass(trace, TidAnalysisModule.class)) {
            module = mod;
        }
        assertNotNull(module);
        module.schedule();
        module.waitForCompletion();
        /* End of the FIXME block */

        fModule = TmfTraceUtils.getAnalysisModuleOfClass(trace, SWSLatencyAnalysis.class, SWSLatencyAnalysis.ID);
        assertNotNull(fModule);
        fModule.schedule();
        fModule.waitForCompletion();
        fTrace = trace;
    }

    /**
     * Dispose everything
     */
    @After
    public void cleanup() {
        final ITmfTrace testTrace = fTrace;
        if (testTrace != null) {
            testTrace.dispose();
        }
    }

    /**
     * This will load the analysis and test it. as it depends on Kernel, this
     * test runs the kernel trace first then the analysis
     */

    @SuppressWarnings("null")
    @Test
    public void testSmallTraceSequential() {
        final SWSLatencyAnalysis swsModule = fModule;
        assertNotNull(swsModule);
        ISegmentStore<@NonNull ISegment> segmentStore = swsModule.getSegmentStore();
        assertNotNull(segmentStore);
        assertEquals(false, segmentStore.isEmpty());
        assertEquals(5, segmentStore.size());
        assertEquals(1, segmentStore.getIntersectingElements(1).iterator().next().getLength());
        assertEquals(2, segmentStore.getIntersectingElements(5).iterator().next().getLength());
        assertEquals(4, segmentStore.getIntersectingElements(10).iterator().next().getLength());
        assertEquals(5, segmentStore.getIntersectingElements(15).iterator().next().getLength());
        assertEquals(22, segmentStore.getIntersectingElements(25).iterator().next().getLength());
    }
}

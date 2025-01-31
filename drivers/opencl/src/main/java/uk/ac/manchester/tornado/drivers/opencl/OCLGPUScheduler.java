/*
 * This file is part of Tornado: A heterogeneous programming framework: 
 * https://github.com/beehive-lab/tornado
 *
 * Copyright (c) 2013-2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Authors: James Clarkson
 *
 */
package uk.ac.manchester.tornado.drivers.opencl;

import uk.ac.manchester.tornado.runtime.tasks.meta.TaskMetaData;

public class OCLGPUScheduler extends OCLKernelScheduler {

    public static final double GPU_COMPUTE_UNIT_COEFF = 1;
    public static final double GPU_COMPUTE_UNIT_QUEUE_COEFF = 128;
    public static final double GPU_WORK_GROUP_COEFF = .125;

    @SuppressWarnings("unused") private long maxComputeUnits;
    @SuppressWarnings("unused") private double workGroupUtil;
    @SuppressWarnings("unused") private long maxWorkGroupSize;

    private static final int WARP_SIZE = 32;
    private boolean ADJUST_IRREGULAR = false;

    private final long[] maxWorkItemSizes;

    public OCLGPUScheduler(final OCLDeviceContext context) {
        super(context);
        OCLDevice device = context.getDevice();

        maxWorkItemSizes = device.getDeviceMaxWorkItemSizes();
        maxComputeUnits = device.getDeviceMaxComputeUnits();
        maxWorkGroupSize = device.getDeviceMaxWorkGroupSize();

        workGroupUtil = GPU_WORK_GROUP_COEFF;
    }

    @Override
    public void calculateGlobalWork(final TaskMetaData meta, long batchThreads) {
        final long[] globalWork = meta.getGlobalWork();

        for (int i = 0; i < meta.getDims(); i++) {
            long value = (batchThreads <= 0) ? (long) (meta.getDomain().get(i).cardinality()) : batchThreads;
            // adjust for irregular problem sizes
            if (ADJUST_IRREGULAR) {
                if (value % WARP_SIZE != 0) {
                    value = ((value / WARP_SIZE) + 1) * WARP_SIZE;
                }
            }
            globalWork[i] = value;
        }
    }

    @Override
    public void calculateLocalWork(final TaskMetaData meta) {
        final long[] localWork = meta.getLocalWork();
        switch (meta.getDims()) {
            case 3:
                /// XXX: Support 3D
                localWork[2] = calculateGroupSize(maxWorkItemSizes[2], meta.getOpenCLGpuBlock2DY(), meta.getGlobalWork()[2]);
            case 2:
                localWork[1] = calculateGroupSize(maxWorkItemSizes[1], meta.getOpenCLGpuBlock2DY(), meta.getGlobalWork()[1]);
                localWork[0] = calculateGroupSize(maxWorkItemSizes[0], meta.getOpenCLGpuBlock2DX(), meta.getGlobalWork()[0]);
                break;
            case 1:
                localWork[0] = calculateGroupSize(maxWorkItemSizes[0], meta.getOpenCLGpuBlockX(), meta.getGlobalWork()[0]);
                break;
            default:
                break;
        }
    }

    private int calculateGroupSize(long maxBlockSize, long customBlockSize, long globalWorkSize) {
        if (maxBlockSize == globalWorkSize) {
            maxBlockSize /= 4;
        }

        int value = (int) Math.min(Math.max(maxBlockSize, customBlockSize), globalWorkSize);
        while (globalWorkSize % value != 0) {
            value--;
        }
        return value;
    }

}

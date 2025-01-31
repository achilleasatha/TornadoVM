/*
 * Copyright (c) 2018, 2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
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
package uk.ac.manchester.tornado.drivers.opencl.graal;

import static uk.ac.manchester.tornado.api.exceptions.TornadoInternalError.guarantee;
import static uk.ac.manchester.tornado.runtime.common.Tornado.DEBUG;
import static uk.ac.manchester.tornado.runtime.common.Tornado.debug;
import static uk.ac.manchester.tornado.runtime.common.Tornado.info;

import java.nio.ByteBuffer;

import jdk.vm.ci.code.InstalledCode;
import jdk.vm.ci.code.InvalidInstalledCodeException;
import uk.ac.manchester.tornado.api.common.Event;
import uk.ac.manchester.tornado.api.exceptions.TornadoRuntimeException;
import uk.ac.manchester.tornado.drivers.opencl.OCLDeviceContext;
import uk.ac.manchester.tornado.drivers.opencl.OCLGPUScheduler;
import uk.ac.manchester.tornado.drivers.opencl.OCLKernel;
import uk.ac.manchester.tornado.drivers.opencl.OCLKernelScheduler;
import uk.ac.manchester.tornado.drivers.opencl.OCLProgram;
import uk.ac.manchester.tornado.drivers.opencl.OCLScheduler;
import uk.ac.manchester.tornado.drivers.opencl.mm.OCLByteBuffer;
import uk.ac.manchester.tornado.drivers.opencl.mm.OCLCallStack;
import uk.ac.manchester.tornado.drivers.opencl.runtime.OCLTornadoDevice;
import uk.ac.manchester.tornado.runtime.common.CallStack;
import uk.ac.manchester.tornado.runtime.common.RuntimeUtilities;
import uk.ac.manchester.tornado.runtime.common.TornadoInstalledCode;
import uk.ac.manchester.tornado.runtime.tasks.meta.TaskMetaData;

public class OCLInstalledCode extends InstalledCode implements TornadoInstalledCode {

    private final OCLKernelScheduler DEFAULT_SCHEDULER;

    private final ByteBuffer buffer = ByteBuffer.allocate(8);
    private final byte[] code;
    private final OCLDeviceContext deviceContext;
    private final OCLKernel kernel;
    private boolean valid;

    private final OCLKernelScheduler scheduler;
    private final int[] internalEvents = new int[1];

    private final long[] singleThreadGlobalWorkSize = new long[] { 1 };
    private final long[] singleThreadLocalWorkSize = new long[] { 1 };

    public OCLInstalledCode(final String entryPoint, final byte[] code, final OCLDeviceContext deviceContext, final OCLProgram program, final OCLKernel kernel) {
        super(entryPoint);
        this.code = code;
        this.deviceContext = deviceContext;
        this.scheduler = OCLScheduler.create(deviceContext);
        this.DEFAULT_SCHEDULER = new OCLGPUScheduler(deviceContext);
        this.kernel = kernel;
        valid = kernel != null;
        buffer.order(deviceContext.getByteOrder());
    }

    @Override
    public void invalidate() {
        if (valid) {
            kernel.cleanup();
            valid = false;
        }
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    public OCLKernel getKernel() {
        return kernel;
    }

    /**
     * It executes a kernel with 1 thread (the equivalent of calling
     * clEnqueueTask.
     * 
     * @param stack
     *            {@link OCLByteBuffer} stack
     * @param meta
     *            {@link TaskMetaData} netadata
     * @return int with the event ID.
     */
    public int executeTask(final OCLByteBuffer stack, final TaskMetaData meta) {
        debug("kernel submitted: id=0x%x, method = %s, device =%s", kernel.getId(), kernel.getName(), deviceContext.getDevice().getDeviceName());
        debug("\tstack    : buffer id=0x%x, address=0x%x relative=0x%x", stack.toBuffer(), stack.toAbsoluteAddress(), stack.toRelativeAddress());

        setKernelArgs(stack, meta);
        stack.write();

        int task;
        if (meta == null) {
            task = deviceContext.enqueueNDRangeKernel(kernel, 1, null, singleThreadGlobalWorkSize, singleThreadLocalWorkSize, internalEvents);
            deviceContext.flush();
            deviceContext.finish();
        } else {
            if (meta != null && meta.isParallel()) {
                if (meta.enableThreadCoarsener()) {
                    task = DEFAULT_SCHEDULER.submit(kernel, meta, null, 0);
                } else {
                    task = scheduler.submit(kernel, meta, null, 0);
                }
            } else {
                task = deviceContext.enqueueNDRangeKernel(kernel, 1, null, singleThreadGlobalWorkSize, singleThreadLocalWorkSize, null);
            }
        }
        return task;
    }

    /**
     * stack needs to be read so that the return value is transfered back to the
     * host.- As this is blocking then no clFinish() is needed
     * 
     * @param stack
     * @param meta
     * @param task
     */
    public void readValue(final OCLByteBuffer stack, final TaskMetaData meta, int task) {
        stack.read();
    }

    public void resolveEvent(final OCLByteBuffer stack, final TaskMetaData meta, int task) {
        Event event = deviceContext.resolveEvent(task);
        debug("kernel completed: id=0x%x, method = %s, device = %s", kernel.getId(), kernel.getName(), deviceContext.getDevice().getDeviceName());
        if (event != null) {
            debug("\tstatus   : %s", event.getStatus());

            if (meta != null && meta.enableProfiling()) {
                debug("\texecuting: %f seconds", event.getExecutionTimeInSeconds());
                debug("\ttotal    : %f seconds", event.getTotalTimeInSeconds());
            }
        }
    }

    @Override
    public Object executeVarargs(final Object... args) throws InvalidInstalledCodeException {
        return null;
    }

    @Override
    public byte[] getCode() {
        return code;
    }

    public String getGeneratedSourceCode() {
        return new String(code);
    }

    /**
     * Set arguments into the OpenCL device Kernel.
     * 
     * @param stack
     *            OpenCL stack parameters {@link OCLByteBuffer}
     * @param meta
     *            task metadata {@link TaskMetaData}
     */
    private void setKernelArgs(final OCLByteBuffer stack, TaskMetaData meta) {
        int index = 0;

        if (deviceContext.needsBump()) {
            buffer.clear();
            buffer.putLong(deviceContext.getBumpBuffer());
            kernel.setArg(index, buffer);
            index++;
        }

        // heap (global memory)
        buffer.clear();
        buffer.putLong(stack.toBuffer());
        kernel.setArg(index, buffer);
        index++;

        // stack pointer
        buffer.clear();
        buffer.putLong(stack.toRelativeAddress());
        kernel.setArg(index, buffer);
        index++;

        // constant memory
        if (meta != null && meta.getConstantSize() > 0) {
            kernel.setArg(index, ByteBuffer.wrap(meta.getConstantData()));
        } else {
            kernel.setArgUnused(index);
        }
        index++;

        // local memory
        if (meta != null && meta.getLocalSize() > 0) {
            info("\tallocating %s of local memory", RuntimeUtilities.humanReadableByteCount(meta.getLocalSize(), true));
            kernel.setLocalRegion(index, meta.getLocalSize());
        } else {
            kernel.setArgUnused(index);
        }
        index++;

        // private memory
        kernel.setArgUnused(index);
    }

    public int submitWithEvents(final OCLCallStack stack, final TaskMetaData meta, final int[] events, long batchThreads) {
        guarantee(kernel != null, "kernel is null");

        if (DEBUG) {
            info("kernel submitted: id=0x%x, method = %s, device =%s", kernel.getId(), kernel.getName(), deviceContext.getDevice().getDeviceName());
            info("\tstack    : buffer id=0x%x, device=0x%x (0x%x)", stack.toBuffer(), stack.toAbsoluteAddress(), stack.toRelativeAddress());
        }

        /*
         * Only set the kernel arguments if they are either: - not set or - have
         * changed
         */
        final int[] waitEvents;
        if (!stack.isOnDevice()) {
            setKernelArgs(stack, meta);
            internalEvents[0] = stack.enqueueWrite(events);
            waitEvents = internalEvents;
        } else {
            waitEvents = events;
        }

        int task;
        if (meta == null) {
            task = deviceContext.enqueueNDRangeKernel(kernel, 1, null, singleThreadGlobalWorkSize, singleThreadLocalWorkSize, waitEvents);
        } else {
            if (meta.isParallel()) {
                if (meta.enableThreadCoarsener()) {
                    task = DEFAULT_SCHEDULER.submit(kernel, meta, waitEvents, batchThreads);
                } else {
                    task = scheduler.submit(kernel, meta, waitEvents, batchThreads);
                }
            } else {
                if (meta.isDebug()) {
                    System.out.println("Running on: ");
                    System.out.println("\tPlatform: " + meta.getDevice().getPlatformName());
                    if (meta.getDevice() instanceof OCLTornadoDevice) {
                        System.out.println("\tDevice  : " + ((OCLTornadoDevice) meta.getDevice()).getDevice().getDeviceName());
                    }
                }
                if (meta.getGlobalWork() == null) {
                    task = deviceContext.enqueueNDRangeKernel(kernel, 1, null, singleThreadGlobalWorkSize, singleThreadLocalWorkSize, waitEvents);
                } else {
                    task = deviceContext.enqueueNDRangeKernel(kernel, 1, null, meta.getGlobalWork(), meta.getLocalWork(), waitEvents);
                }
            }

            if (meta.shouldDumpProfiles()) {
                deviceContext.retainEvent(task);
                meta.addProfile(task);
            }

            if (meta.enableExceptions()) {
                internalEvents[0] = task;
                task = stack.enqueueRead(internalEvents);
            }
        }

        return task;
    }

    private void executeSingleThread() {
        deviceContext.enqueueNDRangeKernel(kernel, 1, null, singleThreadGlobalWorkSize, singleThreadLocalWorkSize, null);
    }

    private void debugInfo(final TaskMetaData meta) {
        if (meta.isDebug()) {
            meta.printThreadDims();
        }
    }

    private int submitSequential(final TaskMetaData meta) {
        final int task;
        debugInfo(meta);
        if ((meta.getGlobalWork() == null) || (meta.getGlobalWork().length == 0)) {
            task = deviceContext.enqueueNDRangeKernel(kernel, 1, null, singleThreadGlobalWorkSize, singleThreadLocalWorkSize, null);
        } else {
            task = deviceContext.enqueueNDRangeKernel(kernel, 1, null, meta.getGlobalWork(), meta.getLocalWork(), null);
        }
        return task;
    }

    private int submitParallel(final OCLCallStack stack, final TaskMetaData meta, long batchThreads) {
        final int task;
        if (meta.enableThreadCoarsener()) {
            task = DEFAULT_SCHEDULER.submit(kernel, meta, batchThreads);
        } else {
            task = scheduler.submit(kernel, meta, batchThreads);
        }
        return task;
    }

    private void launchKernel(final OCLCallStack stack, final TaskMetaData meta, long batchThreads) {
        final int task;
        if (meta.isParallel()) {
            task = submitParallel(stack, meta, batchThreads);
        } else {
            task = submitSequential(meta);
        }

        if (meta.shouldDumpProfiles()) {
            deviceContext.retainEvent(task);
            meta.addProfile(task);
        }

        // read the stack
        if (meta.enableExceptions()) {
            stack.enqueueRead(null);
        }
    }

    private void checkKernelNotNull() {
        if (kernel == null) {
            throw new TornadoRuntimeException("[ERROR] Generated Kernel is NULL. \nPlease report this issue to https://github.com/beehive-lab/TornadoVM");
        }
    }

    public void submitWithoutEvents(final OCLCallStack stack, final TaskMetaData meta, long batchThreads) {

        checkKernelNotNull();

        if (DEBUG) {
            info("kernel submitted: id=0x%x, method = %s, device =%s", kernel.getId(), kernel.getName(), deviceContext.getDevice().getDeviceName());
            info("\tstack    : buffer id=0x%x, device=0x%x (0x%x)", stack.toBuffer(), stack.toAbsoluteAddress(), stack.toRelativeAddress());
        }

        /*
         * Only set the kernel arguments if they are either: - not set or - have
         * changed
         */
        if (!stack.isOnDevice()) {
            setKernelArgs(stack, meta);
            stack.enqueueWrite();
        }

        guarantee(kernel != null, "kernel is null");
        if (meta == null) {
            executeSingleThread();
        } else {
            launchKernel(stack, meta, batchThreads);
        }
    }

    @Override
    public int launchWithDeps(CallStack stack, TaskMetaData meta, long batchThreads, int[] waitEvents) {
        return submitWithEvents((OCLCallStack) stack, meta, waitEvents, batchThreads);
    }

    @Override
    public int launchWithoutDeps(CallStack stack, TaskMetaData meta, long batchThreads) {
        submitWithoutEvents((OCLCallStack) stack, meta, batchThreads);
        return -1;
    }

}

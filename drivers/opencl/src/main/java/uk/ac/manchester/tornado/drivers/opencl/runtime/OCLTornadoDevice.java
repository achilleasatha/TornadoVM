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
package uk.ac.manchester.tornado.drivers.opencl.runtime;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.vm.ci.meta.ResolvedJavaMethod;
import uk.ac.manchester.tornado.api.common.Access;
import uk.ac.manchester.tornado.api.common.Event;
import uk.ac.manchester.tornado.api.common.SchedulableTask;
import uk.ac.manchester.tornado.api.enums.TornadoDeviceType;
import uk.ac.manchester.tornado.api.exceptions.TornadoInternalError;
import uk.ac.manchester.tornado.api.exceptions.TornadoMemoryException;
import uk.ac.manchester.tornado.api.exceptions.TornadoOutOfMemoryException;
import uk.ac.manchester.tornado.api.exceptions.TornadoRuntimeException;
import uk.ac.manchester.tornado.api.mm.ObjectBuffer;
import uk.ac.manchester.tornado.api.mm.TaskMetaDataInterface;
import uk.ac.manchester.tornado.api.mm.TornadoDeviceObjectState;
import uk.ac.manchester.tornado.api.mm.TornadoMemoryProvider;
import uk.ac.manchester.tornado.drivers.opencl.OCLCodeCache;
import uk.ac.manchester.tornado.drivers.opencl.OCLDevice;
import uk.ac.manchester.tornado.drivers.opencl.OCLDeviceContext;
import uk.ac.manchester.tornado.drivers.opencl.OCLDriver;
import uk.ac.manchester.tornado.drivers.opencl.enums.OCLDeviceType;
import uk.ac.manchester.tornado.drivers.opencl.graal.OCLProviders;
import uk.ac.manchester.tornado.drivers.opencl.graal.backend.OCLBackend;
import uk.ac.manchester.tornado.drivers.opencl.graal.compiler.OCLCompilationResult;
import uk.ac.manchester.tornado.drivers.opencl.graal.compiler.OCLCompiler;
import uk.ac.manchester.tornado.drivers.opencl.mm.OCLByteArrayWrapper;
import uk.ac.manchester.tornado.drivers.opencl.mm.OCLByteBuffer;
import uk.ac.manchester.tornado.drivers.opencl.mm.OCLCharArrayWrapper;
import uk.ac.manchester.tornado.drivers.opencl.mm.OCLDoubleArrayWrapper;
import uk.ac.manchester.tornado.drivers.opencl.mm.OCLFloatArrayWrapper;
import uk.ac.manchester.tornado.drivers.opencl.mm.OCLIntArrayWrapper;
import uk.ac.manchester.tornado.drivers.opencl.mm.OCLLongArrayWrapper;
import uk.ac.manchester.tornado.drivers.opencl.mm.OCLMemoryManager;
import uk.ac.manchester.tornado.drivers.opencl.mm.OCLMultiDimArrayWrapper;
import uk.ac.manchester.tornado.drivers.opencl.mm.OCLObjectWrapper;
import uk.ac.manchester.tornado.drivers.opencl.mm.OCLShortArrayWrapper;
import uk.ac.manchester.tornado.runtime.TornadoCoreRuntime;
import uk.ac.manchester.tornado.runtime.common.CallStack;
import uk.ac.manchester.tornado.runtime.common.DeviceObjectState;
import uk.ac.manchester.tornado.runtime.common.RuntimeUtilities;
import uk.ac.manchester.tornado.runtime.common.Tornado;
import uk.ac.manchester.tornado.runtime.common.TornadoAcceleratorDevice;
import uk.ac.manchester.tornado.runtime.common.TornadoInstalledCode;
import uk.ac.manchester.tornado.runtime.common.TornadoSchedulingStrategy;
import uk.ac.manchester.tornado.runtime.sketcher.Sketch;
import uk.ac.manchester.tornado.runtime.sketcher.TornadoSketcher;
import uk.ac.manchester.tornado.runtime.tasks.CompilableTask;
import uk.ac.manchester.tornado.runtime.tasks.PrebuiltTask;
import uk.ac.manchester.tornado.runtime.tasks.meta.TaskMetaData;

public class OCLTornadoDevice implements TornadoAcceleratorDevice {

    private final OCLDevice device;
    private final int deviceIndex;
    private final int platformIndex;
    private static OCLDriver driver = null;
    private String platformName;

    private static boolean BENCHMARKING_MODE = Boolean.parseBoolean(System.getProperties().getProperty("tornado.benchmarking", "False"));

    private static OCLDriver findDriver() {
        if (driver == null) {
            driver = TornadoCoreRuntime.getTornadoRuntime().getDriver(OCLDriver.class);
            TornadoInternalError.guarantee(driver != null, "unable to find OpenCL driver");
        }
        return driver;
    }

    public OCLTornadoDevice(final int platformIndex, final int deviceIndex) {
        this.platformIndex = platformIndex;
        this.deviceIndex = deviceIndex;

        platformName = findDriver().getPlatformContext(platformIndex).getPlatform().getName();
        device = findDriver().getPlatformContext(platformIndex).devices().get(deviceIndex);

    }

    @Override
    public void dumpEvents() {
        getDeviceContext().dumpEvents();
    }

    @Override
    public String getDescription() {
        final String availability = (device.isDeviceAvailable()) ? "available" : "not available";
        return String.format("%s %s (%s)", device.getDeviceName(), device.getDeviceType(), availability);
    }

    @Override
    public String getPlatformName() {
        return platformName;
    }

    @Override
    public OCLDevice getDevice() {
        return device;
    }

    public int getDeviceIndex() {
        return deviceIndex;
    }

    @Override
    public TornadoMemoryProvider getMemoryProvider() {
        return getDeviceContext().getMemoryManager();
    }

    public int getPlatformIndex() {
        return platformIndex;
    }

    @Override
    public OCLDeviceContext getDeviceContext() {
        return getBackend().getDeviceContext();
    }

    public OCLBackend getBackend() {
        return findDriver().getBackend(platformIndex, deviceIndex);
    }

    @Override
    public void reset() {
        getBackend().reset();
    }

    @Override
    public String toString() {
        return String.format(getPlatformName() + " -- " + device.getDeviceName());
    }

    @Override
    public TornadoSchedulingStrategy getPreferredSchedule() {
        if (null != device.getDeviceType()) {

            if (Tornado.FORCE_ALL_TO_GPU) {
                return TornadoSchedulingStrategy.PER_ITERATION;
            }

            switch (device.getDeviceType()) {
                case CL_DEVICE_TYPE_GPU:
                    return TornadoSchedulingStrategy.PER_ITERATION;
                case CL_DEVICE_TYPE_CPU:
                    return TornadoSchedulingStrategy.PER_BLOCK;
                default:
                    return TornadoSchedulingStrategy.PER_ITERATION;
            }
        }
        TornadoInternalError.shouldNotReachHere();
        return TornadoSchedulingStrategy.PER_ITERATION;
    }

    @Override
    public boolean isDistibutedMemory() {
        return true;
    }

    @Override
    public void ensureLoaded() {
        final OCLBackend backend = getBackend();
        if (!backend.isInitialised()) {
            backend.init();
        }
    }

    public void ensureLoadedFPGA() {
        getBackend().fpgaJITinit();
    }

    @Override
    public CallStack createStack(int numArgs) {
        return getDeviceContext().getMemoryManager().createCallStack(numArgs);
    }

    private boolean isOpenCLPreLoadBinary(OCLDeviceContext deviceContext, String deviceInfo) {
        OCLCodeCache installedCode = new OCLCodeCache(deviceContext);
        if (!installedCode.isLoadBinaryOptionEnabled() || (installedCode.getOpenCLBinary(deviceInfo) == null)) {
            return false;
        }
        return true;
    }

    private TornadoInstalledCode compileTask(SchedulableTask task) {
        final OCLDeviceContext deviceContext = getDeviceContext();

        final CompilableTask executable = (CompilableTask) task;
        // final long t0 = System.nanoTime();
        final ResolvedJavaMethod resolvedMethod = TornadoCoreRuntime.getTornadoRuntime().resolveMethod(executable.getMethod());

        // final long t1 = System.nanoTime();
        final Sketch sketch = TornadoSketcher.lookup(resolvedMethod);

        // copy meta data into task
        final TaskMetaData sketchMeta = sketch.getMeta();
        final TaskMetaData taskMeta = executable.meta();
        final Access[] sketchAccess = sketchMeta.getArgumentsAccess();
        final Access[] taskAccess = taskMeta.getArgumentsAccess();
        for (int i = 0; i < sketchAccess.length; i++) {
            taskAccess[i] = sketchAccess[i];
        }

        try {
            OCLProviders providers = (OCLProviders) getBackend().getProviders();
            final OCLCompilationResult result = OCLCompiler.compileSketchForDevice(sketch, executable, providers, getBackend());
            if (deviceContext.isCached(task.getId(), resolvedMethod.getName())) {
                // Return the code from the cache
                return deviceContext.getCode(task.getId(), resolvedMethod.getName());
            }

            // Compile the code
            if (Tornado.ACCELERATOR_IS_FPGA) {
                // A) for FPGA
                return deviceContext.installCode(result.getId(), result.getName(), result.getTargetCode(), Tornado.ACCELERATOR_IS_FPGA);
            } else {
                // B) for CPU multicore or GPU
                return deviceContext.installCode(result);
            }

        } catch (Exception e) {
            driver.fatal("unable to compile %s for device %s", task.getId(), getDeviceName());
            driver.fatal("exception occured when compiling %s", ((CompilableTask) task).getMethod().getName());
            driver.fatal("exception: %s", e.toString());
            e.printStackTrace();
        }
        return null;
    }

    private TornadoInstalledCode compilePreBuiltTask(SchedulableTask task) {
        final OCLDeviceContext deviceContext = getDeviceContext();
        final PrebuiltTask executable = (PrebuiltTask) task;
        if (deviceContext.isCached(task.getId(), executable.getEntryPoint())) {
            return deviceContext.getCode(task.getId(), executable.getEntryPoint());
        }

        final Path path = Paths.get(executable.getFilename());
        TornadoInternalError.guarantee(path.toFile().exists(), "file does not exist: %s", executable.getFilename());
        try {
            final byte[] source = Files.readAllBytes(path);
            return deviceContext.installCode(executable.meta(), task.getId(), executable.getEntryPoint(), source);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private TornadoInstalledCode compileJavaToAccelerator(SchedulableTask task) {
        if (task instanceof CompilableTask) {
            return compileTask(task);
        } else if (task instanceof PrebuiltTask) {
            return compilePreBuiltTask(task);
        }
        TornadoInternalError.shouldNotReachHere("task of unknown type: " + task.getClass().getSimpleName());
        return null;
    }

    private TornadoInstalledCode loadPreCompiledBinaryFromCache(SchedulableTask task) {
        final OCLDeviceContext deviceContext = getDeviceContext();
        final OCLCodeCache check = new OCLCodeCache(deviceContext);
        final String deviceFullName = getFullTaskIdDevice(task);
        final Path lookupPath = Paths.get(check.getOpenCLBinary(deviceFullName));
        String[] tempEntryToSplit = task.getName().split("- ");
        String entry = tempEntryToSplit[1];
        return check.installEntryPointForBinaryForFPGAs(lookupPath, entry);
    }

    private String getFullTaskIdDevice(SchedulableTask task) {
        TaskMetaDataInterface meta = task.meta();
        if (meta instanceof TaskMetaData) {
            TaskMetaData stask = (TaskMetaData) task.meta();
            return task.getId() + ".device=" + stask.getDriverIndex() + ":" + stask.getDeviceIndex();
        } else {
            throw new RuntimeException("[ERROR] TaskMedata Expected");
        }
    }

    @Override
    public boolean isFullJITMode(SchedulableTask task) {
        final OCLDeviceContext deviceContext = getDeviceContext();
        final String deviceFullName = getFullTaskIdDevice(task);
        return (!isOpenCLPreLoadBinary(deviceContext, deviceFullName) && Tornado.ACCELERATOR_IS_FPGA);
    }

    @Override
    public TornadoInstalledCode installCode(SchedulableTask task) {
        final OCLDeviceContext deviceContext = getDeviceContext();
        final String deviceFullName = getFullTaskIdDevice(task);
        if (!isOpenCLPreLoadBinary(deviceContext, deviceFullName) && Tornado.ACCELERATOR_IS_FPGA) {
            compileJavaToAccelerator(task);
            ensureLoadedFPGA();
            return loadPreCompiledBinaryFromCache(task);
        } else if (!isOpenCLPreLoadBinary(deviceContext, deviceFullName) && !Tornado.ACCELERATOR_IS_FPGA) {
            return compileJavaToAccelerator(task);
        } else {
            return loadPreCompiledBinaryFromCache(task);
        }
    }

    private ObjectBuffer createArrayWrapper(Class<?> type, OCLDeviceContext device, long batchSize) {
        ObjectBuffer result = null;
        if (type == int[].class) {
            result = new OCLIntArrayWrapper(device, batchSize);
        } else if (type == short[].class) {
            result = new OCLShortArrayWrapper(device, batchSize);
        } else if (type == byte[].class) {
            result = new OCLByteArrayWrapper(device, batchSize);
        } else if (type == float[].class) {
            result = new OCLFloatArrayWrapper(device, batchSize);
        } else if (type == double[].class) {
            result = new OCLDoubleArrayWrapper(device, batchSize);
        } else if (type == long[].class) {
            result = new OCLLongArrayWrapper(device, batchSize);
        } else if (type == char[].class) {
            result = new OCLCharArrayWrapper(device, batchSize);
        } else {
            TornadoInternalError.unimplemented("array of type %s", type.getName());
        }
        return result;
    }

    private ObjectBuffer createMultiArrayWrapper(Class<?> componentType, Class<?> type, OCLDeviceContext device, long batchSize) {
        ObjectBuffer result = null;

        if (componentType == int[].class) {
            result = new OCLMultiDimArrayWrapper<>(device, (OCLDeviceContext context) -> new OCLIntArrayWrapper(context, batchSize), batchSize);
        } else if (componentType == short[].class) {
            result = new OCLMultiDimArrayWrapper<>(device, (OCLDeviceContext context) -> new OCLShortArrayWrapper(context, batchSize), batchSize);
        } else if (componentType == char[].class) {
            result = new OCLMultiDimArrayWrapper<>(device, (OCLDeviceContext context) -> new OCLCharArrayWrapper(context, batchSize), batchSize);
        } else if (componentType == byte[].class) {
            result = new OCLMultiDimArrayWrapper<>(device, (OCLDeviceContext context) -> new OCLByteArrayWrapper(context, batchSize), batchSize);
        } else if (componentType == float[].class) {
            result = new OCLMultiDimArrayWrapper<>(device, (OCLDeviceContext context) -> new OCLFloatArrayWrapper(context, batchSize), batchSize);
        } else if (componentType == double[].class) {
            result = new OCLMultiDimArrayWrapper<>(device, (OCLDeviceContext context) -> new OCLDoubleArrayWrapper(context, batchSize), batchSize);
        } else if (componentType == long[].class) {
            result = new OCLMultiDimArrayWrapper<>(device, (OCLDeviceContext context) -> new OCLLongArrayWrapper(context, batchSize), batchSize);
        } else {
            TornadoInternalError.unimplemented("array of type %s", type.getName());
        }
        return result;
    }

    private ObjectBuffer createDeviceBuffer(Class<?> type, Object arg, OCLDeviceContext device, long batchSize) throws TornadoOutOfMemoryException {
        ObjectBuffer result = null;
        if (type.isArray()) {

            if (!type.getComponentType().isArray()) {
                result = createArrayWrapper(type, device, batchSize);
            } else {
                final Class<?> componentType = type.getComponentType();
                if (RuntimeUtilities.isPrimitiveArray(componentType)) {
                    result = createMultiArrayWrapper(componentType, type, device, batchSize);
                } else {
                    TornadoInternalError.unimplemented("multi-dimensional array of type %s", type.getName());
                }
            }

        } else if (!type.isPrimitive() && !type.isArray()) {
            result = new OCLObjectWrapper(device, arg, batchSize);
        }

        TornadoInternalError.guarantee(result != null, "Unable to create buffer for object: " + type);
        return result;
    }

    private void checkBatchSize(long batchSize) {
        if (batchSize > 0) {
            throw new TornadoRuntimeException("[ERROR] Batch computation with non-arrays not supported yet.");
        }
    }

    private void reserveMemory(Object object, long batchSize, TornadoDeviceObjectState state) {
        try {
            final ObjectBuffer buffer = createDeviceBuffer(object.getClass(), object, getDeviceContext(), batchSize);
            buffer.allocate(object, batchSize);
            state.setBuffer(buffer);

            final Class<?> type = object.getClass();
            if (!type.isArray()) {
                checkBatchSize(batchSize);
                buffer.write(object);
            }

            state.setValid(true);
        } catch (TornadoOutOfMemoryException | TornadoMemoryException e) {
            e.printStackTrace();
        }
    }

    private void checkForResizeBuffer(Object object, long batchSize, TornadoDeviceObjectState state) {
        // We re-allocate if buffer size has changed
        final ObjectBuffer buffer = state.getBuffer();
        try {
            buffer.allocate(object, batchSize);
        } catch (TornadoOutOfMemoryException | TornadoMemoryException e) {
            e.printStackTrace();
        }
    }

    private void reAllocateInvalidBuffer(Object object, long batchSize, TornadoDeviceObjectState state) {
        try {
            state.getBuffer().allocate(object, batchSize);
            final Class<?> type = object.getClass();
            if (!type.isArray()) {
                checkBatchSize(batchSize);
                state.getBuffer().write(object);
            }
            state.setValid(true);
        } catch (TornadoOutOfMemoryException | TornadoMemoryException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int ensureAllocated(Object object, long batchSize, TornadoDeviceObjectState state) {
        if (!state.hasBuffer()) {
            reserveMemory(object, batchSize, state);
        } else {
            checkForResizeBuffer(object, batchSize, state);
        }

        if (!state.isValid()) {
            reAllocateInvalidBuffer(object, batchSize, state);
        }
        return -1;
    }

    @Override
    public int ensurePresent(Object object, TornadoDeviceObjectState state, int[] events, long batchSize, long offset) {
        if (!state.isValid()) {
            ensureAllocated(object, batchSize, state);
        }

        if (BENCHMARKING_MODE || !state.hasContents()) {
            state.setContents(true);
            int event = state.getBuffer().enqueueWrite(object, batchSize, offset, events, events == null);
            if (events != null) {
                return event;
            }
        }
        return -1;
    }

    @Override
    public int streamIn(Object object, long batchSize, long offset, TornadoDeviceObjectState state, int[] events) {
        if (batchSize > 0 || !state.isValid()) {
            ensureAllocated(object, batchSize, state);
        }
        state.setContents(true);
        return state.getBuffer().enqueueWrite(object, batchSize, offset, events, events == null);
    }

    @Override
    public int streamOut(Object object, long offset, TornadoDeviceObjectState state, int[] events) {
        TornadoInternalError.guarantee(state.isValid(), "invalid variable");
        int event = state.getBuffer().enqueueRead(object, offset, events, events == null);
        if (events != null) {
            return event;
        }
        return -1;
    }

    @Override
    public int streamOutBlocking(Object object, long hostOffset, TornadoDeviceObjectState state, int[] events) {
        TornadoInternalError.guarantee(state.isValid(), "invalid variable");
        return state.getBuffer().read(object, hostOffset, events, events == null);
    }

    public void sync(Object... objects) {
        for (Object obj : objects) {
            sync(obj);
        }
    }

    public void sync(Object object) {
        final DeviceObjectState state = TornadoCoreRuntime.getTornadoRuntime().resolveObject(object).getDeviceState(this);
        resolveEvent(streamOut(object, 0, state, null)).waitOn();
    }

    @Override
    public void flush() {
        this.getDeviceContext().flush();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OCLTornadoDevice) {
            final OCLTornadoDevice other = (OCLTornadoDevice) obj;
            return (other.deviceIndex == deviceIndex && other.platformIndex == platformIndex);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + this.deviceIndex;
        hash = 89 * hash + this.platformIndex;
        return hash;
    }

    @Override
    public void sync() {
        getDeviceContext().sync();
    }

    @Override
    public int enqueueBarrier() {
        return getDeviceContext().enqueueBarrier();
    }

    @Override
    public int enqueueBarrier(int[] events) {
        return getDeviceContext().enqueueBarrier(events);
    }

    @Override
    public int enqueueMarker() {
        return getDeviceContext().enqueueMarker();
    }

    @Override
    public int enqueueMarker(int[] events) {
        return getDeviceContext().enqueueMarker(events);
    }

    @Override
    public void dumpMemory(String file) {
        final OCLMemoryManager mm = getDeviceContext().getMemoryManager();
        final OCLByteBuffer buffer = mm.getSubBuffer(0, (int) mm.getHeapSize());
        buffer.read();

        try (FileOutputStream fos = new FileOutputStream(file); FileChannel channel = fos.getChannel()) {
            channel.write(buffer.buffer());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public Event resolveEvent(int event) {
        return getDeviceContext().resolveEvent(event);
    }

    @Override
    public void flushEvents() {
        getDeviceContext().flushEvents();
    }

    @Override
    public void markEvent() {
        getDeviceContext().markEvent();
    }

    @Override
    public String getDeviceName() {
        return String.format("opencl-%d-%d", platformIndex, deviceIndex);
    }

    @Override
    public TornadoDeviceType getDeviceType() {
        OCLDeviceType deviceType = device.getDeviceType();
        switch (deviceType) {
            case CL_DEVICE_TYPE_CPU:
                return TornadoDeviceType.CPU;
            case CL_DEVICE_TYPE_GPU:
                return TornadoDeviceType.GPU;
            case CL_DEVICE_TYPE_ACCELERATOR:
                return TornadoDeviceType.ACCELERATOR;
            case CL_DEVICE_TYPE_CUSTOM:
                return TornadoDeviceType.CUSTOM;
            case CL_DEVICE_TYPE_ALL:
                return TornadoDeviceType.ALL;
            case CL_DEVICE_TYPE_DEFAULT:
                return TornadoDeviceType.DEFAULT;
            default:
                throw new RuntimeException("Device not supported");
        }
    }

    @Override
    public long getMaxAllocMemory() {
        return device.getDeviceMaxAllocationSize();
    }

    @Override
    public long getMaxGlobalMemory() {
        return device.getDeviceGlobalMemorySize();
    }

}
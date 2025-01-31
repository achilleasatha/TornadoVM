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
package uk.ac.manchester.tornado.drivers.opencl.graal.nodes;

import static uk.ac.manchester.tornado.api.exceptions.TornadoInternalError.guarantee;

import org.graalvm.compiler.core.common.type.StampFactory;
import org.graalvm.compiler.graph.NodeClass;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.calc.FloatingNode;
import org.graalvm.compiler.nodes.spi.LIRLowerable;
import org.graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

import jdk.vm.ci.meta.Value;
import uk.ac.manchester.tornado.drivers.opencl.graal.meta.OCLMemorySpace;
import uk.ac.manchester.tornado.drivers.opencl.graal.meta.OCLStack;

@NodeInfo
public class OCLMemoryRegion extends FloatingNode implements LIRLowerable {

    public static final NodeClass<OCLMemoryRegion> TYPE = NodeClass.create(OCLMemoryRegion.class);

    public static enum Region {
        GLOBAL, LOCAL, PRIVATE, CONSTANT, STACK, HEAP;
    }

    protected Region region;

    public OCLMemoryRegion(Region region) {
        super(TYPE, StampFactory.objectNonNull());
        this.region = region;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen) {
        Value value = null;
        switch (region) {
            case GLOBAL:
                value = OCLMemorySpace.GLOBAL;
                break;
            case LOCAL:
                value = OCLMemorySpace.LOCAL;
                break;
            case CONSTANT:
                value = OCLMemorySpace.CONSTANT;
                break;
            case PRIVATE:
                value = OCLMemorySpace.PRIVATE;
                break;
            case STACK:
                value = OCLStack.STACK;
                break;
            case HEAP:
                value = OCLMemorySpace.HEAP;
                break;

        }

        guarantee(value != null, "unimplemented region: %s", region);
        gen.setResult(this, value);
    }

}

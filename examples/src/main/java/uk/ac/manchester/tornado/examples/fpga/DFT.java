/*
 * This file is part of Tornado: A heterogeneous programming framework:
 * https://github.com/beehive-lab/tornado
 *
 * Copyright (c) 2013-2018, APT Group, School of Computer Science,
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
 * Authors: Michalis Papadimitriou
 *
 */

package uk.ac.manchester.tornado.examples.fpga;

import static uk.ac.manchester.tornado.collections.math.TornadoMath.abs;

import uk.ac.manchester.tornado.api.Parallel;
import uk.ac.manchester.tornado.collections.math.TornadoMath;
import uk.ac.manchester.tornado.runtime.api.TaskSchedule;

public class DFT {

    private static int size;
    private static TaskSchedule graph;
    private static float[] inReal,inImag,outReal,outImag;
    private static int[] inputSize;

    public static void computeDft(float[] inreal, float[] inimag, float[] outreal, float[] outimag, int[] inputSize) {
        int n = inreal.length;
        for (@Parallel int k = 0; k < n; k++) { // For each output element
            float sumreal = 0;
            float sumimag = 0;
            for (int t = 0; t < inputSize[0]; t++) { // For each input element
                float angle = ((2 * TornadoMath.floatPI() * t * k) / (float) n);
                sumreal += (inreal[t] * (TornadoMath.floatCos(angle)) + inimag[t] * (TornadoMath.floatSin(angle)));
                sumimag += -(inreal[t] * (TornadoMath.floatSin(angle)) + inimag[t] * (TornadoMath.floatCos(angle)));
            }
            outreal[k] = sumreal;
            outimag[k] = sumimag;

        }
    }

    public static boolean validate() {
        boolean val = true;
        float[] outRealTor = new float[size];
        float[] outImagTor = new float[size];

        graph.warmup();
        graph.execute();
        graph.streamOut(outReal, outImag);

        DFT.computeDft(inReal, inImag, outRealTor, outImagTor, inputSize);

        for (int i = 0; i < 1; i++) {

            long t1 = System.nanoTime();
            DFT.computeDft(inReal, inImag, outRealTor, outImagTor, inputSize);
            long t2 = System.nanoTime();

            long seqTime = t2 - t1;
            System.out.println("Sequential time: " + seqTime + "\n");

        }

        for (int i = 0; i < size; i++) {
            if (abs(outImagTor[i] - outImag[i]) > 0.1) {
                System.out.println(outImagTor[i] + " vs " + outImag[i] + "\n");
                val = false;
                break;
            }
            if (abs(outReal[i] - outRealTor[i]) > 0.1) {
                System.out.println(outReal[i] + " vs " + outRealTor[i] + "\n");
                val = false;
                break;
            }
        }
        System.out.printf("Number validation: " + val + "\n");
        return val;
    }

    public static void main(String[] args) {

        size = Integer.parseInt(args[0]);

        inReal = new float[size];
        inImag = new float[size];
        outReal = new float[size];
        outImag = new float[size];
        inputSize = new int[1];

        inputSize[0] = size;

        for (int i = 0; i < size; i++) {
            inReal[i] = 1 / (float) (i + 2);
            inImag[i] = 1 / (float) (i + 2);
        }

        graph = new TaskSchedule("s0");
        graph.task("t0", DFT::computeDft, inReal, inImag, outReal, outImag, inputSize);
        graph.streamOut(outReal, outImag);
        graph.warmup();

        for (int i = 0; i < 10; i++) {
            graph.execute();
        }

//        if (validate()) {
//            System.out.println("Validation: " + "SUCCESS " + "\n");
//        } else {
//            System.out.println("Validation: " + " FAIL " + "\n");
//
//        }
    }
}
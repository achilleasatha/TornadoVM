/*
 * Copyright (c) 2013-2019, APT Group, School of Computer Science,
 * The University of Manchester.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package uk.ac.manchester.tornado.examples.compute;

import java.util.Arrays;

import uk.ac.manchester.tornado.api.TaskSchedule;
import uk.ac.manchester.tornado.api.annotations.Parallel;

public class NBody {

    private static void nBody(int numBodies, float[] refPos, float[] refVel, float delT, float espSqr) {
        for (@Parallel int i = 0; i < numBodies; i++) {
            int body = 4 * i;
            float[] acc = new float[] { 0.0f, 0.0f, 0.0f };
            for (int j = 0; j < numBodies; j++) {
                float[] r = new float[3];
                int index = 4 * j;

                float distSqr = 0.0f;
                for (int k = 0; k < 3; k++) {
                    r[k] = refPos[index + k] - refPos[body + k];
                    distSqr += r[k] * r[k];
                }

                float invDist = (float) (1.0f / Math.sqrt(distSqr + espSqr));

                float invDistCube = invDist * invDist * invDist;
                float s = refPos[index + 3] * invDistCube;

                for (int k = 0; k < 3; k++) {
                    acc[k] += s * r[k];
                }
            }
            for (int k = 0; k < 3; k++) {
                refPos[body + k] += refVel[body + k] * delT + 0.5f * acc[k] * delT * delT;
                refVel[body + k] += acc[k] * delT;
            }
        }
    }

    public static void main(String[] args) {
        float delT,espSqr;
        float[] posSeq,velSeq;

        StringBuffer resultsIterations = new StringBuffer();

        int numBodies = 32768;
        int iterations = 10;

        if (args.length == 2) {
            numBodies = Integer.parseInt(args[0]);
            iterations = Integer.parseInt(args[1]);
        } else if (args.length == 1) {
            numBodies = Integer.parseInt(args[0]);
        }

        System.out.println("Running Nbody with " + numBodies + " bodies" + " and " + iterations + " iterations");

        delT = 0.005f;
        espSqr = 500.0f;

        float[] auxPositionRandom = new float[numBodies * 4];
        float[] auxVelocityZero = new float[numBodies * 3];

        for (int i = 0; i < auxPositionRandom.length; i++) {
            auxPositionRandom[i] = (float) Math.random();
        }

        Arrays.fill(auxVelocityZero, 0.0f);

        posSeq = new float[numBodies * 4];
        velSeq = new float[numBodies * 4];

        for (int i = 0; i < auxPositionRandom.length; i++) {
            posSeq[i] = auxPositionRandom[i];
        }
        for (int i = 0; i < auxVelocityZero.length; i++) {
            velSeq[i] = auxVelocityZero[i];
        }

        long start = 0;
        long end = 0;
        for (int i = 0; i < iterations; i++) {
            System.gc();
            start = System.nanoTime();
            nBody(numBodies, posSeq, velSeq, delT, espSqr);
            end = System.nanoTime();
            resultsIterations.append("\tSequential execution time of iteration " + i + " is: " + (end - start) + " ns");
            resultsIterations.append("\n");
        }

        long timeSequential = (end - start);

        System.out.println(resultsIterations.toString());

        // @formatter:off
        final TaskSchedule t0 = new TaskSchedule("s0")
                .task("t0", NBody::nBody, numBodies, posSeq, velSeq, delT, espSqr);
        // @formatter:on

        t0.warmup();

        resultsIterations = new StringBuffer();

        for (int i = 0; i < iterations; i++) {
            System.gc();
            start = System.nanoTime();
            t0.execute();
            end = System.nanoTime();
            resultsIterations.append("\tTornado execution time of iteration " + i + " is: " + (end - start) + " ns");
            resultsIterations.append("\n");

        }
        long timeParallel = (end - start);

        System.out.println(resultsIterations.toString());

        System.out.println("Speedup in peak performance: " + (timeSequential / timeParallel) + "x");
    }

}

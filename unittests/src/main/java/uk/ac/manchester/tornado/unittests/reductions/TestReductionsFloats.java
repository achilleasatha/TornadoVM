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

package uk.ac.manchester.tornado.unittests.reductions;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.Ignore;
import org.junit.Test;

import uk.ac.manchester.tornado.api.TaskSchedule;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.annotations.Reduce;
import uk.ac.manchester.tornado.unittests.common.TornadoTestBase;

public class TestReductionsFloats extends TornadoTestBase {

    private static final int SIZE = 8192;
    private static final int SIZE2 = 32;
    private static final int PI_SIZE = 32768;

    public static void reductionAddFloats(float[] input, @Reduce float[] result) {
        result[0] = 0.0f;
        for (@Parallel int i = 0; i < input.length; i++) {
            result[0] += input[i];
        }
    }

    @Test
    public void testSumFloats() {
        float[] input = new float[SIZE];
        float[] result = new float[1];
        final int neutral = 0;
        Arrays.fill(result, neutral);

        Random r = new Random();
        IntStream.range(0, input.length).sequential().forEach(i -> {
            input[i] = r.nextFloat();
        });

        //@formatter:off
		TaskSchedule task = new TaskSchedule("s0")
			.streamIn(input)
			.task("t0", TestReductionsFloats::reductionAddFloats, input, result)
			.streamOut(result);
		//@formatter:on

        task.execute();

        float[] sequential = new float[1];
        reductionAddFloats(input, sequential);

        // Check result
        assertEquals(sequential[0], result[0], 0.1f);
    }

    public static void reductionAddFloats2(float[] input, @Reduce float[] result) {
        float error = 2f;
        for (@Parallel int i = 0; i < input.length; i++) {
            float v = (error * input[i]);
            result[0] += v;
        }
    }

    public static void reductionAddFloats3(float[] input, @Reduce float[] result) {
        float error = 2f;
        for (@Parallel int i = 0; i < input.length; i++) {
            float v = (error * input[i]);
            result[0] += v;
        }
    }

    public static void reductionAddFloats4(float[] inputA, float[] inputB, @Reduce float[] result) {
        float error = 2f;
        for (@Parallel int i = 0; i < inputA.length; i++) {
            result[0] += (error * (inputA[i] + inputB[i]));
        }
    }

    @Test
    public void testSumFloats2() {
        float[] input = new float[SIZE];
        float[] result = new float[1];

        Random r = new Random();
        IntStream.range(0, SIZE).sequential().forEach(i -> {
            input[i] = r.nextFloat();
        });

        //@formatter:off
        TaskSchedule task = new TaskSchedule("s0")
            .streamIn(input)
            .task("t0", TestReductionsFloats::reductionAddFloats3, input, result)
            .streamOut(result);
        //@formatter:on

        task.execute();

        float[] sequential = new float[1];
        reductionAddFloats2(input, sequential);

        // Check result
        assertEquals(sequential[0], result[0], 0.1f);
    }

    @Test
    public void testSumFloats3() {
        float[] inputA = new float[SIZE];
        float[] inputB = new float[SIZE];
        float[] result = new float[1];

        Random r = new Random();
        IntStream.range(0, SIZE).sequential().forEach(i -> {
            inputA[i] = r.nextFloat();
            inputB[i] = r.nextFloat();
        });

        //@formatter:off
        TaskSchedule task = new TaskSchedule("s0")
            .streamIn(inputA, inputB)
            .task("t0", TestReductionsFloats::reductionAddFloats4, inputA, inputB, result)
            .streamOut(result);
        //@formatter:on

        task.execute();

        float[] sequential = new float[1];
        reductionAddFloats4(inputA, inputB, sequential);

        // Check result
        assertEquals(sequential[0], result[0], 0.1f);
    }

    public static void multiplyFloats(float[] input, @Reduce float[] result) {
        for (@Parallel int i = 0; i < input.length; i++) {
            result[0] *= input[i];
        }
    }

    @Test
    public void testMultFloats() {
        float[] input = new float[SIZE];
        float[] result = new float[1];
        final int neutral = 1;
        Arrays.fill(result, neutral);

        Random r = new Random();
        IntStream.range(0, SIZE).sequential().forEach(i -> {
            input[i] = 1.0f;
        });

        input[10] = r.nextFloat();
        input[12] = r.nextFloat();

        //@formatter:off
        new TaskSchedule("s0")
            .streamIn(input)
            .task("t0", TestReductionsFloats::multiplyFloats, input, result)
            .streamOut(result)
            .execute();
        //@formatter:on

        float[] sequential = new float[] { 1.0f };
        multiplyFloats(input, sequential);

        // Check result
        assertEquals(sequential[0], result[0], 0.1f);
    }

    public static void reductionAddFloatsConditionally(float[] input, @Reduce float[] result) {
        for (@Parallel int i = 0; i < input.length; i++) {
            float v = 0.0f;
            if (input[0] == -1) {
                v = 1.0f;
            }
            result[0] += v;
        }
    }

    // This is currently not supported
    @Ignore
    public void testSumFloatsCondition() {
        float[] input = new float[SIZE2];
        float[] result = new float[1];

        Random r = new Random();
        IntStream.range(0, SIZE2).sequential().forEach(i -> {
            input[i] = r.nextFloat();
        });

        //@formatter:off
        TaskSchedule task = new TaskSchedule("s0")
            .streamIn(input)
            .task("t0", TestReductionsFloats::reductionAddFloatsConditionally, input, result)
            .streamOut(result);
        //@formatter:on

        task.execute();

        float[] sequential = new float[1];
        reductionAddFloatsConditionally(input, sequential);

        // Check result
        assertEquals(sequential[0], result[0], 0.01f);
    }

    public static void computePi(float[] input, @Reduce float[] result) {
        for (@Parallel int i = 1; i < input.length; i++) {
            float value = (float) (Math.pow(-1, i + 1) / (2 * i - 1));
            result[0] += value + input[i];
        }
    }

    @Test
    public void testComputePi() {
        float[] input = new float[PI_SIZE];
        float[] result = new float[1];

        IntStream.range(0, PI_SIZE).sequential().forEach(i -> {
            input[i] = 0;
        });

        Arrays.fill(result, 0.0f);

        //@formatter:off
        new TaskSchedule("s0")
            .task("t0", TestReductionsFloats::computePi, input, result)
            .streamOut(result)
            .execute();
        //@formatter:on

        final float piValue = result[0] * 4;

        assertEquals(3.14, piValue, 0.01f);
    }

    public static void maxReductionAnnotation(float[] input, @Reduce float[] result) {
        for (@Parallel int i = 0; i < input.length; i++) {
            result[0] = Math.max(result[0], input[i]);
        }
    }

    @Test
    public void testMaxReduction() {
        float[] input = new float[SIZE];
        float[] result = new float[1];
        IntStream.range(0, SIZE).forEach(idx -> {
            input[idx] = idx;
        });

        Arrays.fill(result, Float.MIN_VALUE);

        //@formatter:off
        new TaskSchedule("s0")
            .streamIn(input)
            .task("t0", TestReductionsFloats::maxReductionAnnotation, input, result)
            .streamOut(result)
            .execute();
        //@formatter:on

        float[] sequential = new float[] { Float.MIN_VALUE };
        maxReductionAnnotation(input, sequential);

        assertEquals(sequential[0], result[0], 0.01f);
    }

    public static void minReductionAnnotation(float[] input, @Reduce float[] result, float neutral) {
        result[0] = neutral;
        for (@Parallel int i = 0; i < input.length; i++) {
            result[0] = Math.min(result[0], input[i]);
        }
    }

    @Test
    public void testMinReduction() {
        float[] input = new float[SIZE];
        float[] result = new float[1];

        IntStream.range(0, SIZE).parallel().forEach(idx -> {
            input[idx] = idx;
        });

        Arrays.fill(result, Float.MAX_VALUE);

        //@formatter:off
        new TaskSchedule("s0")
            .streamIn(input)
            .task("t0", TestReductionsFloats::minReductionAnnotation, input, result, Float.MAX_VALUE)
            .streamOut(result)
            .execute();
        //@formatter:on

        float[] sequential = new float[1];
        minReductionAnnotation(input, sequential, Float.MAX_VALUE);

        assertEquals(sequential[0], result[0], 0.01f);
    }

}

/*
 * This file is part of Tornado: A heterogeneous programming framework: 
 * https://github.com/beehive-lab/tornado
 *
 * Copyright (c) 2013-2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GNU Classpath is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 * 
 * GNU Classpath is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with GNU Classpath; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 * 
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 *
 */
package uk.ac.manchester.tornado.api.common;

import uk.ac.manchester.tornado.api.common.TornadoFunctions.Task;
import uk.ac.manchester.tornado.api.common.TornadoFunctions.Task1;
import uk.ac.manchester.tornado.api.common.TornadoFunctions.Task10;
import uk.ac.manchester.tornado.api.common.TornadoFunctions.Task15;
import uk.ac.manchester.tornado.api.common.TornadoFunctions.Task2;
import uk.ac.manchester.tornado.api.common.TornadoFunctions.Task3;
import uk.ac.manchester.tornado.api.common.TornadoFunctions.Task4;
import uk.ac.manchester.tornado.api.common.TornadoFunctions.Task5;
import uk.ac.manchester.tornado.api.common.TornadoFunctions.Task6;
import uk.ac.manchester.tornado.api.common.TornadoFunctions.Task7;
import uk.ac.manchester.tornado.api.common.TornadoFunctions.Task8;
import uk.ac.manchester.tornado.api.common.TornadoFunctions.Task9;

public class TaskPackage {

    private String id;
    private int taskType;
    private Object[] taskParameters;
    private long numThreadsToRun;

    public TaskPackage(String id, Task code) {
        this.id = id;
        this.taskType = 0;
        this.taskParameters = new Object[] { code };
    }

    public <T1> TaskPackage(String id, Task1<T1> code, T1 arg) {
        this.id = id;
        this.taskType = 1;
        this.taskParameters = new Object[] { code, arg };
    }

    public <T1, T2> TaskPackage(String id, Task2<T1, T2> code, T1 arg1, T2 arg2) {
        this.id = id;
        this.taskType = 2;
        this.taskParameters = new Object[] { code, arg1, arg2 };
    }

    public <T1, T2, T3> TaskPackage(String id, Task3<T1, T2, T3> code, T1 arg1, T2 arg2, T3 arg3) {
        this.id = id;
        this.taskType = 3;
        this.taskParameters = new Object[] { code, arg1, arg2, arg3 };

    }

    public <T1, T2, T3, T4> TaskPackage(String id, Task4<T1, T2, T3, T4> code, T1 arg1, T2 arg2, T3 arg3, T4 arg4) {
        this.id = id;
        this.taskType = 4;
        this.taskParameters = new Object[] { code, arg1, arg2, arg3, arg4 };
    }

    public <T1, T2, T3, T4, T5> TaskPackage(String id, Task5<T1, T2, T3, T4, T5> code, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5) {
        this.id = id;
        this.taskType = 5;
        this.taskParameters = new Object[] { code, arg1, arg2, arg3, arg4, arg5 };
    }

    public <T1, T2, T3, T4, T5, T6> TaskPackage(String id, Task6<T1, T2, T3, T4, T5, T6> code, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6) {
        this.id = id;
        this.taskType = 6;
        this.taskParameters = new Object[] { code, arg1, arg2, arg3, arg4, arg5, arg6 };
    }

    public <T1, T2, T3, T4, T5, T6, T7> TaskPackage(String id, Task7<T1, T2, T3, T4, T5, T6, T7> code, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7) {
        this.id = id;
        this.taskType = 7;
        this.taskParameters = new Object[] { code, arg1, arg2, arg3, arg4, arg5, arg6, arg7 };
    }

    public <T1, T2, T3, T4, T5, T6, T7, T8> TaskPackage(String id, Task8<T1, T2, T3, T4, T5, T6, T7, T8> code, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7, T8 arg8) {
        this.id = id;
        this.taskType = 8;
        this.taskParameters = new Object[] { code, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8 };
    }

    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> TaskPackage(String id, Task9<T1, T2, T3, T4, T5, T6, T7, T8, T9> code, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7, T8 arg8,
            T9 arg9) {
        this.id = id;
        this.taskType = 9;
        this.taskParameters = new Object[] { code, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9 };
    }

    public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> TaskPackage(String id, Task10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> code, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7,
            T8 arg8, T9 arg9, T10 arg10) {
        this.id = id;
        this.taskType = 10;
        this.taskParameters = new Object[] { code, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10 };
    }

    public <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> TaskPackage(String id, Task15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> code, T1 arg1, T2 arg2,
            T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7, T8 arg8, T9 arg9, T10 arg10, T11 arg11, T12 arg12, T13 arg13, T14 arg14, T15 arg15) {
        this.id = id;
        this.taskType = 15;
        this.taskParameters = new Object[] { code, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15 };
    }

    public String getId() {
        return id;
    }

    public int getTaskType() {
        return taskType;
    }

    public void setNumThreadsToRun(long numThreads) {
        this.numThreadsToRun = numThreads;
    }

    public long getNumThreadsToRun() {
        return numThreadsToRun;
    }

    /**
     * Get all parameters to the lambda expression. First parameter is reserved
     * to the input code.
     * 
     * @return an object array with all parameters.
     */
    public Object[] getTaskParameters() {
        return taskParameters;
    }

    public static TaskPackage createPackage(String id, Task code) {
        return new TaskPackage(id, code);
    }

    public static <T1> TaskPackage createPackage(String id, Task1<T1> code, T1 arg) {
        return new TaskPackage(id, code, arg);
    }

    public static <T1, T2> TaskPackage createPackage(String id, Task2<T1, T2> code, T1 arg1, T2 arg2) {
        return new TaskPackage(id, code, arg1, arg2);
    }

    public static <T1, T2, T3> TaskPackage createPackage(String id, Task3<T1, T2, T3> code, T1 arg1, T2 arg2, T3 arg3) {
        return new TaskPackage(id, code, arg1, arg2, arg3);
    }

    public static <T1, T2, T3, T4> TaskPackage createPackage(String id, Task4<T1, T2, T3, T4> code, T1 arg1, T2 arg2, T3 arg3, T4 arg4) {
        return new TaskPackage(id, code, arg1, arg2, arg3, arg4);
    }

    public static <T1, T2, T3, T4, T5> TaskPackage createPackage(String id, Task5<T1, T2, T3, T4, T5> code, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5) {
        return new TaskPackage(id, code, arg1, arg2, arg3, arg4, arg5);
    }

    public static <T1, T2, T3, T4, T5, T6> TaskPackage createPackage(String id, Task6<T1, T2, T3, T4, T5, T6> code, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6) {
        return new TaskPackage(id, code, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    public static <T1, T2, T3, T4, T5, T6, T7> TaskPackage createPackage(String id, Task7<T1, T2, T3, T4, T5, T6, T7> code, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7) {
        return new TaskPackage(id, code, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8> TaskPackage createPackage(String id, Task8<T1, T2, T3, T4, T5, T6, T7, T8> code, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7,
            T8 arg8) {
        return new TaskPackage(id, code, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> TaskPackage createPackage(String id, Task9<T1, T2, T3, T4, T5, T6, T7, T8, T9> code, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6,
            T7 arg7, T8 arg8, T9 arg9) {
        return new TaskPackage(id, code, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> TaskPackage createPackage(String id, Task10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> code, T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5,
            T6 arg6, T7 arg7, T8 arg8, T9 arg9, T10 arg10) {
        return new TaskPackage(id, code, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> TaskPackage createPackage(String id, Task15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> code,
            T1 arg1, T2 arg2, T3 arg3, T4 arg4, T5 arg5, T6 arg6, T7 arg7, T8 arg8, T9 arg9, T10 arg10, T11 arg11, T12 arg12, T13 arg13, T14 arg14, T15 arg15) {
        return new TaskPackage(id, code, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15);
    }
}

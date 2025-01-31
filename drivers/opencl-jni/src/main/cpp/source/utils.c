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
#include "utils.h"
#include <stdio.h>
#include <string.h>

char *getOpenCLError(char *func, cl_int code) {
    char *str;
    char *msg = malloc(sizeof (char)*128);
    memset(msg, '\0', 128);
    switch (code) {
        case CL_SUCCESS:
            str = "Operation completed successfully.";
            break;
        case CL_INVALID_VALUE:
            str = "CL_INVALID_VALUE";
            break;
        case CL_INVALID_DEVICE:
            str = "CL_INVALID_DEVICE";
            break;
        case CL_DEVICE_NOT_AVAILABLE:
            str = "CL_DEVICE_NOT_AVAILABLE";
            break;
        case CL_OUT_OF_HOST_MEMORY:
            str = "CL_OUT_OF_HOST_MEMORY";
            break;
        case CL_INVALID_CONTEXT:
            str = "CL_INVALID_CONTEXT";
            break;
        case CL_INVALID_MEM_OBJECT:
            str = "CL_INVALID_MEM_OBJECT";
            break;
        default:
            str = "Unknown OpenCL Error";
    }
    sprintf(msg, "%s(%d) %s", func, (int) code, str);
    return msg;
}



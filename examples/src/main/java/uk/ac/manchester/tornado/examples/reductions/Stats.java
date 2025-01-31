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
package uk.ac.manchester.tornado.examples.reductions;

import java.util.ArrayList;
import java.util.Collections;

public class Stats {

    public static double computeMedian(ArrayList<Long> input) {
        Collections.sort(input);
        double middle = input.size() / 2;
        if (input.size() % 2 == 1) {
            middle = (input.get(input.size() / 2) + input.get(input.size() / 2 - 1)) / 2;
        }
        return middle;
    }
}

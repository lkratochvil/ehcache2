/**
 *  Copyright 2003-2010 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.search.aggregator;

/**
 * An Aggregator takes input objects and computes an aggregate function from them.
 * 
 * @author Greg Luck
 */
public interface Aggregator<T> {

    /**
     * Add the given value to the aggregator function
     * 
     * @param input
     *            a single input value
     * @throws AggregatorException
     *             if the function cannot be computed, possibly due to unsupported types
     */
    void accept(Object input) throws AggregatorException;

    /**
     * Retrieve the final result
     * 
     * @return aggregate result
     */
    T aggregateResult();

}

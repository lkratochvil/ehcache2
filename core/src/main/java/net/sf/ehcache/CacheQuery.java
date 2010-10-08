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

package net.sf.ehcache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Direction;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.aggregator.Aggregator;
import net.sf.ehcache.search.aggregator.AggregatorException;
import net.sf.ehcache.search.expression.AlwaysMatchCriteria;
import net.sf.ehcache.search.expression.And;
import net.sf.ehcache.search.expression.Criteria;
import net.sf.ehcache.store.StoreQuery;

/**
 * Query builder implementation. Instances are bound to a specific cache
 * 
 * @author teck
 */
class CacheQuery implements Query, StoreQuery {

    private volatile boolean frozen;
    private volatile boolean includeKeys;
    private volatile boolean includeValues;
    private volatile int maxResults = -1;

    private final List<Ordering> orderings = Collections.synchronizedList(new ArrayList<Ordering>());
    private final List<Attribute<?>> includedAttributes = Collections.synchronizedList(new ArrayList<Attribute<?>>());
    private final List<Criteria> criteria = Collections.synchronizedList(new ArrayList<Criteria>());
    private final List<AttributeAggregator> aggregators = Collections.synchronizedList(new ArrayList<AttributeAggregator>());
    private final Cache cache;

    /**
     * Create a new builder instance
     * 
     * @param cache
     */
    public CacheQuery(Cache cache) {
        this.cache = cache;
    }

    /**
     * {@inheritDoc}
     */
    public Query includeKeys() {
        checkFrozen();
        this.includeKeys = true;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Query includeValues() {
        checkFrozen();
        this.includeValues = true;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Query includeAttribute(Attribute<?>... attributes) {
        checkFrozen();

        for (Attribute<?> attribute : attributes) {
            if (attribute == null) {
                throw new NullPointerException("null attribute");
            }

            this.includedAttributes.add(attribute);
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Query includeAggregator(Aggregator aggregator, Attribute<?> attribute) throws SearchException, AggregatorException {
        checkFrozen();
        aggregators.add(new AttributeAggegatorImpl(attribute, aggregator));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Query addOrder(Attribute<?> attribute, Direction direction) {
        checkFrozen();
        this.orderings.add(new OrderingImpl(attribute, direction));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Query maxResults(int maxResults) {
        checkFrozen();
        this.maxResults = maxResults;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Query add(Criteria criteria) {
        checkFrozen();

        if (criteria == null) {
            throw new NullPointerException("null criteria");
        }

        this.criteria.add(criteria);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Results execute() throws SearchException {
        return cache.executeQuery(snapshot());
    }

    /**
     * {@inheritDoc}
     */
    public Query end() {
        frozen = true;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public List<Ordering> getOrdering() {
        assertFrozen();
        return Collections.unmodifiableList(orderings);
    }

    /**
     * {@inheritDoc}
     */
    public Criteria getCriteria() {
        assertFrozen();
        return getEffectiveCriteriaCopy();
    }

    /**
     * {@inheritDoc}
     */
    public boolean requestsKeys() {
        assertFrozen();
        return includeKeys;
    }

    /**
     * {@inheritDoc}
     */
    public boolean requestsValues() {
        assertFrozen();
        return includeValues;
    }

    /**
     * {@inheritDoc}
     */
    public Cache getCache() {
        assertFrozen();
        return cache;
    }

    /**
     * {@inheritDoc}
     */
    public List<Attribute<?>> requestedAttributes() {
        assertFrozen();
        return Collections.unmodifiableList(this.includedAttributes);
    }

    /**
     * {@inheritDoc}
     */
    public int maxResults() {
        assertFrozen();
        return maxResults;
    }
    
    /**
     * {@inheritDoc}
     */
    public List<AttributeAggregator> getAggregators() {
        assertFrozen();
        return Collections.unmodifiableList(aggregators);
    }
    

    private Criteria getEffectiveCriteriaCopy() {
        int count = criteria.size();
        if (count == 0) {
            return new AlwaysMatchCriteria();
        } else if (count == 1) {
            return criteria.get(0);
        } else {
            return new And(criteria.toArray(new Criteria[count]));
        }

        // unreachable
    }

    private void assertFrozen() {
        if (!frozen) {
            throw new AssertionError("not frozen");
        }
    }

    private StoreQuery snapshot() {
        if (frozen) {
            return this;
        }

        return new StoreQueryImpl();
    }

    private void checkFrozen() {
        if (frozen) {
            throw new SearchException("Query is frozen and cannot be mutated");
        }
    }

    /**
     * StoreQuery implementation (essentially a snapshot of this (non-frozen) query builder
     */
    private class StoreQueryImpl implements StoreQuery {
        private final Criteria copiedCriteria = CacheQuery.this.getEffectiveCriteriaCopy();
        private final boolean copiedIncludeKeys = includeKeys;
        private final boolean copiedIncludeValues = includeValues;
        private final List<Attribute<?>> copiedAttributes = Collections.unmodifiableList(new ArrayList<Attribute<?>>(includedAttributes));
        private final int copiedMaxResults = maxResults;
        private final List<Ordering> copiedOrdering = Collections.unmodifiableList(new ArrayList<Ordering>(orderings));
        private final List<AttributeAggregator> copiedAggregators = Collections.unmodifiableList(new ArrayList<AttributeAggregator>(
                aggregators));

        public Criteria getCriteria() {
            return copiedCriteria;
        }

        public boolean requestsKeys() {
            return copiedIncludeKeys;
        }

        public boolean requestsValues() {
            return copiedIncludeValues;
        }

        public Cache getCache() {
            return cache;
        }

        public List<Attribute<?>> requestedAttributes() {
            return copiedAttributes;
        }

        public int maxResults() {
            return copiedMaxResults;
        }

        public List<Ordering> getOrdering() {
            return copiedOrdering;
        }

        public List<AttributeAggregator> getAggregators() {
            return copiedAggregators;
        }
    }

    /**
     * An attribute/direction pair
     */
    private static class OrderingImpl implements Ordering {

        private final Attribute<?> attribute;
        private final Direction direction;

        public OrderingImpl(Attribute<?> attribute, Direction direction) {
            if ((attribute == null) || (direction == null)) {
                throw new NullPointerException();
            }

            this.attribute = attribute;
            this.direction = direction;
        }

        public Attribute<?> getAttribute() {
            return attribute;
        }

        public Direction getDirection() {
            return direction;
        }
    }

    private static class AttributeAggegatorImpl implements AttributeAggregator {

        private final Attribute<?> attribute;
        private final Aggregator<?> aggregator;

        public AttributeAggegatorImpl(Attribute<?> attribute, Aggregator<?> aggregator) {
            if ((attribute == null) || (aggregator == null)) {
                throw new NullPointerException();
            }

            this.attribute = attribute;
            this.aggregator = aggregator;
        }

        public Attribute<?> getAttribute() {
            return attribute;
        }

        public Aggregator<?> getAggregator() {
            return aggregator;
        }

    }

}
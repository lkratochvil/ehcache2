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

package net.sf.ehcache.pool.sizeof.filter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A Filter that will filter fields, based on the {@link IgnoreSizeOf} annotation
 *
 * @author Chris Dennis
 */
public final class AnnotationSizeOfFilter implements SizeOfFilter {

    /**
     * {@inheritDoc}
     */
    public Collection<Field> filterFields(Class<?> klazz, Collection<Field> fields) {
        Collection<Field> removed = new ArrayList<Field>();
        for (Field f : fields) {
            if (f.isAnnotationPresent(IgnoreSizeOf.class)) {
                removed.add(f);
            }
        }
        if (removed.isEmpty()) {
            return fields;
        } else {
            Collection<Field> filtered = new ArrayList<Field>(fields);
            filtered.removeAll(removed);
            return filtered;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean filterClass(Class<?> klazz) {
        boolean classAnnotated = klazz.isAnnotationPresent(IgnoreSizeOf.class);
        Package pack = klazz.getPackage();
        boolean packageAnnotated = pack == null ? false : pack.isAnnotationPresent(IgnoreSizeOf.class);
        return !classAnnotated && !packageAnnotated;
    }
}

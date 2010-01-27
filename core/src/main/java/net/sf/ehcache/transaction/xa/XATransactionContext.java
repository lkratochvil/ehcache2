/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

package net.sf.ehcache.transaction.xa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.transaction.Transaction;
import javax.transaction.xa.Xid;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.Command;
import net.sf.ehcache.transaction.TransactionContext;

/**
 * @author Alex Snaps
 */
public class XATransactionContext implements TransactionContext {

    private final Set<Object> removedKeys = new HashSet<Object>();
    private final Set<Object> addedKeys = new HashSet<Object>();
    private final List<VersionAwareCommand> commands = new ArrayList<VersionAwareCommand>();
    private final ConcurrentMap<Object, Element> commandElements = new ConcurrentHashMap<Object, Element>();
    private final EhcacheXAStoreImpl storeImpl;
    private final Xid xid;
    private int sizeModifier;
    private transient Transaction transaction;


    public XATransactionContext(Xid xid, EhcacheXAStoreImpl storeImpl) {
        this.storeImpl = storeImpl;
        this.xid = xid;
    }
    
    public void initializeTransients(Transaction transaction) {
        this.transaction = transaction;
    }

    public Element get(Object key) {
        return removedKeys.contains(key) ? null : commandElements.get(key);
    }
    
    public Transaction getTransaction() {
        return this.transaction;
    }

    public boolean isRemoved(Object key) {
        return removedKeys.contains(key);
    }

    public Collection getAddedKeys() {
        return Collections.unmodifiableSet(addedKeys);
    }

    public Collection getRemovedKeys() {
        return Collections.unmodifiableSet(removedKeys);
    }

    public void addCommand(final Command command, final Element element) {
        Serializable key = null;
        if (element != null) {
            key = element.getKey();
        }
        VersionAwareWrapper wrapper = null;
        if (key != null) {
            long version = storeImpl.checkout(key, xid);
            wrapper = new VersionAwareWrapper(command, version, key);
            commandElements.put(element.getObjectKey(), element);
        } else {
            wrapper = new VersionAwareWrapper(command);
        }

        if (key != null) {
            if (command.isPut(key)) {
                boolean removed = removedKeys.remove(key);
                boolean added = addedKeys.add(key);
                if (removed || added && !storeImpl.getUnderlyingStore().containsKey(key)) {
                    sizeModifier++;
                }
            } else if (command.isRemove(key)) {
                removedKeys.add(key);
                if (addedKeys.remove(key) || storeImpl.getUnderlyingStore().containsKey(key)) {
                    sizeModifier--;
                }
            }
        }
        commands.add(wrapper);
    }

    public List<VersionAwareCommand> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public Set<Object> getUpdatedKeys() {
        
        Set<Object> keys = new HashSet<Object>();
        for (VersionAwareCommand command : getCommands()) {
            Object key = command.getKey();
            if (key != null) {
                keys.add(key);
            }
        }

        return Collections.unmodifiableSet(keys);
    }

    public int getSizeModifier() {
        return sizeModifier;
    }

}

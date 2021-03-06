/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.qpid.server.exchange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.qpid.exchange.ExchangeDefaults;
import org.apache.qpid.server.filter.AMQInvalidArgumentException;
import org.apache.qpid.server.filter.FilterManager;
import org.apache.qpid.server.filter.FilterSupport;
import org.apache.qpid.server.filter.Filterable;
import org.apache.qpid.server.message.InstanceProperties;
import org.apache.qpid.server.message.ServerMessage;
import org.apache.qpid.server.model.Binding;
import org.apache.qpid.server.model.ManagedObject;
import org.apache.qpid.server.model.ManagedObjectFactoryConstructor;
import org.apache.qpid.server.model.Queue;
import org.apache.qpid.server.queue.BaseQueue;
import org.apache.qpid.server.virtualhost.QueueManagingVirtualHost;

@ManagedObject( category = false, type = ExchangeDefaults.DIRECT_EXCHANGE_CLASS )
public class DirectExchange extends AbstractExchange<DirectExchange>
{

    private static final Logger _logger = LoggerFactory.getLogger(DirectExchange.class);

    private static final class BindingSet
    {
        private CopyOnWriteArraySet<Binding<?>> _bindings = new CopyOnWriteArraySet<>();
        private List<BaseQueue> _unfilteredQueues = new ArrayList<BaseQueue>();
        private Map<BaseQueue, FilterManager> _filteredQueues = new HashMap<>();

        public synchronized void addBinding(Binding<?> binding)
        {
            _bindings.add(binding);
            recalculateQueues();
        }

        public synchronized void removeBinding(Binding<?> binding)
        {
            _bindings.remove(binding);
            recalculateQueues();
        }

        public synchronized void updateBinding(Binding<?> binding)
        {
            recalculateQueues();
        }

        private void recalculateQueues()
        {
            List<BaseQueue> queues = new ArrayList<BaseQueue>(_bindings.size());
            Map<BaseQueue, FilterManager> filteredQueues = new HashMap<>();

            for(Binding<?> b : _bindings)
            {

                if(FilterSupport.argumentsContainFilter(b.getArguments()))
                {
                    try
                    {
                        FilterManager filter = FilterSupport.createMessageFilter(b.getArguments(), b.getQueue());
                        filteredQueues.put(b.getQueue(),filter);
                    }
                    catch (AMQInvalidArgumentException e)
                    {
                        _logger.warn("Binding ignored: cannot parse filter on binding of queue '"+b.getQueue().getName()
                                     + "' to exchange '" + b.getExchange().getName()
                                     + "' with arguments: " + b.getArguments(), e);
                    }

                }
                else
                {

                    if(!queues.contains(b.getQueue()))
                    {
                        queues.add(b.getQueue());
                    }
                }
            }
            _unfilteredQueues = queues;
            _filteredQueues = filteredQueues;
        }


        public List<BaseQueue> getUnfilteredQueues()
        {
            return _unfilteredQueues;
        }

        public CopyOnWriteArraySet<Binding<?>> getBindings()
        {
            return _bindings;
        }

        public boolean hasFilteredQueues()
        {
            return !_filteredQueues.isEmpty();
        }

        public Map<BaseQueue,FilterManager> getFilteredQueues()
        {
            return _filteredQueues;
        }
    }

    private final ConcurrentMap<String, BindingSet> _bindingsByKey =
            new ConcurrentHashMap<String, BindingSet>();

    @ManagedObjectFactoryConstructor
    public DirectExchange(final Map<String, Object> attributes, final QueueManagingVirtualHost<?> vhost)
    {
        super(attributes, vhost);
    }

    @Override
    public List<? extends BaseQueue> doRoute(ServerMessage payload,
                                             final String routingKey,
                                             final InstanceProperties instanceProperties)
    {

        BindingSet bindings = _bindingsByKey.get(routingKey == null ? "" : routingKey);

        if(bindings != null)
        {
            List<BaseQueue> queues = bindings.getUnfilteredQueues();

            if(bindings.hasFilteredQueues())
            {
                Set<BaseQueue> queuesSet = new HashSet<BaseQueue>(queues);
                Filterable filterable = Filterable.Factory.newInstance(payload, instanceProperties);

                Map<BaseQueue, FilterManager> filteredQueues = bindings.getFilteredQueues();
                for(Map.Entry<BaseQueue, FilterManager> entry : filteredQueues.entrySet())
                {
                    if(!queuesSet.contains(entry.getKey()))
                    {
                        FilterManager filter = entry.getValue();
                        if(filter.allAllow(filterable))
                        {
                            queuesSet.add(entry.getKey());
                        }
                    }
                }
                if(queues.size() != queuesSet.size())
                {
                    queues = new ArrayList<>(queuesSet);
                }
            }
            return queues;
        }
        else
        {
            return Collections.emptyList();
        }


    }

    @Override
    protected void onBindingUpdated(final Binding<?> binding, final Map<String, Object> oldArguments)
    {
        String bindingKey = binding.getBindingKey();
        Queue<?> queue = binding.getQueue();

        assert queue != null;
        assert bindingKey != null;

        BindingSet bindings = _bindingsByKey.get(bindingKey);
        bindings.updateBinding(binding);
    }

    protected void onBind(final Binding<?> binding)
    {
        String bindingKey = binding.getBindingKey();
        Queue<?> queue = binding.getQueue();

        assert queue != null;
        assert bindingKey != null;

        BindingSet bindings = _bindingsByKey.get(bindingKey);

        if(bindings == null)
        {
            bindings = new BindingSet();
            BindingSet newBindings;
            if((newBindings = _bindingsByKey.putIfAbsent(bindingKey, bindings)) != null)
            {
                bindings = newBindings;
            }
        }

        bindings.addBinding(binding);

    }

    protected void onUnbind(final Binding<?> binding)
    {
        assert binding != null;

        BindingSet bindings = _bindingsByKey.get(binding.getBindingKey());
        if(bindings != null)
        {
            bindings.removeBinding(binding);
        }

    }

}

/*
 * Copyright (c) 2008-2013 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.queue;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class WeightedFairQueue<T, E> implements BlockingQueue<E> {

    public interface WFQElementInformationProvider<T, E> {
        public int getRequestCost(E element);
        public T getQualityClass(E element);
        public int getWeight(T element);
    }

    protected Map<T, Queue<E>>                    queues;

    protected Map<T, Integer>                     requestCount;

    protected int                                 capacity;

    protected WFQElementInformationProvider<T, E> elementInformationProvider;

    public WFQElementInformationProvider<T, E> getElementInformationProvider() {
        return elementInformationProvider;
    }

    public WeightedFairQueue(int capacity, WFQElementInformationProvider<T, E> elementInformationProvider) {
        this.queues = new HashMap<T, Queue<E>>();
        this.requestCount = new HashMap<T, Integer>();
        this.capacity = capacity;
        this.elementInformationProvider = elementInformationProvider;
    }

    @Override
    public boolean add(E e) {
        boolean result = this.offer(e);
        if(!result)
            throw new IllegalStateException("No remaining queue capacity");
        else
            return result;
    }

    @Override
    public boolean offer(E e) {
        if(this.remainingCapacity() > 0)
            return this.getQueue(e).offer(e);
        else
            return false;
    }

    @Override
    public E remove() {
        Queue<E> resultQueue = this.getNextQueue();

        if (resultQueue != null)
            return resultQueue.remove();
        else
            return null;
    }

    @Override
    public E poll() {
        Queue<E> resultQueue = this.getNextQueue();

        if (resultQueue != null)
            return resultQueue.poll();
        else
            return null;
    }

    @Override
    public E element() {
        Queue<E> resultQueue = this.getNextQueue();

        if (resultQueue != null)
            return resultQueue.element();
        else
            return null;
    }

    @Override
    public E peek() {
        Queue<E> resultQueue = this.getNextQueue();

        if (resultQueue != null)
            return resultQueue.peek();
        else
            return null;
    }

    @Override
    public void put(E e) throws InterruptedException {
        while(!this.getQueue(e).offer(e)) {
            Thread.sleep(1);
        }
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        // TODO(ckleineweber): Implement offer() method
        return false;
    }

    @Override
    public E take() throws InterruptedException {
        E element;
        Queue<E> q = this.getNextQueue();

        if (q == null)
            return null;

        while((element = q.poll()) == null) {
            Thread.sleep(1);
        }
        this.countRequest(element);
        return element;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        // TODO(ckleineweber): return after timeout
        return this.getNextQueue().poll();
    }

    @Override
    public int remainingCapacity() {
        return this.capacity - this.size();
    }

    @Override
    public boolean remove(Object o) {
        return this.getQueue((E) o).remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        boolean result = true;
        Iterator<?> it = c.iterator();
        while(it.hasNext() && result)
            result &= this.contains(it.next());
        return result;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean result = false;
        for(E element: c)
            result |= this.add(element);
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = false;
        for(Object element: c)
            result |= this.remove((E) element);
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean result = false;
        Iterator<E> it = this.iterator();
        while(it.hasNext()) {
            E element = it.next();
            if(!c.contains(element)) {
                this.remove(element);
                result = true;
            }
        }
        return result;
    }

    @Override
    public void clear() {
        for(Queue<E> q: this.queues.values())
            q.clear();
    }

    @Override
    public boolean contains(Object o) {
        return this.getQueue((E) o).contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            E lastElement = null;

            public void remove() {
                if(lastElement != null)
                    getQueue(lastElement).remove(lastElement);
            }

            public boolean hasNext() {
                return (size() > 0);
            }

            public E next() {
                lastElement = getNextQueue().peek();
                return lastElement;
            }
        };
    }

    @Override
    public Object[] toArray() {
        Object[] result = new Object[this.size()];
        Iterator<E> it = this.iterator();
        int i = 0;
        while(it.hasNext()) {
            result[i] = it.next();
            i++;
        }
        return result;
    }

    @Override
    public <E> E[] toArray(E[] a) {
        if(a.length >= size()) {
            Iterator<?> it = iterator();
            for(int i = 0; i < size(); i++) {
                if(it.hasNext())
                    a[i] = (E) it.next();
            }
            return a;
        } else {
            return (E[]) toArray();
        }
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return this.drainTo(c, this.size());
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        int result = 0;
        while(!this.isEmpty() && result < maxElements) {
            try {
                c.add(this.take());
                result++;
            } catch(InterruptedException e) {}
        }
        return result;
    }

    @Override
    public boolean isEmpty() {
        for(Queue q: queues.values()) {
            if(!q.isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public int size() {
        int size = 0;
        for(Queue<E> q: this.queues.values()) {
            size += q.size();
        }
        return size;
    }

    protected void countRequest(E element) {
        int count = 0;
        T qualityClass = this.elementInformationProvider.getQualityClass(element);
        if(this.requestCount.containsKey(qualityClass)) {
            count = this.requestCount.get(qualityClass);
        }
        count += this.elementInformationProvider.getRequestCost(element);
        this.requestCount.put(qualityClass, count);
    }

    protected Queue<E> getQueue(E element) {
        T qualityClass = this.elementInformationProvider.getQualityClass(element);
        if(this.queues.containsKey(qualityClass)) {
            return this.queues.get(qualityClass);
        } else {
            Queue<E> q = new ConcurrentLinkedQueue<E>();
            this.queues.put(qualityClass, q);
            return q;
        }
    }

    protected Queue<E> getNextQueue() {
        T resultClass = null;

        for(T c: this.queues.keySet()) {
            if(resultClass == null && !this.queues.get(c).isEmpty()) {
                resultClass = c;
            } else if (resultClass != null) {
                if(((getProportion(c) - getCurrentProportion(c)) >
                        (getProportion(resultClass) - getCurrentProportion(resultClass))) &&
                        !this.queues.get(c).isEmpty()) {
                    resultClass = c;
                }
            } else if (!this.queues.get(c).isEmpty()) {
                resultClass = c;
            }
        }

        if(resultClass == null)
            return null;
        else
            return this.queues.get(resultClass);
    }

    protected double getProportion(T qualityClass) {
        double sum = 0.0;

        for(T c: this.requestCount.keySet()) {
            sum += (double) this.elementInformationProvider.getWeight(c);
        }

        return (double) this.elementInformationProvider.getWeight(qualityClass) / sum;
    }

    protected double getCurrentProportion(T qualityClass) {
        double sum = 0.0;

        if(!this.requestCount.containsKey(qualityClass) || !this.queues.containsKey(qualityClass))
            return sum;

        for(T c: this.requestCount.keySet()) {
            sum += (double) this.requestCount.get(c);
        }

        return (double) this.requestCount.get(qualityClass) / sum;
    }
}

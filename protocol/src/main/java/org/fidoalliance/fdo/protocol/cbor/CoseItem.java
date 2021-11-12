package org.fidoalliance.fdo.protocol.cbor;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Wraps Cose Array as Tagged Item.
 */
public class CoseItem implements List<Object> {

  private List<Object> cose;

  protected CoseItem(List<Object> cose) {
    this.cose = cose;
  }

  @Override
  public int size() {
    return cose.size();
  }

  @Override
  public boolean isEmpty() {
    return cose.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return cose.contains(o);
  }

  @Override
  public Iterator<Object> iterator() {
    return cose.iterator();
  }

  @Override
  public Object[] toArray() {
    return cose.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return cose.toArray(a);
  }

  @Override
  public boolean add(Object o) {
    return cose.add(o);
  }

  @Override
  public void add(int index, Object element) {
    cose.add(index, element);
  }

  @Override
  public boolean remove(Object o) {
    return cose.remove(o);
  }

  @Override
  public Object remove(int index) {
    return cose.remove(index);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return cose.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<?> c) {
    return cose.addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<?> c) {
    return cose.addAll(index, c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return cose.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return cose.retainAll(c);
  }

  @Override
  public void clear() {
    cose.clear();
  }

  @Override
  public Object get(int index) {
    return cose.get(index);
  }

  @Override
  public Object set(int index, Object element) {
    return cose.set(index, element);
  }

  @Override
  public int indexOf(Object o) {
    return cose.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return cose.lastIndexOf(o);
  }

  @Override
  public ListIterator<Object> listIterator() {
    return cose.listIterator();
  }

  @Override
  public ListIterator<Object> listIterator(int index) {
    return cose.listIterator(index);
  }

  @Override
  public List<Object> subList(int fromIndex, int toIndex) {
    return cose.subList(fromIndex, toIndex);
  }
}

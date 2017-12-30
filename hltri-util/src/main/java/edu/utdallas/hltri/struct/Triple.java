package edu.utdallas.hltri.struct;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by rmm120030 on 8/16/16.
 */
public class Triple<A,B,C> implements Serializable {
  private static final long serialVersionUID = 1L;

  private final A a;
  private final B b;
  private final C c;

  public Triple(A a, B b, C c) {
    this.a = a;
    this.b = b;
    this.c = c;
  }

  public static <A,B,C> Triple<A,B,C> of(A a, B b, C c) {
    return new Triple<>(a, b, c);
  }


  public A first() { return a; }
  public B second() { return b; }
  public C third() { return c; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;
    return Objects.equals(a, triple.a) &&
        Objects.equals(b, triple.b) &&
        Objects.equals(c, triple.c);
  }

  @Override
  public int hashCode() {
    return Objects.hash(a, b, c);
  }
}

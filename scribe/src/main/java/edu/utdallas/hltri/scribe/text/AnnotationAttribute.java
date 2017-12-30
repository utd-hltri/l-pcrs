//package edu.utdallas.hltri.scribe.text;
//
///**
// * Created by travis on 9/9/15.
// */
//
//import com.google.common.base.Throwables;
//import com.google.common.reflect.TypeToken;
//
//import java.lang.reflect.Constructor;
//import java.lang.reflect.InvocationTargetException;
//import java.util.Collection;
//
//import edu.utdallas.hltri.scribe.text.annotation.Annotation;
//import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;
//import edu.utdallas.hltri.util.Unsafe;
//
//package edu.utdallas.hltri.scribe.text;
//
//import com.google.common.base.Throwables;
//import com.google.common.reflect.TypeToken;
//
//import edu.utdallas.hltri.scribe.text.annotation.Annotation;
//import edu.utdallas.hltri.scribe.text.annotation.AnnotationType;
//import edu.utdallas.hltri.util.Unsafe;
//
//import java.lang.reflect.Constructor;
//import java.lang.reflect.InvocationTargetException;
//import java.util.Collection;
//import java.util.Optional;
//import java.util.function.BiConsumer;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//import java.util.stream.StreamSupport;
//
///**
// * Created by travis on 8/14/14.
// */
//public abstract class AnnotationAttribute<S extends Annotation<S>, T> {
//  public final String   name;
//
//  protected AnnotationAttribute(String name) {
//    this.name = name;
//  }
//
//  abstract public void set(S annotation, T value);
//
//  abstract public T get(S annotation);
//
//  public static <S extends Annotation<S>, T extends Annotation<T>> AnnotationAttribute<S, T> typed(String name, AnnotationType<T> type) {
//    return new AnnotationAttribute<S, T>(name) {
//      @Override
//      public void set(S annotation, T value) {
//        annotation.asGate().getFeatures().put(name, value.getId());
//      }
//
//      @Override
//      public T get(S annotation) {
//        final int id = (int) annotation.asGate().getFeatures().get(name);
//        final Document<?> doc = annotation.getDocument();
//
//        return doc.getAnnotationById(id, type).get();
//      }
//    };
//  }
//
//  public static <S extends Annotation<S>, T> AnnotationAttribute<S, T> typed(String name, Class<T> type) {
//    if (Annotation.class.isAssignableFrom(type)) {
//      throw new UnsupportedOperationException("Cannot create typed attribute for annotation. Use AnnotationType instead.");
//    }
//
//    return new AnnotationAttribute<S, T>(name) {
//      @Override
//      public void set(S annotation, T value) {
//        annotation.asGate().getFeatures().put(name, value);
//      }
//
//      @Override
//      public T get(S annotation) {
//        return type.cast(annotation.asGate().getFeatures().get(name));
//      }
//    };
//  }
//
//
//  public static <S extends Annotation<S>, T, C extends Collection<T>> AnnotationAttribute<S, C> inferred(String name) {
//    final TypeToken<C> collectionToken = new TypeToken<C>(AnnotationAttribute.class) {
//    };
//    final Class<? super C> collectionClass = collectionToken.getRawType();
//
//    final TypeToken<T> typeToken = new TypeToken<T>(AnnotationAttribute.class) {
//    };
//    final Class<? super T> typeClass = typeToken.getRawType();
//
//    if (Annotation.class.isAssignableFrom(typeClass)) {
//      return new AnnotationAttribute<S, C>(name) {
//        @Override
//        public void set(S annotation, C value) {
//          final int[]
//              ids =
//              value.stream().mapToInt(x -> Annotation.class.cast(x).getId()).toArray();
//          annotation.asGate().getFeatures().put(name, ids);
//        }
//
//        @Override
//        public C get(S annotation) {
//          try {
//            final C clazz = Unsafe.cast(collectionClass.newInstance());
//            final int[] ids = Unsafe.cast(annotation.asGate().getFeatures().get(name));
//            final Document<?> doc = annotation.getDocument();
//            for (int id : ids) {
//              final gate.Annotation gateAnn = doc.getUnsafeAnnotationById(id).get().asGate();
//              final Constructor<? super T>
//                  constructor =
//                  typeClass.getConstructor(doc.getClass(), gateAnn.getClass());
//              clazz.add(Unsafe.cast(constructor.newInstance(doc, gateAnn)));
//            }
//            return clazz;
//          } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
//            throw Throwables.propagate(e);
//          }
//        }
//      };
//    } else {
//      return new AnnotationAttribute<S, C>(name) {
//        @Override
//        public void set(S annotation, C value) {
//          annotation.asGate().getFeatures().put(name, value);
//        }
//
//        @Override
//        public C get(S annotation) {
//          return Unsafe.cast(collectionClass.cast(annotation.asGate().getFeatures().get(name)));
//        }
//      };
//    }
//  }
//
//  @Override
//  public String toString() {
//    return name;
//  }
//}
//
//

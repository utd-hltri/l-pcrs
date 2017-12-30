package edu.utdallas.hltri.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.utdallas.hltri.logging.Logger;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Created by travis on 7/23/15.
 */
public class ImmutibleIntIdenitifer<T> extends IntIdentifier<T> {
  private static final Logger log = Logger.get(ImmutibleIntIdenitifer.class);

//  public ImmutibleIntIdenitifer(List<T> items, TObjectIntMap<T> ids) {
//    super(
//        Collections.unmodifiableList(items),
//        TCollections.unmodifiableMap(ids)
//    );
//    this.isImmutable = true;
//  }

  public ImmutibleIntIdenitifer(List<T> items, Object2IntMap<T> ids) {
    super(
        Collections.unmodifiableList(items),
        Object2IntMaps.unmodifiable(ids)
    );
    this.isImmutable = true;
  }
}

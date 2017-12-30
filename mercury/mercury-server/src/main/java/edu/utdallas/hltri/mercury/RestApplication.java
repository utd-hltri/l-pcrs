package edu.utdallas.hltri.mercury;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * Created by travis on 4/5/17.
 */

@ApplicationPath("/api")
public class RestApplication extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    final Set<Class<?>> resources = new HashSet<>();
    resources.add(SearchResource.class);
    return resources;
  }
}

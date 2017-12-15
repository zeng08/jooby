package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Err;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.Status;
import org.jooby.funzy.Throwing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class RouterImpl implements Router {

  private static final int _10 = 10;
  private RouteImpl[] routes = new RouteImpl[_10];
  private int routeSize;
  private ConcurrentHashMap<Integer, RouteImpl[]> cache = new ConcurrentHashMap<>();
  private Route.ErrHandler err;

  public Route.Chain chain(String method, String path) {
    return new RouteChainImpl(chainArray(cache, routes, method, path), err);
  }

  @Override public Stream<Route> routes() {
    return Arrays.stream(routes);
  }

  public RouterImpl start() {
    if (routeSize < routes.length) {
      // shrink route array:
      routes = Arrays.copyOf(routes, routeSize);
    }
    err(defaultErrHandler());
    return this;
  }

  private static RouteImpl[] chainArray(ConcurrentHashMap<Integer, RouteImpl[]> cache,
      RouteImpl[] routes, String method, String path) {
    return cache.computeIfAbsent(hash(method, path), key -> {
      List<RouteImpl> matches = new ArrayList<>(_10);
      boolean methodNotAllowed = false;
      for (int r = 0; r < routes.length; r++) {
        RouteImpl it = routes[r];
        PathPattern.Result result = it.pattern.test(path);
        if (result.matches) {
          if (it.method.equals(method) || it.method.equals("*")) {
            matches.add(it);
          } else {
            methodNotAllowed = true;
          }
        }
      }
      int size = matches.size();
      if (size > 0) {
        RouteImpl route = matches.get(size - 1);
        if (route.endpoint) {
          return matches.toArray(new RouteImpl[matches.size()]);
        }
      }
      return new RouteImpl[]{RouteImpl
          .fallback(null, method, path, fallback(path, methodNotAllowed))};
    });
  }

  @Override public Route define(String method, String pattern, Route.Filter handler) {
    RouteImpl route = new RouteImpl(method, pattern, handler);
    if (routeSize >= routes.length) {
      routes = Arrays.copyOf(routes, routeSize + _10);
    }
    routes[routeSize++] = route;
    return route;
  }

  @Override public Route.ErrHandler err() {
    return this.err;
  }

  @Override public Router err(Route.ErrHandler handler) {
    if (this.err == null) {
      this.err = handler;
    } else {
      this.err = this.err.then(handler);
    }
    return this;
  }

  @Override public <T> Router with(Throwing.Consumer2<Context, T> consumer, Runnable action) {
    int beforeSize = routeSize;
    action.run();
    for (int i = beforeSize; i < routeSize; i++) {
      RouteImpl route = routes[i];
      if (route.endpoint) {
        Route.Handler handler = (Route.Handler) route.handler;
        route.handler = mapHandler(handler, consumer);
      }
    }
    return this;
  }

  private static <T> Route.Handler mapHandler(Route.Handler handler,
      Throwing.Consumer2<Context, T> consumer) {
    return new Route.Handler() {
      @Override public void handle(@Nonnull Context ctx, @Nonnull Route.Chain chain)
          throws Throwable {
        Object value = handle(ctx);
        consumer.accept(ctx, (T) value);
      }

      @Override public Object handle(@Nonnull Context ctx) throws Throwable {
        return handler.handle(ctx);
      }
    };
  }

  private static Route.ErrHandler defaultErrHandler() {
    return (ctx, problem) -> {
      try {
        Logger log = LoggerFactory.getLogger(Err.class);
        log.error("execution of {} {} resulted in exception", ctx.method(), ctx.path(), problem);
        ctx.reset();
        ctx.status(problem.status());
        ctx.send(problem.getMessage());
      } catch (Throwable x) {
        Logger log = LoggerFactory.getLogger(Route.ErrHandler.class);
        log.error("execution of error handler resulted in unexpected exception", x);
      }
    };
  }

  private static Route.Filter fallback(String path, boolean methodNotAllowed) {
    return (ctx, chain) -> {
      if (path.equals("/favicon.ico")) {
        // not found
        ctx.status(404).end();
      } else {
        if (methodNotAllowed) {
          throw new Err(Status.METHOD_NOT_ALLOWED);
        }
        throw new Err(Status.NOT_FOUND);
      }
    };
  }

  private static Integer hash(String method, String path) {
    return Integer.valueOf(31 * (31 + method.hashCode()) + path.hashCode());
  }
}

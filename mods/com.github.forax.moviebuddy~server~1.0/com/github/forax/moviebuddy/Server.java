package com.github.forax.moviebuddy;

import static com.github.forax.moviebuddy.JsonStream.asStream;
import static java.nio.file.Paths.get;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.lang.Integer.parseInt;
import static java.util.regex.Pattern.compile;
import static java.lang.Math.sqrt;
import static com.github.forax.moviebuddy.User.findUserById;
import static com.github.forax.moviebuddy.Movie.findMovieById;

import java.io.IOError;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class Server extends Verticle {
  private final static String FILES_PREFIX = "../..";
  
  @Override
  public void start() {
    List<Movie> movies;
    List<User> users;
    try {
      movies = asStream(get("db/movies.json")).map(Movie::parse).collect(toList());
      users = asStream(get("db/users.json")).map(User::parse).collect(toList());
    } catch (IOException e) {
      throw new IOError(e);
    }
    
    // sort for binary search
    movies.sort(null);
    users.sort(null);
    
    HttpServer server = vertx.createHttpServer();
    RouteMatcher route = new RouteMatcher();
    
    route.noMatch(req -> {
      String path = req.path();
      if (path.equals("/") || path.contains("..")) {
        path = "/index.html";
      }
      req.response().sendFile(FILES_PREFIX + "/public/" + path);
    });
    
    route.get("/movies", req -> {
      req.response().sendFile(FILES_PREFIX + "/db/movies.json");
    });
    
    route.get("/movies/:id", req -> {
      int id = parseInt(req.params().get("id"));
      /* linear search
      req.response().end(
          movies.stream()
                .filter(movie -> movie._id == id)
                .findFirst()
                  .map(Movie::toString)
                  .orElse(""));
      */
      req.response().end(findMovieById(id, movies).map(Movie::toString).orElse(""));
    });
      
    
    route.get("/movies/search/title/:title/:limit", req -> {
      Pattern pattern = compile(req.params().get("title").toLowerCase());
      req.response().end(
          movies.stream()
                .filter(movie -> pattern.matcher(movie.title).find())
                .map(Movie::toString)
                .limit(parseInt(req.params().get("limit")))
                .collect(joining(",", "[", "]")));
    });

    route.get("/movies/search/actors/:actors/:limit", req -> {
      Pattern pattern = compile(req.params().get("actors").toLowerCase());
      req.response().end(
          movies.stream()
                .filter(movie -> pattern.matcher(movie.actors).find())
                .map(Movie::toString)
                .limit(parseInt(req.params().get("limit")))
                .collect(joining(",", "[", "]")));
    });
     
    route.get("/movies/search/genre/:genre/:limit", req -> {
      Pattern pattern = compile(req.params().get("genre").toLowerCase());
      req.response().end(
          movies.stream()
                .filter(movie -> pattern.matcher(movie.genre).find())
                .map(Movie::toString)
                .limit(parseInt(req.params().get("limit")))
                .collect(joining(",", "[", "]")));
    });
    
    route.get("/users", req -> {
      req.response().sendFile(FILES_PREFIX + "/db/users.json");
    });
    
    route.get("/users/:id", req -> {
      int id = parseInt(req.params().get("id"));
      req.response().end(User.findUserById(id, users).map(User::toString).orElse(""));
    });
    
    route.get("/users/search/:name/:limit", req -> {
      Pattern pattern = compile(req.params().get("name").toLowerCase());
      req.response().end(
          users.stream()
                .filter(user -> pattern.matcher(user.name).find())
                .map(User::toString)
                .limit(parseInt(req.params().get("limit")))
                .collect(joining(",", "[", "]")));
    });
    
    route.post("/rates", req -> {
      req.bodyHandler(buffer -> {
        String body = buffer.getString(0, buffer.length());
        JsonObject userRate = new JsonObject(body);
        User user = findUserById(parseInt(userRate.getString("userId")), users).get();
        Movie movie = findMovieById(parseInt(userRate.getString("movieId")), movies).get();
        if (user.rates == null) user.rates = new HashMap<>();
        user.rates.put(movie, parseInt(userRate.getString("rate")));
        req.response().setStatusCode(201).end();
      });
    });
    
    route.get("/users/share/:userid1/:userid2", req -> {
      User user1 = findUserById(parseInt(req.params().get("userid1")), users).get();
      User user2 = findUserById(parseInt(req.params().get("userid2")), users).get();
      if (user1.rates == null || user2.rates == null) {
        req.response().end("[]");
        return;
      }
      HashSet<Movie> set = new HashSet<>();
      set.addAll(user1.rates.keySet());
      set.retainAll(user2.rates.keySet());
      req.response().end(set.stream().map(Movie::toString).collect(joining(",", "[", "]")));
    });
    
    route.get("/users/distance/:userid1/:userid2", req -> {
      User user1 = findUserById(parseInt(req.params().get("userid1")), users).get();
      User user2 = findUserById(parseInt(req.params().get("userid2")), users).get();
      if (user1 == user2 || user1.rates == null || user2.rates == null) {
        req.response().end("0");
        return;
      }
      double[] sum_of_squares = { 0.0 };
      user1.rates.forEach((movie, rate1) -> {
        Integer rate2 = user2.rates.get(movie);
        if (rate2 != null) {
          double diff = rate1 - rate2;
          sum_of_squares[0] += diff * diff;
        }
      });
      req.response().end(Double.toString(1.0 / (1.0 + sqrt(sum_of_squares[0]))));
    });
    
    server.requestHandler(route).listen(3000, "localhost");
    container.logger().info("Listening on 3000 ...");
  }
}
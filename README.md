movie_buddy
===========

A movie buddy application for Devoxx 2014 (Vertx + Java 8)


Install movie_buddy project
---
Just clone this repo !
```
git clone git@github.com:forax/movie_buddy.git
```

Setup Vertx shell script
---
Vertx uses the vertx shell script, so you have to put it in your path
```
export PATH=[PATH TO MOVIE BUDDY]/deps/vert.x-2.1RC2/bin:$PATH
```


Run
---
Use terx command to run the movie_buddy module
```
vertx runmod com.github.forax.moviebuddy~server~1.0/
```

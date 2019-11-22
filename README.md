more-lambdas
=======================
[![Build Status](https://travis-ci.org/PhantomThief/more-lambdas-java.svg?branch=master)](https://travis-ci.org/PhantomThief/more-lambdas-java)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.phantomthief/more-lambdas)](https://search.maven.org/artifact/com.github.phantomthief/more-lambdas/)

Some useful lambda implements for Java 8.

## Usage

advanced distinct
```Java
List<String> list = ....;
List<String> result = list.stream()
	.filter(MorePredicates.distinctUsing(Object::hashCode)) // distinct using hashCode
	.collect(Collectors.toList());
```

more collectors
```Java
Map<Integer, String> map = ...;
map.entrySet().stream()
	... // some ops
	.collect(MoreCollectors.toMap()); // no need to map key and value again if it's an entry stream.
```

also, there is simple HPPC support in MoreCollectors.
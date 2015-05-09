more-lambdas [![Build Status](https://travis-ci.org/PhantomThief/more-lambdas-java.svg?branch=master)](https://travis-ci.org/PhantomThief/more-lambdas-java)
=======================

Some useful lambda implements for Java 8.

## Get Started

```xml
<dependency>
    <groupId>com.github.phantomthief</groupId>
    <artifactId>more-lambdas</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Usage

advanced distinct
```Java
List<String> list = ....;
List<String> result = list.stream() //
	.filter(MorePredicates.distinctUsing(Object::hashCode)) // distinct using hashCode
	.collect(Collectors.toList());
```

more collectors
```Java
Map<Integer, String> map = ...;
map.entrySet().stream() //
	... // some ops
	.collect(MoreCollectors.toMap()); // no need to map key and value again if it's an entry stream.
```

also, there is simple HPPC support in MoreCollectors.
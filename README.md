# FolderTextSearch

Kotlin library for simple text search inside chosen folder on file system.

## Requirements

This library is supposed to consist of two parts: text index builder and search query executor.

Text index builder should:
 - Be able to build a text index for a given folder in a file system.
 - Show progress while building the index.
 - Build the index using several threads in parallel.
 - Be cancellable. It should be possible to interrupt indexing.
 - (Optional) Be incremental. It would be nice if the builder would be able to listen to the 
file system changes and update the index accordingly.

Search query executor should:
 - Find a position in files for a given string.
 - Be able to process search requests in parallel.

Please also cover the library with a set of unit-tests. 
Your code should not use third-party indexing libraries. To implement the library, 
you can use any JVM languages and any build systems, but we would appreciate you choosing Kotlin and Gradle.

## Getting Started

Download links:

SSH clone URL: ssh://git@git.jetbrains.space/podmev/main/FolderTextSearch.git

HTTPS clone URL: https://git.jetbrains.space/podmev/main/FolderTextSearch.git



These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

## Prerequisites

What things you need to install the software and how to install them.

```
Examples
```

## Deployment

Add additional notes about how to deploy this on a production system.

## Resources

Add links to external resources for this project, such as CI server, bug tracker, etc.

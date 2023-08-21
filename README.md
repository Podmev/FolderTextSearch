# FolderTextSearch

Kotlin library for simple text search inside chosen folder on file system.

# Table of contents

1. [Requirements](#requirements)
2. [Getting started](#getting-started)
3. [Features](#features)
    - [Creating index for folder](#index-feature)
    - [Searching string in folder](#search-feature)
    - [Creating index for folder and searching string in folder as one request](#index-search-feature)
    - [Incremental indexing](#incremental-feature)
    - [Browsing & modification stored index](#modification-index-feature)
   - [Choosing trigramMap type](#choosing-trigram-map-type)
4. [Limitations](#limitations)
5. [Examples](#examples)
6. [Roadmap (unordered)](#roadmap)
7. [Algorithms inside](#algorithms)
    - [Indexing & Searching in general](#indexing-searching-algorithms)
    - [Indexing](#indexing-algorithms)
    - [Searching](#searching-algorithms)
    - [Incremental indexing](#incremental-algorithms)
8. [Implementations](#implementations)
9. [Tests](#tests)
    - [Big tests](#big-tests)
    - [Setup big test](#setup-big-test)
10. [Used stack](#used-stack)
11. [Continuous integration](#ci)

## <a id="requirements"></a> Requirements

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

## <a id="getting-started"></a> Getting Started

Download links:

SSH clone URL: ssh://git@git.jetbrains.space/podmev/main/FolderTextSearch.git

HTTPS clone URL: https://git.jetbrains.space/podmev/main/FolderTextSearch.git

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

## <a id="features"></a> Features

### <a id="index-feature"></a> Creating index for folder

- Supports many folders to store
- Indexing one folder per time
- Shows progress
    - calculated by number of indexed files
- Can be cancelled
    - on cancel progress stays where it was on that moment of cancelling
- There is state during indexing with various changing data
- At any time of indexing can be taken immutable snapshot of the state
- Synchronous and asynchronous api
- Returns as a result list of indexed paths in folder (can be ignored in most of times)
- At any time of process it is available to get buffer of new visited and indexed files
- Api for printing current state of indexing with progress and all statistics
- Ignore not indexable files, like archives, executables, pictures, etc.
- Extra statistics in indexing state:
    - time (started, finished(or current), total),
    - number of files (visited, indexed, total),
    - status (not started, in progress, cancelling, cancelled, finished, failed),
    - reason of fail

### <a id="search-feature"></a> Searching string in folder

- Supports many requests simultaneously
- Can work only when there is no indexing process
- Cannot search if there is no index for folder
- Shows progress
    - calculated by bytes of parsed lines from total selected files by index (updates after each line)
- Can be cancelled
    - on cancel progress stays where it was on that moment of cancelling
- There is state during indexing with various changing data
- At any time of indexing can be taken immutable snapshot of the state
- Synchronous and asynchronous api
- Returns as a result list of token matches: class with 3 fields:
    - file path, where string was found,
    - line number (1-based),
    - position in the line (1-based)
- At any time of process it is available to get buffer of newfound token matches
- At any time of process it is available to get buffer of new visited files, selected by index
- Api for printing current state of searching with progress and all statistics
- Extra statistics in searching state:
    - time (started, finished(or current), total),
    - number of files (visited, parsed, total) - only files selected by index,
    - byte size of files (visited, parsed, total) - only files selected by index,
    - status (not started, in progress, cancelling, cancelled, finished, failed),
    - reason of fail

### <a id="index-search-feature"></a> Creating index for folder and searching string in folder as one request

- Shows progress
    - calculated of 2 phases: indexing and searching progress
- Can be cancelled
    - on cancel progress stays where it was on that moment of cancelling
- There is aggregated state during executing, which contains both parts: indexing and searching state
- At any time of it can be taken immutable snapshot of the state
- Synchronous and asynchronous api
- Api for printing current state of indexing&searching with progress and all statistics
- Extra statistics is a combination of statistic of indexing and searching phases

### <a id="incremental-feature"></a> Incremental indexing

- Reading events from the file system
- Works in background
- Can be started and stopped any time and repeated many times
- Supports creating new subtree of files in folder
- Supports loading all changes in indexing folder, which were made after last creating index.
- Supports incremental indexing of all current indexing folders at the same time

### <a id="modification-index-feature"></a> Browsing & modification stored index

- Get all folders with calculated index
- Delete index for all folders
- Delete index for specific folder
- Check if there is index for folder

### <a id="choosing-trigram-map-type"></a> Choosing trigramMap type

- For TrigramSearchApi there are options for using internal structure - TrigramMap
- TrigramMap has 2 implementations: simple and timed
- Simple is a bit faster, but doesn't support incremental indexing
- Timed is a bit slower and does support incremental indexing
- By default, it is timed implementation
- Implementation can be chosen in constructor of TrigramSearchApi

## <a id="limitations"></a> Limitations

- No multi-string search
- No support of searching string less than 3 characters
- No support of searching in files with complete or partial "bad" encoding
- No progress at first, while indexing or searching, when it is initially going to browse files, but it is small part of
  time
- Indexing folder and subfolder works independently
- No filters for search any kind

## <a id="examples"></a> Examples

A lot of real usage of Api can be found in tests

Simple example to make index and find tokens in folder

```
        val searchApi:SearchiApi = TrigramSearchApi()
        val folderPath: Path = ...
        val token: String = ...

        val indexingState = searchApi.createIndexAtFolder(folderPath)
        //waiting indexing result
        indexingState.result.get()
        
        val searchingState = searchApi.searchString(folderPath, token)
        //waiting searching result
        val tokenMatches: List<TokenMatch> = searchingState.result.get()
        
        
```

## <a id="roadmap"></a> Roadmap (unordered)

- Saving and loading index on disk, making it persistent
    - on request
    - automatic by time
- Use database instead of files to save index
- Add filters for search
    - ignore case
    - separate word
    - fuzzy search
    - filter file type, for example *.kt
    - regex
- Scope for search within folder
- Grouping results by files and lines
- Ranging results by more relevant or other properties
- Support CLI
- Support GUI with desktop app or web interface
- Support multi-line string requests
- Optimize indexing big files:
    - separating them in blocks and indexing them separately
- Make artifact and upload somewhere on the Internet
- Optimize speed of indexing and searching
    - more decomposition and more parallel jobs
    - fix bottlenecks
- Optimize memory usage or leaks
- More test coverage
- Add more jobs to CI-CD:
    - with deploying new version to some repository
    - with tests with big folders

## <a id="algorithms"></a> Algorithms inside

### <a id="indexing-searching-algorithms"></a> Indexing & Searching in general

- For saving index it is used so-called trigram index
    - Each line of each file in folder is analysed
    - For every 3 sequencial character in line (triplet), it adds this file to set of files which has this character
      triplet
    - Result is association triplet to set of files with it
- For searching string in folder
    - It separates in character triplets (with sliding window of 3)
    - Set of files for each triplet is taken from trigram index
    - Search will be performed in intersection of all these sets
    - For each of selected files search works honestly in each line to find all occurrences

### <a id="indexing-algorithms"></a> Indexing

- Indexing is separated in parts working in parallel with coroutine jobs, connected with channels
- Before running asynchronous indexing it creates changing indexing state and gives as result.
- Everything can be monitored though this state by user of library
- Using 3 channels without limits of elements:
    - channel of visited files
    - channel of indexed files
    - channel of pairs: character triplet and file path
- There are 4 main parallel tasks:
    - Walking file tree with root chosen folder
        - iterating file tree
        - every visited file is sent to channel of visited files
        - every visited file is sa in indexing state
        - after accomplish visiting files, channel closes
        - On finish total number of files saved, and then progress can be calculated
    - Indexing visited files
        - constructing index for file
            - for each line finds all triplets and sends in channel of pairs: character triplet and file path
        - sends indexed file path in channel of indexed files
    - Reading indexed files
        - saves file in buffer of state with indexed files
        - saves file in result
        - registers time for file
    - Reading pairs with triplets and files
        - saves in structure

### <a id="searching-algorithms"></a> Searching

- Searching is separated in parts working in parallel with coroutine jobs, connected with channels
- Before running asynchronous searching it creates changing searching state and gives as result.
- Everything can be monitored though this state by user of library
- Using 3 channels without limits of elements:
    - channel of narrowed paths, those which has all triplets from searching string
    - channel of file lines: file path, line index, line
    - channel of token matches: file path, line index, position in line
- There are 4 main parallel tasks:
    - analyse searching string (token) and get narrow paths from index
        - get narrowed paths by token and index - only files which has all triplets from searching string
        - for each of these files - pushes to visited files in searching state
        - setups total values for files and files sizes in bytes
        - for each file path sends to channel of narrowed paths
        - closes in the end channel of narrowed paths
    - searching in file paths
        - for each narrow file in channel separates in lines
        - publish every line in channel of file lines
        - closes channel of file lines
    - searching in file lines
        - for each file line in channel searches token in line and sends all occurrences in channel of token matches
        - register in state parsed line - so progress can be updated
        - close channel of token matches
    - reading token matches
        - for each token match in channel adds token match to result and to the searching state buffer

### <a id="incremental-algorithms"></a> Incremental indexing

- Incremental indexing has 3 main parts
    - Receive changing event from file system
    - Apply changes in index
    - On start synchronize index before incremental indexing

## <a id="implementations"></a> Implementations

There are 2 implementations of *SearchApi*:

- *IndexlessSearchApi*
    - simple minimalistic implementation, which doesn't support indexing
    - search works straightforward: every time honestly looks for string in folder in every file
    - only synchronous api
    - simple, no parallel tasks or coroutines
    - used to compare behaviour or search with complete implementation *TrigramSearchApi*
- *TrigramSearchApi*
    - complete implementation with all features described above

## <a id="tests"></a> Tests

- There are more than 400 unit tests covered most of the features and code in general
- Aside of util tests there are 4 types of tests:
    - general search and fail situations for both implementations on small examples of file folders
    - testing internals of TrigramSearchApi
    - feature tests for complete implementation of SearchApi
    - manual tests with a lot of output for some external folder as source of searching, for example Intellij-Idea
      project
- Many tests checking search on source files of the same project
- Feature tests for indexing
    - concurrency
    - incremental
    - caching
    - cancel
    - index state
    - progress
    - skipping bad files
- Feature tests for searching
    - concurrency
    - caching
    - cancel
    - progress
    - relative to index
- TrigramSearchApi
    - smoke test
    - status changing logic
    - internal structure of index
    - watching and firing changes on file system

### <a id="big-tests"></a> Big tests

- Results of indexing and searching in Intellij-Idea a lot of output can be found in directory **docs/intellijIndexLogs
  ** in root of project
- Output files are organized by dates of executing, including previous bugs and output of previous version of project
- For example, indexing in *Intellij-Idea* project in *java* folder works about 4-5 minutes
- For example, searching in *Intellij-Idea* project in *java* folder
    - token "class" works about 25-30 seconds, because it is very popular token - more 10500 times for 48000 files
    - less popular token works about 50 milliseconds

### <a id="setup-big-test"></a> Setup big test

- Clone project Intellij Idea beside the project https://github.com/JetBrains/intellij-community
- Run tests though *searchApi/bigTest/main.kt* in *test* directory
- It can be chosen different api to run in file *IntellijIdeaTrigramTest*
- Current progress and stats of indexing and searching will go to console output

## <a id="used-stack"></a> Used stack

- Kotlin
    - kotlinx.coroutines
    - kotlinx.io
    - kotlinx.streams
    - kotlinx.reflect
    - kotlinx.math
- Java libs
    - java.nio
    - java.time
    - java.util.concurrent
    - java.io
    - java.util
    - java.util.stream
    - org.junit.jupiter
- Gradle
- Space CI DSL using kotlin script

## <a id="ci"></a> Continuous integration

- CI server: https://podmev.jetbrains.space/p/main/automation/jobs?repo=FolderTextSearch
- There is no continuous delivery for now
- CI is destined for Space environment
- CI file is *.space.kts* in the root of project
- Tasks in job:
    - build
    - test
- There is set up notifications in Space chat *CI-Channel* about fails of job tasks
- Each task has resources for workers 

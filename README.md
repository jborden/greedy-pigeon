
# greedy-pigeon

Code - [James Borden](https://github.com/jborden)

Audio and Visual Assets - [Hassan Estakhrian](https://www.antennafuzz.com/)

A game made for Ludum Dare 38. You can play it here: https://jborden.github.io/ld38_postjam/

## Requirements

* JDK 1.8+ (figwheel will complain with JDK < 1.8, but will still work)
* Leiningen 2.7.1
* node.js 5.1.1 [This is done to match the verion of node.js being used in Electron v1.6.0]

Works best with Chrome.

On Mac/Linux, installing node.js using [Node Version Manager](https://github.com/creationix/nvm) is recommended.

## Prerequisites

* Install Java for your system

* on macOS, it is recomended to install [homebrew](https://brew.sh/)

lein and npm can then be installed with

```bash
$ brew install leiningen
```

```bash
$ brew install npm
```


## Initial setup

Compile the development version of the application:

```bash
$ lein cljsbuild once dev
```

Install the portfinder, connect and serve-static libraries
```bash
$ npm i -S portfinder connect serve-static
```

Run the node server:
```bash
$ node resources/public/server.js
```

Point your browser to the url indicated on the command line.

**development**

http://localhost:8000

**release**
http://localhost:800/index_release.html

## Figwheel

Figwheel can be used to run the project and to provide a repl

```bash
$ rlwrap lein figwheel
```

After figwheel compiles the code, run the node server in another terminal and visit the url indicated on the command line.
The fighweel repl will connect after the url is loaded in the browser.

The file src/cljs/dev.cljs contains on-jsload function that figwheel calls each time it detects a file change in src/cljs.

## Production release compilation

To compile the code for a production release
```bash
$ lein cljsbuild once release
```

To test that it works, start up the node server. Point your browser to the index_release.html file
ex: http://localhost:8000/index_release.html

### Errors related to Name Mangling

A common source of errors in advanced compilation is related to issues with name mangling of object property and method names.
Though the cljsjs/three package comes with externs, they are not exhaustive. If you are using a property or method not listed at
https://github.com/cljsjs/packages/blob/master/three/resources/cljsjs/three/common/three.ext.js, you will need to add it to the
src/js/greedy_pigeon.externs.js file. There is a caveat, however. Because the extern file mentioned above already declares the
var THREE, you will need to append your property and method names to this var instead of creating a new THREE var as is typically suggested
in online cljs tutorials. The provided externs files has examples of doing this using methods and properties that are used by this project
but are not included in cljsjs/three externs.

Note: The project begins with no errors at the command line or in the console.

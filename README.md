# Delilah

> Keep your cotton pickin' fingers out my curly hair.
>
> -- Samson

Status: Alpha, api still subject to change

Delilah is an agent that retrieves information about power services

## Setup

Add the following entry to your deps.edn file

```clojure
{:deps [delilah/delilah {:git/url "https://github.com/PavlosMelissinos/delilah.git"
                         :sha ???}]}
```

## Usage

### DEDDIE

[Documentation](projects/deddie/README.md)

### DEI

#### Setup

Download a webdriver of your choice:

* [geckodriver (Firefox, default)](https://github.com/mozilla/geckodriver/releases)
* [chromedriver](https://chromedriver.chromium.org/downloads)
* [Microsoft Edge Driver](https://developer.microsoft.com/en-us/microsoft-edge/tools/webdriver/#downloads)
* [OperaDriver](https://github.com/operasoftware/operachromiumdriver/releases)

Please note that currently only Firefox (geckodriver) has been tested and is used when no webdriver is explicitly specified.

Extract the binary from the archive if needed and put it in `~/.cache/delilah/webdrivers/`.

Require dei API

```clojure
(:require [delilah.gr.dei.api :as dei])
```

#### Extract account data

```clojure
(dei/extract {:delilah.gr.dei/user "foo", :delilah.gr.dei/pass "bar"})
```

> :warning: The pdf blobs are printed as vectors and can be really long, so use `(set! *print-length* x)`
> before running delilah.gr.dei.api/extract in the REPL to limit the size of the output


#### Use chromedriver

```clojure
(let [driver {:type :chrome
              :path-driver "path/to/chromedriver/binary"
              :headless true}]
  (dei/extract {:delilah.gr.dei/user "foo", :delilah.gr.dei/pass "bar", :driver driver}
```

#### Download the bills in pdf form

Approach #1: Specify it when calling the extract function

```clojure
(dei/extract {:delilah.gr.dei/user "foo", :delilah.gr.dei/pass "bar", :save-files? true})
```

Approach #2: Download just the latest bill

```clojure
(-> (dei/extract {:delilah.gr.dei/user "foo", :delilah.gr.dei/pass "bar"})
    dei/latest-bill
    (dei/save-pdf! download-path))
```

Approach #3: Full manual mode

```clojure
(let [data (dei/extract {:delilah.gr.dei/user "foo", :delilah.gr.dei/pass "bar"})]
  ; Choose a random bill
  (rand-nth (:bills data)))
```
The :pdf-contents key of the result stores the binary file as a byte array. You can use `clojure.java.io/copy` to save it to disk.

Project structure inspired by [Polylith](https://polylith.gitbook.io/polylith)

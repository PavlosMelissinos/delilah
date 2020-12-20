# Delilah

> Keep your cotton pickin' fingers out my curly hair.
>
> -- Samson

Status: Alpha, api still subject to change

Delilah is an agent that retrieves information about power services

## Setup

Add the following entry to your deps.edn file

```clojure
{:deps [delilah {:git/url "https://github.com/PavlosMelissinos/delilah.git"
                 :sha ???}]}
```

## Usage

### DEDDIE

Require deddie API

```clojure
(:require [delilah.gr.deddie.api :as deddie])
```

#### Get outages

##### Entire prefecture

```clojure
;; For the entire prefecture of Thessaloniki
(deddie/outages "ΘΕΣΣΑΛΟΝΙΚΗΣ")
```

results in:

```clojure
[{:start #object[java.time.LocalDateTime 0x3b27ee8a "2020-11-23T08:00"],
  :end #object[java.time.LocalDateTime 0x24ff0bf8 "2020-11-23T14:30"],
  :municipality "ΣΥΚΕΩΝ",
  :affected-areas
  ["θα μείνει χωρίς ρεύμα η περιοχή των ΣΥΚΕΩΝ και συγκεκριμένα οι δρόμοι:Βορ.Ηπειρου(8ο Δημ.Σχολειο), Σολωμου, Ελυτη, Καποδιστριου(για δύο έως τρεις ώρες σε κάθε δρόμος το διάστημα 08:00 με 14:30.)"],
  :note-id nil,
  :cause "Κατασκευές",
  :affected-areas-raw
  "θα μείνει χωρίς ρεύμα η περιοχή των ΣΥΚΕΩΝ και συγκεκριμένα οι δρόμοι:Βορ.Ηπειρου(8ο Δημ.Σχολειο), Σολωμου, Ελυτη, Καποδιστριου(για δύο έως τρεις ώρες σε κάθε δρόμος το διάστημα 08:00 με 14:30.)"}
  ...]
```

##### Specific municipality

```clojure
;; For the municipality of Athens
(deddie/outages "ΑΤΤΙΚΗΣ" "ΑΘΗΝΑΙΩΝ")
```

results in
```clojure
[{:start #object[java.time.LocalDateTime 0x754efe6c "2020-11-24T08:00"],
  :end #object[java.time.LocalDateTime 0x3d7a83d8 "2020-11-24T11:00"],
  :municipality "ΑΘΗΝΑΙΩΝ",
  :affected-areas
  ["ΠΕΛΑΤΕΣ ΕΠΙ ΤΩΝ ΟΔΩΝ ΧΑΤΖΗΚΩΝΣΤΑΝΤΗ ΛΟΥΙΖΗΣ ΡΙΑΝΚΟΥΡ ΠΑΝΟΡΜΟΥ."],
  :note-id "810",
  :cause "Κατασκευές",
  :affected-areas-raw
  "ΠΕΛΑΤΕΣ ΕΠΙ ΤΩΝ ΟΔΩΝ ΧΑΤΖΗΚΩΝΣΤΑΝΤΗ ΛΟΥΙΖΗΣ ΡΙΑΝΚΟΥΡ ΠΑΝΟΡΜΟΥ."}
 {:start #object[java.time.LocalDateTime 0x6cd021b4 "2020-11-25T08:00"],
  :end #object[java.time.LocalDateTime 0x1aef31cb "2020-11-25T11:30"],
  :municipality "ΑΘΗΝΑΙΩΝ",
  :affected-areas
  [{:from #object[java.time.LocalTime 0x216fa4 "08:00"],
    :affected-numbers "Μονά",
    :street "ΛΕΩΦ.ΙΩΝΙΑΣ Νο 53",
    :to #object[java.time.LocalTime 0x4157131f "11:30"]}],
  :note-id "951",
  :cause "Λειτουργία",
  :affected-areas-raw
  "Μονά      οδός:ΛΕΩΦ.ΙΩΝΙΑΣ Νο 53  από: 08:00 πμ έως: 11:30 πμ"}
 {:start #object[java.time.LocalDateTime 0x7ed3770e "2020-11-25T08:00"],
  :end #object[java.time.LocalDateTime 0x561e1294 "2020-11-25T12:00"],
  :municipality "ΑΘΗΝΑΙΩΝ",
  :affected-areas ["ΠΕΛΑΤΕΣ ΕΠΙ ΤΗΣ ΟΔΟΥ ΑΓ.ΛΟΥΚΑ."],
  :note-id "816",
  :cause "Κατασκευές",
  :affected-areas-raw "ΠΕΛΑΤΕΣ ΕΠΙ ΤΗΣ ΟΔΟΥ ΑΓ.ΛΟΥΚΑ."},
 ...)
```


##### Advanced parser (beta)

Out of the three incidents, you might notice that the affected area field of the second one has a special format. Some municipalities in Attiki support a more structure way of reporting outages, so this is delilah's slightly more sophisticated parser that tries to make sense of them.

However, it's very fragile so use at your own risk

Here's another example of the advanced parser for Athens
``` clojure
({:start #object[java.time.LocalDateTime 0x60425afa "2020-11-25T08:30"],
  :end #object[java.time.LocalDateTime 0x26bd13e3 "2020-11-25T12:00"],
  :municipality "ΑΘΗΝΑΙΩΝ",
  :affected-areas
  [{:from-street "ΙΘΑΚΗΣ έως  ΚΑΛΥΜΝΟΥ ΝΟ 22",
    :from #object[java.time.LocalTime 0x263d38d6 "08:30"],
    :affected-numbers "Ζυγά",
    :street "ΚΑΛΥΜΝΟΥ",
    :to #object[java.time.LocalTime 0x63dc365f "12:00"]}
   {:from-street "ΙΘΑΚΗΣ  έως : ΑΡΙΣΤΟΤΕΛΟΥΣ ΝΟ 164",
    :from #object[java.time.LocalTime 0x1f8d7f6a "08:30"],
    :affected-numbers "Ζυγά",
    :street "ΑΡΙΣΤΟΤΕΛΟΥΣ",
    :to #object[java.time.LocalTime 0x63dc365f "12:00"]}],
  :note-id "952",
  :cause "Λειτουργία",
  :affected-areas-raw
  "Ζυγά      οδός:ΚΑΛΥΜΝΟΥ απο κάθετο: ΙΘΑΚΗΣ έως  ΚΑΛΥΜΝΟΥ ΝΟ 22 από: 08:30 πμ έως: 12:00 μμ\r\nΖυγά      οδός:ΑΡΙΣΤΟΤΕΛΟΥΣ απο κάθετο: ΙΘΑΚΗΣ  έως : ΑΡΙΣΤΟΤΕΛΟΥΣ ΝΟ 164 από: 08:30 πμ έως: 12:00 μμ"}
 ...)
```

##### Get outages by prefecture/municipality IDs

```clojure
;; For the municipality of Athens
(deddie/outages 10 112)
;; Identical to `(deddie/outages "ΑΤΤΙΚΗΣ" "ΑΘΗΝΑΙΩΝ")`
```

##### Miscellaneous

Get outages by prefecture/municipality ID: `(deddie/outages 10 112)` (same as `(deddie/outages "ΑΤΤΙΚΗΣ" "ΑΘΗΝΑΙΩΝ")`)

Get the id of a prefecture/municipality by name: `deddie/prefecture-name->id "ΘΕΣΣΑΛΟΝΙΚΗΣ")` or `(deddie/municipality-name->id "ΑΤΤΙΚΗΣ" "ΠΕΡΙΣΤΕΡΙΟΥ")`, etc

Get all of the available prefectures/municipalities: `(deddie/prefectures)`, `(deddie/all-municipalities)`, `(deddie/municipalities 10)`

Check the [API source code](src/delilah/gr/deddie/api.clj) for some more examples


### DEI

#### Setup

Download a webdriver of your choice. Please note that currently only Firefox (geckodriver) is supported.

Extract the binary from the archive if needed and put it in `~/.cache/delilah/webdrivers/`.

Require dei API

```clojure
(:require [delilah.gr.dei.api :as dei])
```

#### Extract account data

```clojure
(dei/extract {:user "foo" :pass "bar"})
```

> :warning: The pdf blobs are printed as vectors and can be really long, so use `(set! *print-length* 20)` to keep the output readable

#### Download the bills in pdf form

Approach #1: Specify it when calling the extract function

```clojure
(dei/extract {:user "foo" :pass "bar" :save-files? true})
```

Approach #2: Download just the latest bill

```clojure
(let [data (dei/extract {:user "foo" :pass "bar"})]
  (-> data dei/latest-bill dei/save-pdf!))
```

Approach #3: Full manual mode

```clojure
(let [data  (dei/extract {:user "foo" :pass "bar"})]
  ; Choose the bill you want
  (rand-nth (:bills data)))
```
The :pdf-contents key of the result stores the binary file as a byte array. You can use `clojure.java.io/copy` to save it to disk.

## Upcoming features

* [ ] DEI
  * [ ] Tests
  * [ ] PDF content parser
  * [ ] Chromedriver support
* [ ] HEDNO (ΔΕΔΔΗΕ) power cuts

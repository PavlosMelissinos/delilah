<img src="images/dei.png" alt="dei logo" width="150"/>

## Setup

Download a webdriver of your choice:

* [geckodriver (Firefox, default)](https://github.com/mozilla/geckodriver/releases)
* [chromedriver](https://chromedriver.chromium.org/downloads)
* [Microsoft Edge Driver](https://developer.microsoft.com/en-us/microsoft-edge/tools/webdriver/#downloads)
* [OperaDriver](https://github.com/operasoftware/operachromiumdriver/releases)

Please note that currently only Firefox (geckodriver) has been tested and is used when no webdriver is explicitly specified.

Extract the binary from the archive if needed and put it in `~/.cache/delilah/webdrivers/`.

## Usage

Require the API namespace

```clojure
(:require [delilah.gr.dei.api :as dei])
```

### Extract account data

> :warning: The pdf blobs are printed as vectors and can be really long, so use `(set! *print-length* x)`
> before running delilah.gr.dei.api/extract in the REPL to limit the size of the output

```clojure
(dei/extract {:delilah.gr.dei/user "foo", :delilah.gr.dei/pass "bar"})
```

Results in something like this:

```
  {:amount "100,00 €",
   :v1/end-date "17/08/2020",
   :address "ΚΕΛΙ 33, 18120, ΚΟΡΥΔΑΛΛΟΣ",
   :v1/expiration-date "14/09/2020",
   :v1/power-consumption "1000 kW",
   :e-payment-code "RF00000000000000000000000",
   :pdf-url
   "https://www.dei.gr/EBill/UserBill.aspx?FileName=000000000000_00000000_000000000_0.pdf",
   :v1/e-payment-code "RF00000000000000000000000",
   :v1/bill-type "ΕΚΚΑΘΑΡΙΣΤΙΚΟΣ",
   :bill-date #object[java.time.LocalDate 0x7addc9aa "2020-08-20"],
   :contract-account "000000000000",
   :date-received #object[java.time.Instant 0x583f470d "2020-08-21T10:50:43Z"],
   :v1/period-length "24",
   :v1/start-date "16/04/2020",
   :expiration-date "14/09/2020",
   :v1/amount "100,00",
   :pdf-contents
   [37, 80, 68, 70, 45, 49, 46, 55, 10, 37, -28, -29, -49, -46, 10, 53, 32, 48,
    32, 111, ...]}
```

### Use chromedriver

```clojure
(let [driver {:type :chrome
              :path-driver "path/to/chromedriver/binary"
              :headless true}]
  (dei/extract {:delilah.gr.dei/user "foo", :delilah.gr.dei/pass "bar", :driver driver}
```

### Download the bills in pdf form

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

The result here is a map that satisfies the following spec:
```clojure
(clojure.spec.alpha/def ::pdf-contents bytes?)
(clojure.spec.alpha/def (s/keys :req-un [::pdf-contents]))
```
The `:pdf-contents` key stores the binary file as a byte array. You can use `clojure.java.io/copy` to save it to disk.

### Enrich bills with email data (IMAP)

ΔΕΗ sends an email notification a few days after a bill has been issued.
delilah can optionally retrieve that info for you and enrich its reports.

Gmail:

```clojure
(dei/extract {:delilah.gr.dei/user           "foo"
              :delilah.gr.dei/pass           "bar"
              :delilah.gr.dei.mailer/user    "mail-foo"
              :delilah.gr.dei.mailer/pass    "mail-bar"
              :delilah.gr.dei.mailer/enrich? true})
```

In order to use a different mail provider, you need to override at least some of the optional settings.
Here are the settings, along with their default values (where applicable):

```clojure
(dei/extract {:delilah.gr.dei/user           "foo"
              :delilah.gr.dei/pass           "bar"
              :delilah.gr.dei.mailer/user    "mail-foo"
              :delilah.gr.dei.mailer/pass    "mail-bar"
              :delilah.gr.dei.mailer/imap    "imap.gmail.com"
              :delilah.gr.dei.mailer/port    587
              :delilah.gr.dei.mailer/tls     true
              :delilah.gr.dei.mailer/folder  "[Gmail]/All Mail" ;; default folder to look into
              :delilah.gr.dei.mailer/search  "ΔΕΗ e-bill"
              :delilah.gr.dei.mailer/enrich? false})
```

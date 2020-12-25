# Delilah

> Keep your cotton pickin' fingers out my curly hair.
>
> -- Samson

Status: Alpha, api still subject to change

Delilah is an agent that retrieves information about power services.

This library provides helper functions that parse data from power company websites and turn them into structured information. Use cases involve retrieving your latest electricity bill or checking out whether there's a planned power outage in your area.

## Setup

Add the following entry to your deps.edn file

```clojure
{:deps [delilah/delilah {:git/url "https://github.com/PavlosMelissinos/delilah.git"
                         :sha ???}]}
```

## Projects

Delilah is structured as a monorepo and is heavily based on [Polylith](https://polylith.gitbook.io/polylith).

It is comprised of the following projects (more to come):

### deddie

This project informs you of any power outages in your area (prefecture or municipality). For more information, check out the project [here](projects/deddie)

### dei

This project allows you to retrieve your electricity bills as well as some dei related information about your property. For more information, check out the project [here](projects/dei)

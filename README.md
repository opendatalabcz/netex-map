# NeTEx Map

<img src="https://fit.cvut.cz/static/images/fit-cvut-logo-cs.svg" alt="logo FIT ČVUT" height="200">

This software was created with the support of **Faculty of Information Technology, Czech Technical University in Prague**.
More information can be found at [fit.cvut.cz](https://fit.cvut.cz/en).

The open repository can be found at [https://github.com/opendatalabcz/netex-map](https://github.com/opendatalabcz/netex-map).

## Overview

NeTEx Map is a web application that visualizes Czech public transportation timetables published in NeTEx format on an interactive map with time-based controls. The application estimates geographical information, since it isn't part of the czech timetables.

## Project Structure

### Kotlin Spring Boot Server
- Imports, processes, and serves timetables
- Imports coarse stop positions from [JrUtil](https://github.com/dvdkon/jrutil) unified export published at [https://data.jr.ggu.cz/results/latest/](https://data.jr.ggu.cz/results/latest/)
- RESTful API

### Vue.js Web Application
- Interactive map visualization of estimated vehicle positions time-based controls
- Basic search functionality for timetables

### PostGIS Database
- PostgreSQL database with support for spacial data

### GraphHopper
- Routing service based on [OpenStreetMap](https://www.openstreetmap.org/about) data
- Provides estimated routes

### Overpass API
- Optimized query engine for [OpenStreetMap](https://www.openstreetmap.org/about) data
- Provides physical stop positions

### nginx
- Reverse proxy

## Project Setup

1. First obtain input data
    1. Obtain timetables in NeTEx format (`NeTEx_DrahyMestske.zip` and `NeTEx_VerejnaLinkovaDoprava.zip`) at [https://portal.cisjr.cz/pub/netex/](https://portal.cisjr.cz/pub/netex/)
    2. Obtain OpenStreetMap regional export in format `.osm.pbf`, e.g. at [https://osm.fit.vut.cz/extracts/czech_republic/](https://osm.fit.vut.cz/extracts/czech_republic/)
    3. Obtain coarse stop positions at [https://data.jr.ggu.cz/results/latest/](https://data.jr.ggu.cz/results/latest/) (file `JDF_merged_GTFS.zip`)
2. Create Docker image from Kotlin server
    ```sh
    cd ./map-api
    ./gradlew bootBuildImage
    ```
3. Compile web app
    ```sh
    cd ./map
    pnpm build --outDir <OUTPUT-DIR>
    ```
4. Setup proper host paths in `.env` file
5. Run Docker compose
    ```sh
    # At project root directory
    # Please note that Overpass initialization can take over 1 hour.
    docker compose up -d
    ```

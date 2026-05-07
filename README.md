# NeTEx Map

<img src="https://fit.cvut.cz/static/images/fit-cvut-logo-en.svg" alt="FIT CTU logo" height="200">

This software was developed with the support of the **Faculty of Information Technology, Czech Technical University in Prague**.
For more information, visit [fit.cvut.cz](https://fit.cvut.cz).

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

### nginx
- Reverse proxy

## Project Setup

1. First obtain input data
    1. Obtain timetables in NeTEx format (`NeTEx_VerejnaLinkovaDoprava.zip` and optionally `NeTEx_DrahyMestske.zip`) at [https://portal.cisjr.cz/pub/netex/](https://portal.cisjr.cz/pub/netex/)
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
    DB_ADMIN_PASSWORD=<PASSWORD> docker compose up -d
    ```

## Licence

Copyright (C) 2026  David Gaier

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

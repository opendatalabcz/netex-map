#!/bin/sh

set -e

echo "Starting data import..."

SERVER_ARGS=
mkdir -p "${DATA_DIR}"

download_data() {
    destination="$1"
    source_url="$2"
    label="$3"
    arg="$4"
    always_arg="$5"
    force_download="$6"

    if [ "${force_download}" = true ] || [ ! -f "${destination}" ]; then
        echo "Downloading ${label} from URL: ${source_url} ..."
        wget -q -O "${destination}" "${source_url}"
        echo "${label} downloaded"
        SERVER_ARGS="${SERVER_ARGS} ${arg}"
    else
        if [ "${always_arg}" = true ]; then
            SERVER_ARGS="${SERVER_ARGS} ${arg}"
        fi
    fi
}

if [ "${OSM_MODE}" = true ]; then
    download_data "${DATA_DIR}/${OSM_PBF_FILE_NAME}" "${OSM_PBF_URL}" "OSM data" "--osm-pbf=${DATA_DIR}/${OSM_PBF_FILE_NAME}" "${FORCE_SERVER_LOAD}" "${FORCE_DOWNLOAD}"
    echo "OSM download done"
    exit 0
fi

download_data "${DATA_DIR}/public_lines.zip" "${NETEX_PUBLIC_LINES_URL}" "Public lines" "--netex-zip=${DATA_DIR}/public_lines.zip" "${FORCE_SERVER_LOAD}" "${FORCE_DOWNLOAD}"
download_data "${DATA_DIR}/city_lines.zip" "${NETEX_CITY_LINES_URL}" "City lines" "--netex-zip=${DATA_DIR}/city_lines.zip" "${FORCE_SERVER_LOAD}" "${FORCE_DOWNLOAD}"
download_data "${DATA_DIR}/jrutil_gtfs.zip" "${JRUTIL_GTFS_URL}" "JrUtil data" "--jrutil-gtfs=${DATA_DIR}/jrutil_gtfs.zip" "${FORCE_SERVER_LOAD}" "${FORCE_DOWNLOAD}"
if [ -n "${SERVER_ARGS}" ]; then
    OSM_FORCE_LOAD=true
fi
download_data "${DATA_DIR}/${OSM_PBF_FILE_NAME}" "${OSM_PBF_URL}" "OSM data" "--osm-pbf=${DATA_DIR}/${OSM_PBF_FILE_NAME}" "${OSM_FORCE_LOAD}" false

if [ -n "${SERVER_ARGS}" ]; then
    echo "Starting import with arguments: ${SERVER_ARGS}"
    java -jar /app/app.jar "--spring.main.web-application-type=none" ${SERVER_ARGS}
else
    echo "No data to import"
fi

echo "Import done"

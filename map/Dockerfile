FROM node:20-alpine AS build
WORKDIR /app

RUN npm install -g pnpm

COPY map/package.json map/pnpm-lock.yaml .
RUN pnpm install

COPY map/ .
ARG FE_INITIAL_MOMENT='2026-06-01T16:40:10' FE_CITY_LINES_ZOOM_THRESHOLD=12 FE_API_URN='/api' FE_FIT_CVUT_URL='#' FE_ODL_URL='#' FE_PROFINIT_URL='#' FE_GITHUB_REPO_URL='#' FE_JRUTIL_URL='#' FE_PID_MAP_URL='#' FE_CZECH_NETEX_DATA_URL='#' FE_JRUTIL_DATA_URL='#' FE_OSM_URL='#' FE_CZECH_OSM_DATA_URL='#' FE_GRAPHHOPER_URL='#'
RUN pnpm build

FROM nginx:mainline-alpine3.23-slim
ARG PROXY_HTML_CONTAINER_PATH=/usr/share/nginx/html
COPY --from=build /app/dist ${PROXY_HTML_CONTAINER_PATH}

# build using node/yarn, then serve static files with nginx
# requires --build-arg api_base_url

# build stage
FROM node:10-alpine as build-stage
WORKDIR /app
COPY package*.json /app/
COPY yarn.lock /app/
RUN yarn
COPY ./ /app/
ARG api_base_url
RUN test -n "$api_base_url"
ENV REACT_APP_API_BASE_URL=$api_base_url
RUN yarn build

# serve with nginx
FROM nginx:1.17
COPY --from=build-stage /app/build/ /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

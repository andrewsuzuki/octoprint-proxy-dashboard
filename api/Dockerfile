FROM clojure:lein-2.8.1
WORKDIR /usr/src/app
COPY project.clj /usr/src/app/
RUN lein deps
COPY . /usr/src/app
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" api-standalone.jar
ENTRYPOINT ["java", "-jar", "api-standalone.jar"]

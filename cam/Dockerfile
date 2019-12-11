FROM clojure:lein-2.8.1
RUN apt-get -y update && apt-get -y install ffmpeg
WORKDIR /usr/src/app
COPY project.clj /usr/src/app/
RUN lein deps
COPY . /usr/src/app
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" cam-standalone.jar
ENTRYPOINT ["java", "-jar", "cam-standalone.jar"]

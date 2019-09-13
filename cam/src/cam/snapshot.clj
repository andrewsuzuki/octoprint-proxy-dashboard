(ns cam.snapshot
  (:require [clojure.java.io :as io])
  (:import (net.bramp.ffmpeg.builder FFmpegBuilder)
           (net.bramp.ffmpeg FFmpegExecutor)
           (java.util UUID Base64)
           (java.nio.file Files)))

(defn camera->data-uri
  "open video device and take single frame, returning
  a string data uri of the image (base-64 jpeg)"
  [device]
  (let [image-file (io/file (str "/tmp/" (UUID/randomUUID) ".jpg")) ; temporary file
        ; create ffmpeg command that opens video device to take a single
        ; frame, formats as jpeg, and saves to the temporary file
        builder (-> (FFmpegBuilder.)
                    (.setInput device)
                    (.setFormat "video4linux2")
                    (.addOutput (.getAbsolutePath image-file))
                    (.setFormat "mjpeg")
                    (.setFrames 1)
                    (.addExtraArgs (into-array String ["-q:v" "1"])) ; 1 is highest quality (does this work?)
                    (.done))]
    ; execute ffmpeg command
    (-> (FFmpegExecutor.)
        (.createJob builder)
        (.run))
    ; retrieve file and return data uri (base 64)
    (let [image-path (.toPath image-file)
          content-type (Files/probeContentType image-path)
          base64-string (->> image-path
                             (Files/readAllBytes)
                             (.encodeToString (Base64/getEncoder)))]
      ; remove temporary file
      (.delete image-file)
      ; form data uri
      (str "data:" content-type ";base64," base64-string))))

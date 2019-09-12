# octoprint-spap

Proxy server and UI for multiple Octoprints, with webcam snapshots. Made in New Haven for MakeHaven.

Three services:
* **front-end (javascript)** simple react front-end that connects to `api` via websocket and receives updates on individual octoprint/`cam` servers, reflecting changes immediately to the ui.
* **api (clojure)** for each configured Octoprint, polls `cam` server on interval if available (for webcam snapshots), and subscribes to the Octoprint Push API servers, broadcasting all relevant changes to clients via its own websocket server.
* **cam (clojure)** simple webcam service to be installed on 3d printer pis alongside octoprint. on web requests, it requests a frame from the webcam (using FFmpeg), and returns a base64-encoded representation of the image.

![octoprint proxy screenshot](screenshot.png)

## Contributing

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request

## Credits

* Andrew Suzuki - @andrewsuzuki - [andrewsuzuki.com](http://andrewsuzuki.com)

## License

MIT

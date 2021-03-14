import React from "react";
import omit from "lodash.omit";
import sortby from "lodash.sortby";

import "./App.css";
import Printer from "./Printer";

const reconnectTimeout = 2000; // 2 seconds

// api base url, without trailing slash
const apiBase = (
  process.env.REACT_APP_API_BASE_URL || "http://localhost:8080"
).replace(/\/+$/, "");

let nextLoaderId = 0;

export default class App extends React.Component {
  state = {
    printers: [],
    cams: {},
    loaders: [],
    connectionErrorString: null,
    connection: null
  };

  componentDidMount() {
    this.start();
  }

  componentWillUnmount() {
    this.reset();
  }

  reset = () => {
    // close connection if it's open
    const { connection } = this.state;
    if (
      connection &&
      [WebSocket.CONNECTING, WebSocket.OPEN].includes(connection.readyState)
    ) {
      this.state.connection.close();
    }

    this.setState({
      printers: [],
      cams: {},
      loaders: [],
      connectionErrorString: null,
      connection: null
    });
  };

  onNoConnection = () => {
    // show user error
    this.setState({
      connectionErrorString: "Couldn't reach server, attempting reconnection..."
    });
    // try again in two seconds
    setTimeout(this.start, reconnectTimeout);
  };

  onMessage = message => {
    const json = JSON.parse(message.data);

    switch (json.type) {
      case "new-printer":
        this.setState({
          printers: this.state.printers.map(printer => {
            if (printer.id === json["printer-id"]) {
              return json.data;
            } else {
              return printer;
            }
          })
        });
        break;
      case "new-cam":
        this.setState({
          cams: {
            ...this.state.cams,
            [json["printer-id"]]: json.data
          }
        });
        break;
      case "remove-cam":
        this.setState({
          cams: omit(this.state.cams, json["printer-id"])
        });
        break;
      default:
        // do nothing
        console.warn(`Unrecognized message type from server (${json.type})`);
    }
  };

  start = async () => {
    this.reset();

    // Hydrate
    const isHydrated = await this.hydrate();

    if (!isHydrated) {
      this.onNoConnection();
      return;
    }

    // connect to websocket at /subscribe
    const loaderId = this.startLoader();
    const wsUrl = new URL(apiBase);
    wsUrl.protocol = wsUrl.protocol === "https:" ? "wss:" : "ws:";
    const ws = new WebSocket(`${wsUrl}subscribe`);
    this.setState({
      connection: ws
    });
    ws.onopen = () => {
      this.stopLoader(loaderId);
    };
    ws.onerror = () => {
      this.stopLoader(loaderId);
    };
    ws.onclose = this.onNoConnection;
    ws.onmessage = this.onMessage;
  };

  hydrate = async () => {
    const loaderId = this.startLoader();

    try {
      const response = await fetch(`${new URL(apiBase)}hydrate`);
      const json = await response.json();
      this.stopLoader(loaderId);

      const { printers, cams } = json;

      this.setState({
        printers: printers,
        cams: cams
      });

      return true;
    } catch (e) {
      this.stopLoader(loaderId);
      return false;
    }
  };

  startLoader = () => {
    const loaderId = nextLoaderId++;

    this.setState({
      loaders: [...this.state.loaders, loaderId]
    });

    return loaderId;
  };

  stopLoader = loaderId => {
    this.setState({
      loaders: this.state.loaders.filter(l => l !== loaderId)
    });
  };

  render() {
    const {
      loaders,
      printers,
      cams,
      connection,
      connectionErrorString
    } = this.state;

    const isLoading = loaders.length > 0;

    return (
      <div className="App">
        <div className="App-header">
          <p>Octoprint Proxy</p>
          {isLoading ? (
            <p>
              <span role="img" aria-labelledby="hourglass">
                ⌛
              </span>{" "}
              Loading...
            </p>
          ) : connection ? (
            <p>
              <small>🌐 Connected (updates automatically)</small>
            </p>
          ) : null}
        </div>
        <div className="App-content">
          {connectionErrorString ? (
            <p>{connectionErrorString}</p>
          ) : isLoading ? null : (
            <div className="grid">
              {sortby(printers, printer => printer.index).map(printer => (
                <div className="col-md-6 col-xlg-4" key={printer.id}>
                  <Printer printer={printer} cam={cams[printer.id]} />
                </div>
              ))}
            </div>
          )}
        </div>
        <div className="App-footer">
          &copy;{new Date().getFullYear()} Andrew Suzuki &middot; Made for
          MakeHaven &middot;{" "}
          <a
            href="https://github.com/andrewsuzuki/octoprint-proxy-dashboard"
            title="source on github"
          >
            source
          </a>{" "}
        </div>
      </div>
    );
  }
}

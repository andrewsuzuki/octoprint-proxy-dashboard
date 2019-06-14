import React from "react";
import "./App.css";
import Printer from "./Printer";

const updateIntervalSeconds = 10;

const endpoint =
  process.env.REACT_APP_SPAP_ENDPOINT || "http://localhost:8080/api";

let nextLoaderId = 0;

export default class App extends React.Component {
  state = {
    printers: null,
    loaders: []
  };

  componentDidMount() {
    this.startPolling();
  }

  componentWillUnmount() {
    clearInterval(this.poller);
  }

  startPolling = () => {
    this.poller = setInterval(this.poll, updateIntervalSeconds * 1000);
    this.poll();
  };

  poll = async () => {
    const printers = await this.fetch();
    this.setState({ printers });
  };

  fetch = async () => {
    const loaderId = this.startLoader();

    try {
      const response = await fetch(endpoint);
      const json = await response.json();
      this.stopLoader(loaderId);
      return json;
    } catch (e) {
      this.stopLoader(loaderId);
      return null;
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
    const { loaders, printers } = this.state;

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
          ) : (
            <p>
              <small>
                Updates automatically every {updateIntervalSeconds} seconds
              </small>
            </p>
          )}
        </div>
        <div className="App-content">
          {printers ? (
            <div className="grid">
              {printers.map(printer => (
                <div className="col-md-6 col-xlg-4" key={printer.name}>
                  <Printer printer={printer} />
                </div>
              ))}
            </div>
          ) : isLoading ? null : (
            <p>Service not available</p>
          )}
        </div>
        <div className="App-footer">
          &copy;{new Date().getFullYear()} Andrew Suzuki &middot;{" "}
          <a
            href="https://github.com/andrewsuzuki/octoprint-spap"
            title="source on github"
          >
            source
          </a>
        </div>
      </div>
    );
  }
}

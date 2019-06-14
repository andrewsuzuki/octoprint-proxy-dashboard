import React from "react";
import "./App.css";
import Printer from "./Printer";

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
    this.poller = setInterval(this.poll, 10 * 1000);
    this.poll();
  };

  poll = async () => {
    const printers = await this.fetch();
    this.setState({ printers });
  };

  fetch = async () => {
    const loaderId = nextLoaderId++;

    this.setState({
      loaders: [...this.state.loaders, loaderId]
    });

    const response = await fetch(endpoint);
    const json = await response.json();

    this.setState({
      loaders: this.state.loaders.filter(l => l !== loaderId)
    });

    return json;
  };

  render() {
    const { loaders, printers } = this.state;

    const isLoading = loaders.length > 0;

    return (
      <div className="App">
        <div className="App-header">
          <p>Octoprint Proxy</p>
          {isLoading ? <p>Loading...</p> : null}
        </div>
        {printers ? (
          <div>
            {printers.map(printer => (
              <Printer key={printer.name} printer={printer} />
            ))}
          </div>
        ) : isLoading ? null : (
          <p>Service not available</p>
        )}
      </div>
    );
  }
}

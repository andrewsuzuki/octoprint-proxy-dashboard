import React from "react";
import classNames from "classnames";
import "./Printer.css";

function Printer({ printer }) {
  const {
    name,
    errored,
    connected,
    last_retrieved,
    printer_info,
    current_job
  } = printer;

  return (
    <div className="Printer">
      <h2
        className={classNames("Printer-name", {
          "Printer-name-errored": errored || !connected
        })}
      >
        {name}
      </h2>
      <p>Polled at: {new Date(last_retrieved).toLocaleString()}</p>
      {errored ? <p>Could not contact server</p> : null}
      {!connected ? (
        <p>
          Octoprint <b>not connected</b> to printer. Please connect through
          Octoprint.
        </p>
      ) : (
        <React.Fragment>
          <p>{JSON.stringify(printer_info)}</p>
          <p>{JSON.stringify(current_job)}</p>
        </React.Fragment>
      )}
    </div>
  );
}

export default Printer;

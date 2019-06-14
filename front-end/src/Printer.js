import React from "react";
import classNames from "classnames";
import "./Printer.css";

function Printer({ printer }) {
  const { name, errored, last_retrieved, printer_info, current_job } = printer;

  return (
    <div className="Printer">
      <h2
        className={classNames("Printer-name", {
          "Printer-name-errored": errored
        })}
      >
        {name}
      </h2>
      <p>Polled at: {new Date(last_retrieved).toLocaleString()}</p>
      <p>{JSON.stringify(printer_info)}</p>
      <p>{JSON.stringify(current_job)}</p>
    </div>
  );
}

export default Printer;

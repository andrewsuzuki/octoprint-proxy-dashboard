package core

import (
	"bytes"
	"encoding/json"
	"io"
	"io/ioutil"
	"log"
	"net/http"
	"strings"
	"time"

	"github.com/tidwall/gjson"
)

// Endpoints
const endpointConnection = "/api/connection"
const endpointVersion = "/api/version"
const endpointPrinter = "/api/printer?history=false&exclude=sd"
const endpointCurrentJob = "/api/job"

func requestWithPrinterConfig(method string, endpoint string, body io.Reader, p PrinterConfig) (*http.Response, error) {
	req, err := http.NewRequest(method, p.ServerAddress+endpoint, body)
	if err != nil {
		log.Fatal(err)
	}
	if len(p.ApiKey) > 0 {
		req.Header.Set("X-Api-Key", p.ApiKey)
	}
	req.Header.Set("Content-Type", "application/json")
	resp, err := http.DefaultClient.Do(req)
	return resp, err
}

// getConnectionState asks octoprint for connection state string,
// then also parses that string for errors to determine if printer is connected
func getConnectionState(p PrinterConfig) (string, bool, error) {
	resp, err := requestWithPrinterConfig("GET", endpointConnection, nil, p)

	if err != nil {
		return "", false, err
	}

	body, err := ioutil.ReadAll(resp.Body)
	defer resp.Body.Close()
	if err != nil {
		return "", false, err
	}

	stateString := gjson.Get(string(body), "current.state").String()
	connected := strings.Contains(stateString, "Operational")
	return stateString, connected, nil
}

// attemptConnectionIfNeeded first attempts to get connection information from
// Octoprint. If printer state is errored or offline, it will attempt to connect.
// Finally, it returns bool true if connected, false if not.
func attemptConnectionIfNeeded(p PrinterConfig) (bool, error) {
	_, connected, err := getConnectionState(p)

	if err != nil {
		return false, err
	}

	if connected {
		return true, nil
	}

	// Attempt connection, if configuration allows

	if !p.AutoConnect {
		return false, nil
	}

	log.Println("Requesting " + p.ServerAddress + " to connect to its printer")

	err = connect(p)
	if err != nil {
		return false, err
	}

	// Wait a second
	time.Sleep(1 * time.Second)

	// Ask again
	_, connected, err = getConnectionState(p)

	if err != nil {
		return false, err
	}

	return connected, nil
}

func connect(p PrinterConfig) error {
	var connectBody = []byte(`{"command":"connect"}`)
	resp, err := requestWithPrinterConfig("POST", endpointConnection, bytes.NewBuffer(connectBody), p)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	return nil
}

// getApiVersion requests the Octoprint api version
// at the given server
func getApiVersion(p PrinterConfig) (string, error) {
	resp, err := requestWithPrinterConfig("GET", endpointVersion, nil, p)

	if err != nil {
		return "", err
	}

	var result map[string]string

	json.NewDecoder(resp.Body).Decode(&result)
	defer resp.Body.Close()
	return result["api"], nil
}

// getPrinterInfo requests state and temperature data
// for the given printer
func getPrinterInfo(p PrinterConfig) (map[string]interface{}, error) {
	resp, err := requestWithPrinterConfig("GET", endpointPrinter, nil, p)

	if err != nil {
		return nil, err
	}

	var result map[string]interface{}

	json.NewDecoder(resp.Body).Decode(&result)
	defer resp.Body.Close()

	return result, nil
}

// getCurrentJob requests information on the
// current job for the given printer
func getCurrentJob(p PrinterConfig) (map[string]interface{}, error) {
	resp, err := requestWithPrinterConfig("GET", endpointCurrentJob, nil, p)

	if err != nil {
		return nil, err
	}

	var result map[string]interface{}

	json.NewDecoder(resp.Body).Decode(&result)
	defer resp.Body.Close()

	return result, nil
}

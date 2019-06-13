// Copyright 2019 Andrew Suzuki. All rights reserved.
package cmd

import (
	"log"
	"os"
	"path"
	"path/filepath"

	"github.com/spf13/cobra"
	"github.com/spf13/viper"


	"github.com/andrewsuzuki/octoprint-spap/core"
)

var cfgFile string

var RootCmd = &cobra.Command{
	Use:   "ospap",
	Short: "safe publicly-accessible proxy for octoprint",
	Run: func (cmd *cobra.Command, args []string) {
        var c core.Config
        viper.Unmarshal(&c)
        if err := viper.Unmarshal(&c); err != nil {
            log.Fatalf("Couldn't read config: %s", err)
        }
        core.Run(c)
    },
}

func Execute() {
	if err := RootCmd.Execute(); err != nil {
		log.Println(err)
		os.Exit(-1)
	}
}

func init() {
	cobra.OnInitialize(initConfig)

	RootCmd.PersistentFlags().StringVar(&cfgFile, "config", "", "config file (default is $HOME/ospap.json)")
}

// initConfig reads in config file and ENV variables if set.
func initConfig() {
	dir, err := filepath.Abs(filepath.Dir(os.Args[0]))
	if err != nil {
		log.Fatal(err)
	}

	viper.SetConfigName("ospap") // name of config file (without extension)

	if cfgFile != "" {
		viper.SetConfigFile(cfgFile)
		configDir := path.Dir(cfgFile)
		if configDir != "." && configDir != dir {
			viper.AddConfigPath(configDir)
		}
	}

	viper.AddConfigPath(dir)
	viper.AddConfigPath(".")
	viper.AddConfigPath("$HOME")
	viper.AutomaticEnv() // read in environment variables that match

	// If a config file is found, read it in.
	if err := viper.ReadInConfig(); err == nil {
		log.Println("Using config file:", viper.ConfigFileUsed())
	} else {
		log.Println(err)
        os.Exit(1)
	}
}

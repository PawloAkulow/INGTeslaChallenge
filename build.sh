#!/bin/bash

# Check if Maven is installed
if ! command -v mvn &> /dev/null
then
    echo "Maven could not be found. Please install Maven before running this script."
    exit
fi

# Build the project using Maven
mvn clean install

#!/bin/bash

# Set the main class and the path to the JAR file
MAIN_CLASS="com.EnergySavingBanking.INGTeslaChallenge"
JAR_FILE="target/INGTeslaChallenge-1.0-SNAPSHOT.jar"

# Check if Java is installed
if ! command -v java &> /dev/null
then
    echo "Java could not be found. Please install Java before running this script."
    exit
fi

# Run the program
java -cp $JAR_FILE $MAIN_CLASS

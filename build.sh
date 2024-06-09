#!/bin/bash
./mvnw clean package

ls -lh ./target/*.jar

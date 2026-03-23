#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT_DIR="$SCRIPT_DIR/output"

rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

mvn clean install -DskipTests -q -f "$SCRIPT_DIR/pom.xml"

cp "$SCRIPT_DIR/agent-bootstrap/target/agent-bootstrap-"*.jar "$OUTPUT_DIR/java-agent.jar"
cp "$SCRIPT_DIR/agent.yaml" "$OUTPUT_DIR/"

mkdir -p "$OUTPUT_DIR/plugins"
cp "$SCRIPT_DIR/agent-plugin/plugin-http/target/plugin-http-"*.jar "$OUTPUT_DIR/plugins/"
cp "$SCRIPT_DIR/agent-plugin/plugin-jdbc/target/plugin-jdbc-"*.jar "$OUTPUT_DIR/plugins/"
cp "$SCRIPT_DIR/agent-plugin/plugin-executor/target/plugin-executor-"*.jar "$OUTPUT_DIR/plugins/"

echo "Build complete:"
find "$OUTPUT_DIR" -type f | sort

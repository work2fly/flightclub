#!/bin/bash
# Build Flight Club from source
# Requires: JDK 11+ (tested with OpenJDK 11)

set -e

echo "Compiling Flight Club..."
rm -rf out
mkdir -p out

find . -name "*.java" -not -path "./out/*" > /tmp/fc_sources.txt
javac -d out -encoding UTF-8 @/tmp/fc_sources.txt
rm /tmp/fc_sources.txt

# Copy resource files needed at runtime
cp *.wav *.txt out/ 2>/dev/null || true

echo "Build complete. Classes in ./out/"
echo ""
echo "Run the game:"
echo "  java -cp out flightclub.startup.XCFrame [task] [pilot_type] [host:port] [pgs hgs sps]"
echo ""
echo "  pilot_type: 0=paraglider, 1=hangglider, 2=sailplane, 3=balloon"
echo ""
echo "Run with a bigger window:"
echo "  java -Dfc.width=1280 -Dfc.height=720 -cp out flightclub.startup.XCFrame"
echo ""
echo "Run the Task Designer:"
echo "  java -cp out flightclub.task.TaskFrame"

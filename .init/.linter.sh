#!/bin/bash
cd /home/kavia/workspace/code-generation/elegant-dance-studio-website-242614-242624/nextjs_backend_admin
./gradlew checkstyleMain
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi


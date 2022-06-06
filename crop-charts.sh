#!/usr/bin/env sh
find charts -name '*.pdf' -exec sh -c 'pdfcrop $1 &' shell {} \;

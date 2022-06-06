#!/usr/bin/env sh
find charts/**/*.pdf -exec pdfcrop {} & \;

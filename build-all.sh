find . -maxdepth 1 -mindepth 1 -type d -exec sh -c '(cd {} && mvn clean install)' ';'

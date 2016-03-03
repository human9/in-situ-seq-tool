JAR = target/my-cyaction-app-1.0-SNAPSHOT.jar

install: $(JAR)
	cp $(JAR) ~/CytoscapeConfiguration/3/apps/installed/

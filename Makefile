JAR = target/inseq-1.0-PROTOTYPE.jar

winstall: $(JAR)
	cp $(JAR) C:/Users/John.Salamon/CytoscapeConfiguration/3/apps/installed

install: $(JAR)
	cp $(JAR) ~/CytoscapeConfiguration/3/apps/installed/

all:run

compile:
	javac MovieDB.java Table.java

javadoc:
	javadoc Table.java

clean:
	rm -f *.class

run:compile
	java MovieDB

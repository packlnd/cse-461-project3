all: server Client.class

server: server.cpp
	g++ -std=c++11 -Wall -pedantic -o server ConnectionInfo.cpp server.cpp
Client.class: Client.java
	javac Client.java

all: master_server

master_server: master_server.cpp
	g++ -std=c++11 -Wall -pedantic -o master_server ConnectionInfo.cpp master_server.cpp
RelayServer: RelayServer.cpp
	g++ -std=c++11 -Wall -pedantic -o RelayServer ConnectionInfo.cpp RelayServer.cpp
Client.class: Client.java
	javac Client.java

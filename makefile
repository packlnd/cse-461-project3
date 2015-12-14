all: master_server

master_server: master_server.cpp
	g++ -std=c++11 -Wall -pedantic -o master_server ConnectionInfo.cpp master_server.cpp

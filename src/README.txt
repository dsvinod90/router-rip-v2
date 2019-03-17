Author:
Full name   : Ishan Guliani
Pen name    : ishanguliani
Email       : ig5859@rit.edu

Description:
Implementation of RIPv2 protocol from RFC 2453

JAVA Files:
1.  RoverManager
2.  RouterProcess
3.  RIPPacket
4.  RoutingTableEntry
5.  MyThreadPoolExecutorService
6.  Log
7.  Helper

How to execute -
OPTION 1: If you have the 'make' program installed in your machine -
-> Fire up a new router process:
$ make program multicast_ip=224.0.0.9 id=1 port=4445

OPTION 2: If you do not have the 'make' program installed -
$ rm -rf classes ||:	# remove the classes directory if it exists
$ mkdir -p classes	    # create a clean 'classes' directory
$ javac -d classes Main.java	# compile the program and copy all .class files to 'class' directory
$ cd classes && java RouterProcess <224.0.0.9> <1> <4445> && cd ..	# change directory to 'classes' and run the program

NOTE: The rover id must be unique for each new router


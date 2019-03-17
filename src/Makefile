PROGRAM = program
CLASS = classes
MAKEDIR = directory
MAINFILE = main

$(PROGRAM): $(CLASS)
	@echo "Running the executable..."
	@echo "-------------------"
	@cd classes && java RouterProcess $(multicast_ip) $(id) $(port)

$(CLASS): $(MAKEDIR)
	@echo "compiling java files..."
	@echo "compilation completed without errors!"
	@echo "all class files copied to ./classes directory!"
	@javac -d classes RouterProcess.java

all: $(PROGRAM)
	@echo "executing program..."

clean:
	@echo "cleaning all .class files in the classes directory..."
	@rm -rf classes ||:
	@echo "cleaned successfully!"

$(MAKEDIR): $(MAINFILE) clean
	@echo "Creating directory 'classes' in the current directory..."

$(MAINFILE): RouterProcess.java
	@echo "----Beginning make program------"
	@echo "Sample execution: Fire up a router process:  $$ make program multicast_ip=224.0.0.9 id=1 port=4445"
	@echo "--------------------"
	@echo "Preparing directory for execution"
	@echo "Looking for RouterProcess.java..."

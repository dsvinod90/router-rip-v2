ROVER = rover
NASA = nasa
MAKEDIR = directory
MAINFILE = main

$(ROVER):
	@echo "Running the executable..."
	@echo "-------------------"
	@cd classes && java RouterProcess $(multicast_ip) $(id) $(port)

$(NASA): $(MAKEDIR)
	@echo "compiling program... compilation completed without errors!"
	@echo "You are ready to deploy rovers to the moon!"
	@echo "To deploy your first rover now, run 'make rover multicast_ip=224.0.0.9 id=1 port=4445' "
	@echo "----------------------"
	@javac -d classes RouterProcess.java

all: $(ROVER)
	@echo "executing program..."

clean:
	@rm -rf classes ||:
	@echo "Setting up communication protocols..."

$(MAKEDIR): $(MAINFILE) clean
	@echo "Creating buffer directories for intermediate storage 'classes'..."
	@mkdir -p classes

$(MAINFILE): RouterProcess.java
	@echo "Analyzing moon surface for rover deployment..."
	@echo "--------------------"

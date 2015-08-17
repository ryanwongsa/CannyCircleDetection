#VARIABLES
JAVAC = javac
JFLAGS = -g

#default rule
all: 
	$(JAVAC) $(JFLAGS) *.java


run: 
	java HoughCircleDetection ${file} ${gwin} ${sigma} ${lthresh} ${hthresh} ${mrad} ${houghthresh}

# explicit rule
clean:
	@rm –f $(SRCFILE).class
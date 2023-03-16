TARGETS=tema2
build: $(TARGETS)

tema2: Tema2.java Task1.java Task1Writer.java Task2.java Constants.java
	javac $^
	
run_test:
	java Tema2 ../input 4
	
clean:
	rm -f *.class
	
.PHONY: build clean

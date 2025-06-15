### On Warning processing
A Ruby script was used to compress the warnings reported by LiSA. The Ruby files are:

- main.rb
- analyzers.rb
- all files within the analyzers folder

To start the compression process:

- Run the test (Overflow and DivisionByZero analysis is supported for both intervals and pentagons), providing the .imp 
    files in the correct folder - `intervalsdivisionbyzerotest`, `intervalsoverflowtest`, `pentagonsdivisionbyzerotest`,
    `pentagonsoverflowtest`
- Run `docker compose build` & `docker compose up`
- Connect to the started container
- Run `ruby main.rb`

### On Results
The LiSA analyzer was run on each file provided by the students. The compressed results are available in the `warnings`
folder. Output of taint analysis hasn't been compressed; in order to produce the results, tests need to be run.

### On Tests
The tests are located in the `test` folder inside the `test879899` package.


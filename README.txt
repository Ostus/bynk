//////////////////////////
//  To run the program  //
//////////////////////////

sbt
runMain Main <path>
Ex:
runMain Main ./src/test/resources/files

[Possible to run into error 'java.nio.charset.MalformedInputException' if there are files in the
directory which are not readable files. Stumbled upon this error when the program was almost
complete and did not have time to make a safe-guard for it. A long-term solution would be to have a
try in Main around the directory.listFiles() and catch the mentioned error. A work-around is to
delete all files that are not readable files and the program runs as intended.]

Enter the search string you'd like to find in the files in the directory that is not equal to ""

When done; exit the program by running the command ":quit"

///////////////////////////
//  Program assumptions  //
///////////////////////////

Searches the provided directory recursively and extracts all files
Ignores upper/lower case, if the letter matches it's a hit
Returns an error message if the search term is an empty string
Only unique words are sought after, both in the search string as well as in the file's content

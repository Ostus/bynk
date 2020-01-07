import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.{Files, Paths}

import Program.LocalFile
import org.scalatest.FunSuite

class Test extends FunSuite {
  def compareIgnoringContent(matchingPercentage: Int, inData: Array[LocalFile], fileNames: List[String]): Unit = {
    inData.map(lf => assert(fileNames.contains(lf.name) && lf.matchingPercentage === matchingPercentage))
  }

  test("Should fail when path argument is not a directory") {
    val path = "notADirectory"
    assert(Program.readFile(Array(path)) === Left(Program.NotDirectory(s"Path [​$path] is not a directory")))
  }

  test("Should fail when path argument is missing") {
    assert(Program.readFile(Array.empty) === Left(Program.MissingPathArg))
  }

  test("Should be able to extract files with working path") {
    val dir = "./src/test/resources/files"
    val either = Program.readFile(Array(dir))

    assert(either.right.toOption.isDefined)
  }

  test("Calculation should return expected results for different overlapping file contents") {
    val files = Array(
      LocalFile(name = "file1.txt", "string is not for me"),
      LocalFile(name = "file2.txt", "int is for me"),
      LocalFile(name = "file3.txt", "some other random string containing the word for"),
      LocalFile(name = "file4.txt", "another string containing the word me")
    )
    val search1 = Program.calculateMatchingPercentage("is for me", files)
    val search2 = Program.calculateMatchingPercentage("containing", files)
    val search3 = Program.calculateMatchingPercentage("for", files)
    val search4 = Program.calculateMatchingPercentage("non-existing", files)

    assert(search1.length === 4)
    assert(search1(0).name === "file1.txt" && search1(0).matchingPercentage === 100)
    assert(search1(1).name === "file2.txt" && search1(1).matchingPercentage === 100)
    assert(search1(2).name === "file3.txt" && search1(2).matchingPercentage === 33)
    assert(search1(3).name === "file4.txt" && search1(3).matchingPercentage === 33)

    assert(search2.length === 2)
    assert(search2(0).name === "file3.txt" && search2(0).matchingPercentage === 100)
    assert(search2(1).name === "file4.txt" && search2(1).matchingPercentage === 100)

    assert(search3.length === 3)
    assert(search3(0).name === "file1.txt" && search3(0).matchingPercentage === 100)
    assert(search3(1).name === "file2.txt" && search3(1).matchingPercentage === 100)
    assert(search3(2).name === "file3.txt" && search3(2).matchingPercentage === 100)

    assert(search4.length === 0)
  }

  test("Calculation should only present the 10 highest matching files") {
    val files = (1 to 20).toArray.map(int => LocalFile(name = "file" + int, "match"))
    val calculated = Program.calculateMatchingPercentage("match", files)

    assert(files.length === 20)
    assert(calculated.length === 10)
    calculated.map(file => assert(file.matchingPercentage === 100))
  }

  test("Should handle a File which contains both files and directories") {
    val dir = "./src/test/resources/files/tmp"
    val subFolder = "/subfolder"
    val fileName = "file.txt"
    val content = "the quick brown fox jumped over the lazy dog"

    // NOTE: Using Files.createDirectories(...) due to laziness to avoid
    // tests crashing since the directory already exists.

    // Create a directory
    val path = Paths.get(dir)
    Files.createDirectories(path)
    val directory = new File(dir)

    // Create an subfolder
    val subPath = Paths.get(dir + subFolder)
    Files.createDirectories(subPath)

    // Create the file
    val file = new File(dir + subFolder + "/" + fileName)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(content)
    bw.close()

    val index = Program.index(directory)

    index.files.map(localFile => assert(localFile === LocalFile(name = fileName, content = content)))
    assert(index.fileCount === 1)
  }

  test("Should present the top 10 matching files when a directory is provided with a given search string") {
    val dir = "./src/test/resources/files"
    val optFile = Program.readFile(Array(dir)).right.toOption

    assert(optFile.isDefined)

    val index = Program.index(optFile.get)
    val allFiles = index.files

    val search = Program.calculateMatchingPercentage("entered the", allFiles)

    assert(search.length === 10)

    val firstFileNames = List(
      "vol08.iss0001-0071.txt",
      "tr823.txt",
      "vol04.iss0064-0118.txt",
      "vol09.iss0050-0100.txt")
    compareIgnoringContent(100, search.take(4), firstFileNames)

    val followingFileNames = List(
      "adventur.txt",
      "intro.txt",
      "localedef.txt",
      "mtrace.txt",
      "memusagestat.txt",
      "codegeek.txt"
    )
    compareIgnoringContent(50, search.takeRight(6), followingFileNames)
  }
}

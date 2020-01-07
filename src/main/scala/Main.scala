import java.io.File
import scala.io.Source

object Main extends App {
  Program
    .readFile(args)
    .fold(println, file => Program.iterate(Program.index(file)))
}

object Program {
  import scala.io.StdIn.readLine

  // Initiate the matching percentage to some dummy number, will be
  // overwritten once the calculation of content match is done
  case class LocalFile(name: String, content: String, matchingPercentage: Int = -1)
  case class Index(fileCount: Int, files: Array[LocalFile])

  sealed trait ReadFileError

  case object MissingPathArg extends ReadFileError
  case class NotDirectory(error: String) extends ReadFileError
  case class FileNotFound(t: Throwable) extends ReadFileError

  def readFile(args: Array[String]): Either[ReadFileError, File] = {
    args.headOption.toRight(MissingPathArg) match {
      case Right(path) =>
        try {
          val file = new File(path)
          if (file.isDirectory)
            Right(file)
          else
            Left(NotDirectory(s"Path [​$path] is not a directory"))
        } catch {
          case e: NullPointerException =>
            Left(FileNotFound(e))
        }
      case Left(error) => Left(error)
    }
  }

  def index(file: File): Index = {
    def extractFiles(directory: File): Array[LocalFile] = {
      directory.listFiles().foldLeft(Array.empty[LocalFile]) { (localFiles, file) =>
        if (file.isDirectory) {
          localFiles ++ extractFiles(file)
        } else if (file.isFile) {
          if (file.canRead) {
            val source = Source.fromFile(file.getPath)

            val content = source.mkString
            source.close()

            localFiles :+ LocalFile(name = file.getName, content = content)
          } else {
            println(s"Error: file with name '${file.getName}' is not readable")
            localFiles
          }
        } else {
          println(s"Error: Unknown file type, neither a directory nor a file")
          localFiles
        }
      }
    }

    val files = extractFiles(file)
    val fileCount = files.length

    println(s"$fileCount files read in directory ${file.getPath}")
    Index(fileCount = fileCount, files = files)
  }

  def iterate(indexedFiles: Index): Unit = {
    print("search> ")
    val searchString = readLine()
    searchString match {
      case ":quit" =>
        // Finish the program
        System.exit(0)
      case "" =>
        println("Error: Search string cannot be empty, please provide a non-empty search string")
        iterate(indexedFiles)
      case str =>
        val topMatchingFiles = calculateMatchingPercentage(str, indexedFiles.files)

        topMatchingFiles match {
          case Array() =>
            println(s"No matches found for string '$searchString'")
          case fileArray =>
            fileArray.foreach(file => println(s"${file.matchingPercentage}% for file '${file.name}'"))
        }

        // Couldn't get this to work. Prints the "No matches found ..."
        // regardless of if there are matching files or not
//        topMatchingFiles.foldLeft(println(s"No matches found for string '$searchString'")) { (_, file) =>
//          println(s"${file.matchingPercentage}% for file '${file.name}'")
//        }

        iterate(indexedFiles)
    }
  }

  def calculateMatchingPercentage(searchString: String, files: Array[LocalFile]): Array[LocalFile] = {
    val results = files.map { localFile =>
      val delimiter = "\\s+"  // Split the string with whitespace characters

      // Convert iterables to Vectors to extract only unique elements
      val allContentWords = localFile.content.split(delimiter).toVector.map(_.toLowerCase).distinct
      val allSearchStringWords = searchString.split(delimiter).toVector.map(_.toLowerCase).distinct

      val matchingWords = allSearchStringWords.map(searchWord =>
        allContentWords.contains(searchWord)
      )

      val matchingPercentage = matchingWords.count(_ == true).toFloat / allSearchStringWords.size
      localFile.copy(matchingPercentage = (matchingPercentage * 100).toInt)
    }

    // Skim the files with highest percentage to the top of the
    // array and remove any files that has 0 % match percentage
    val sortedResults = results.filter(_.matchingPercentage > 0).sortWith(_.matchingPercentage > _.matchingPercentage)

    if (sortedResults.length > 10) {
      val arraySize = sortedResults.length
      sortedResults.dropRight(arraySize - 10)
    } else
      sortedResults
  }
}

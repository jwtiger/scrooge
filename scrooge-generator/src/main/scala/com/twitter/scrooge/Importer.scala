/*
 * Copyright 2011 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.scrooge

import java.io.{IOException, File}
import scala.collection.Map
import scala.io.Source

// an imported file should have its own path available for nested importing.
case class FileContents(importer: Importer, data: String)

// an Importer turns a filename into its string contents.
trait Importer extends (String => FileContents) {
  def paths: Seq[String] // for tests
  def lastModified(filename: String): Option[Long]
}

object Importer {
  def fileImporter(importPaths: Seq[String]): Importer = new Importer {
    val paths = List(".") ++ importPaths

    private[this] def withPath(path: String) = fileImporter(path +: importPaths)

    private def resolve(filename: String): (File, Importer) = {
      val f = new File(filename)
      val candidates = if (f.isAbsolute) {
        List(f)
      } else {
        paths map { path => new File(path, filename).getCanonicalFile }
      }

      candidates find {
        _.canRead
      } map { f =>
        (f, this.withPath(f.getParent()))
      } getOrElse {
        throw new IOException("Can't find file: " + filename)
      }
    }

    def lastModified(filename: String): Option[Long] = {
      val (f, i) = resolve(filename)
      Some(f.lastModified)
    }

    // find the requested file, and load it into a string.
    def apply(filename: String): FileContents = {
      val (f, i) = resolve(filename)
      FileContents(i, Source.fromFile(f).mkString)
    }
  }

  def fakeImporter(files: Map[String, String]) = new Importer {
    def paths = Seq()
    def lastModified(filename: String) = None
    def apply(filename: String): FileContents = {
      files.get(filename) map { FileContents(this, _) } getOrElse {
        throw new IOException("Can't find file: " + filename)
      }
    }
  }

  def resourceImporter(c: Class[_]) = new Importer {
    def paths = Seq()
    def lastModified(filename: String) = None
    def apply(filename: String): FileContents = {
      try {
        FileContents(this, Source.fromInputStream(c.getResourceAsStream(filename)).mkString)
      } catch {
        case e =>
          throw new IOException("Can't load resource: " + filename)
      }
    }
  }
}

/*
 * Copyright (C) 03/06/13 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package viabilitree

//import java.io._
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import viabilitree.kdtree.structure._
import com.thoughtworks.xstream._
import io.binary._
import better.files._
import viabilitree.model.Control
import cats._
import cats.implicits._
import viabilitree.viability.kernel.KernelComputation

import scala.util.Failure

package object export extends better.files.Implicits {

  implicit def stringToFile(s: String) = File(s)

 // implicit def stringToFile(s: String) = new File(s)

//  def save(o: AnyRef, output: File) = {
//    val xstream = new XStream(new BinaryStreamDriver)
//    val dest = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(output)))
//    try xstream.toXML(o, dest)
//    finally dest.close
//  }
//
//  def load[T](input: File) = {
//    val xstream = new XStream(new BinaryStreamDriver)
//    val source = new BufferedInputStream(new GZIPInputStream(new FileInputStream(input)))
//    try  xstream.fromXML(source).asInstanceOf[T]
//    finally source.close()
//  }
//
//  def traceTraj(t:Seq[Point], file: File): Unit = {
//    val output = Resource.fromFile(file)
//    t.foreach { p =>
//      output.writeStrings(p.map(_.toString), " ")
//      output.write("\n")
//    }
//  }

  /*  traceViabilityKernel in export
  => CONTENT <: (testPoint: Point, label: Boolean, control: Option(Int), ...)
  => save in a text file the content of each leaf L of a ViabilityKernel with label = TRUE
  => separator is a space
  => a line contains :
    the coordinates of the testPoint  (dim values) !!! testPoint is not centered in the region of the leaf L, it is centered on A leaf maximaly divided in the leaf L
    the min and max of each interval of the region delimited by the leaf (2*dim values)
    the coordinates of the control_th element in setU applied to point testPoint (Control.size values)

  Usage :
    traceViabilityKernel(aViabilityKernel,theCorrespondingModel.controls,s"fileName.txt")
    */

  def traceViabilityKernel[T](tree: Tree[T], label: T => Boolean, testPoint: T => Vector[Double], control: T => Option[Int], controls: Vector[Double] => Vector[Control], file: File): Unit = {
    file.delete(true)

    //todo add the first line in the .txt file, of the form x1 x2 ... x${dim} min1 max1 ... min${dim} max${dim} control1 ... control${aControl.size}
//    def header =
//      (0 until tree.dimension).map(d => s"x$d") ++
//        (0 until tree.dimension).map(d => s"min$d") ++
//        (0 until tree.dimension).map(d => s"max$d") ++
//        (0 until controls.size).map(c => s"control$c")

//    file << header.mkString(" ")

    tree.leaves.filter(l => label(l.content)).foreach {
      leaf =>
        val point = testPoint(leaf.content)
        val radius = leaf.zone.region
        val test = radius.flatMap(inter => Seq(inter.min, inter.max))
        val controlInx = control(leaf.content).get
        val controlValue = controls(point)(controlInx)
        val pointLString = point.map(x => x.toString)
        val radiusLString = radius.flatMap(inter => Seq(inter.min, inter.max))
        val controlLString = controlValue.value.map(c => c.toString)
        val uneLigne = pointLString ++ radiusLString ++ controlLString
        file << uneLigne.mkString(" ")
    }
  }

  def saveVTK2D[T](tree: Tree[T], label: T => Boolean, file: File): Unit = saveVTK2D(tree, label, file, 0, 1)

  def saveVTK2D[T](tree: Tree[T], label: T => Boolean, file: File, x: Int, y: Int): Unit = {
    file.delete(true)

    def coords =
      tree.leaves.filter(l => label(l.content)).map {
        l =>
          val intervals = l.zone.region
          assert(intervals.size == 2, s"Dimension of the space should be 2, found ${intervals.size}")
          val ix = intervals(x)
          val iy = intervals(y)
          Seq(ix.min, iy.min, ix.span, iy.span)
      }

    def points(l: Seq[Double]) = {
      val (x, y, dx, dy) = (l(0), l(1), l(2), l(3))
      val (xmax, ymax) = (x + dx, y + dy)

      List(
        List(x, y, 0.0),
        List(xmax, y, 0.0),
        List(x, ymax, 0.0),
        List(xmax, ymax, 0.0)
      )
    }

    val toWrite = coords.map(points)

    file.append("""# vtk DataFile Version 2.0
2D
ASCII
DATASET UNSTRUCTURED_GRID""")

    file.append(s"\nPOINTS ${toWrite.size * 4} float\n")

    toWrite.flatten.foreach {
      p =>
        file.append(p.mkString(" "))
        file.append("\n")
    }

    file.append(s"CELLS ${toWrite.size} ${toWrite.size * 5}\n")

    Iterator.iterate(0)(_ + 1).grouped(4).take(toWrite.size).foreach {
      c =>
        file.append("4 ")
        file.append(c.mkString(" "))
        file.append("\n")
    }

    file.append(s"CELL_TYPES ${toWrite.size}\n")

    (0 until toWrite.size).foreach { i => file.append(s"8 ") }

    file.append("\n")
  }

//    def saveVTK3D[T <: Label](tree: NonEmptyTree[T], file: File): Unit = saveVTK3D(tree, file, 0, 1, 2)
//
//    def saveVTK3D[T <: Label](tree: NonEmptyTree[T], file: File, x: Int, y: Int, z: Int): Unit = {
//      file.delete()
//      val output = Resource.fromFile(file)
//
//      def coords =
//        tree.leaves.filter(_.content.label).map {
//          l =>
//            val intervals = l.zone.region
//            assert(intervals.size == 3, s"Dimension of the space should be 3, found ${intervals.size}")
//            val ix = intervals(x)
//            val iy = intervals(y)
//            val iz = intervals(z)
//            Seq(ix.min, iy.min, iz.min, ix.span, iy.span, iz.span)
//        }
//
//      def points(l: Seq[Double]) = {
//        val (x, y, z, dx, dy, dz) = (l(0), l(1), l(2), l(3), l(4), l(5))
//        val (xmax, ymax, zmax) = (x + dx, y + dy, z + dz)
//
//        List(
//          List(x, y, z),
//          List(xmax, y, z),
//          List(xmax, ymax, z),
//          List(x, ymax, z),
//          List(x, y, zmax),
//          List(xmax, y, zmax),
//          List(xmax, ymax, zmax),
//          List(x, ymax, zmax)
//        )
//      }
//
//      val toWrite = coords.map(points)
//
//      output.write("""# vtk DataFile Version 2.0
//Prof 18 slice 1
//ASCII
//DATASET UNSTRUCTURED_GRID""")
//
//      output.write(s"\nPOINTS ${toWrite.size * 8} float\n")
//
//      toWrite.flatten.foreach {
//        p =>
//          output.writeStrings(p.map(_.toString), " ")
//          output.write("\n")
//      }
//
//      output.write(s"CELLS ${toWrite.size} ${toWrite.size * 9}\n")
//
//      Iterator.iterate(0)(_ + 1).grouped(8).take(toWrite.size).foreach {
//        c =>
//          output.write("8 ")
//          output.writeStrings(c.map(_.toString), " ")
//          output.write("\n")
//      }
//
//      output.write(s"CELL_TYPES ${toWrite.size}\n")
//      (0 until toWrite.size).foreach {
//        i => output.write(s"12 ")
//      }
//      output.write("\n")
//    }

}


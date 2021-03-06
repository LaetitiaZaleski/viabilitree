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

import better.files._
<<<<<<< HEAD
import com.thoughtworks.xstream._
import com.thoughtworks.xstream.io.binary._
import viabilitree.approximation.OracleApproximation
import viabilitree.kdtree._
=======
import cats._
import cats.implicits._
import viabilitree.approximation.{ OracleApproximation, OracleApproximationContent }
import viabilitree.viability.basin.BasinComputation
>>>>>>> ISCPIF/master

import scala.io.Source
import scala.math.floor

package object export extends better.files.Implicits {

  implicit def stringToFile(s: String) = File(s)

  // implicit def stringToFile(s: String) = new File(s)

  def save(o: AnyRef, output: File) = {
    output.parent.createDirectories()
    import java.io._
    import java.util.zip._
    val xstream = new XStream(new BinaryStreamDriver)
    val dest = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(output.toJava)))
    try xstream.toXML(o, dest)
    finally dest.close
  }

  def load[T](input: File) = {
    import java.io._
    import java.util.zip._
    val xstream = new XStream(new BinaryStreamDriver)
    val source = new BufferedInputStream(new GZIPInputStream(new FileInputStream(input.toJava)))
    try xstream.fromXML(source).asInstanceOf[T]
    finally source.close()
  }

<<<<<<< HEAD
  var oriX =1.0
  var oriY=1.0
  var oppX=1.0
  var oppY=1.0
  var ppa=1.0
  var X=1.0
  var Y=1.0

  def oracleFromFile(input: String)= (p: Vector[Double]) => { // A partir d'un fichier (au format vino) renvoit un oracle
    val filename = input
    val bufferedSource = Source.fromFile(filename)
    var i = 0
    var inOrOut = false


    for (line <- bufferedSource.getLines()) {
      if (i==0){ //origine
        val ori = line.split(",")
         oriX = ori(0).toFloat
         oriY = ori(1).toFloat
      }
      if (i==1){ // opposite
        val opp = line.split(",")
         oppX = opp(0).toFloat
         oppY = opp(1).toFloat
      }
      if (i==2){ //ppa
         ppa = line.toFloat
        val hiX = (oppX - oriX) / ppa // hi : taille d'un intervalle
        val hiY = (oppY - oriY) / ppa
        val p0 = p(0) - oriX
        val p1 = p(1) - oriY //on leur enleve l'origine pour avoir la meme box
        X = floor(p0 / hiX)
        Y = floor(p1 / hiY)
      }
      if (i > 2) {
        val bar = line.split(",")
        if (bar(0).toInt == X && bar(1).toInt == Y) {

          // println("in : "+bar(0)+","+bar(1))
          inOrOut = true
        }
      }
      i = i + 1
    }
    //println("out")
    inOrOut
  }


  def boxFromFile (input: String) : Zone = {// A partir d'un fichier (au format vino) renvoit une box
  val filename = input
    val bufferedSource = Source.fromFile(filename)
    val lines = bufferedSource.getLines.toList
    bufferedSource.close
    val x_ori = lines(0).split(",")
    val x_opp = lines(1).split(",")
    val x_oriX = x_ori(0).toDouble
    val x_oriY =  x_ori(1).toDouble
    val x_oppX = x_opp(0).toDouble
    val x_oppY =  x_opp(1).toDouble
    println("box: ("+x_oriX+","+x_oppX+");("+x_oriY+","+x_oppY+")")
    Vector(
      (x_oriX, x_oppX),
      (x_oriY, x_oppY))

  }

//  def traceTraj(t:Seq[Point], file: File): Unit = {
//    val output = Resource.fromFile(file)
//    t.foreach { p =>
//      output.writeStrings(p.map(_.toString), " ")
//      output.write("\n")
//    }
//  }

=======
  def traceTraj(t: Seq[Vector[Double]], file: File): Unit = {
    file.delete(true)
    file.parent.createDirectories()
    file.touch()
    t.foreach { p =>
      file.append(p.mkString(" "))
      file.append(s"\n")
    }
  }
>>>>>>> ISCPIF/master

  /* ------------------------- Hyper-rectangles ---------------------- */

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

  //  def traceViabilityKernel[T](tree: Tree[T], label: T => Boolean, testPoint: T => Vector[Double], control: T => Option[Int], controls: Vector[Double] => Vector[Control], file: File): Unit = {
  //    file.delete(true)
  //
  //    //todo add the first line in the .txt file, of the form x1 x2 ... x${dim} min1 max1 ... min${dim} max${dim} control1 ... control${aControl.size}
  ////    def header =
  ////      (0 until tree.dimension).map(d => s"x$d") ++
  ////        (0 until tree.dimension).map(d => s"min$d") ++
  ////        (0 until tree.dimension).map(d => s"max$d") ++
  ////        (0 until controls.size).map(c => s"control$c")
  //
  ////    file << header.mkString(" ")
  //
  //    tree.leaves.filter(l => label(l.content)).foreach {
  //      leaf =>
  //        val point = testPoint(leaf.content)
  //        val radius = leaf.zone.region
  //        val test = radius.flatMap(inter => Seq(inter.min, inter.max))
  //        val controlInx = control(leaf.content).get
  //        val controlValue = controls(point)(controlInx)
  //        val pointLString = point.map(x => x.toString)
  //        val radiusLString = radius.flatMap(inter => Seq(inter.min, inter.max))
  //        val controlLString = controlValue.value.map(c => c.toString)
  //        val uneLigne = pointLString ++ radiusLString ++ controlLString
  //        file << uneLigne.mkString(" ")
  //    }
  //  }

  object HyperRectangles {

    import viabilitree.viability._
    import viabilitree.viability.kernel._

    def intervals(zone: Zone) = zone.region.flatMap(inter => Seq(inter.min, inter.max))

    def viabilityControlledDynamic(kernelComputation: KernelComputation) =
      viabilityKernelColumns(
        KernelContent.label.get,
        KernelContent.testPoint.get,
        KernelContent.control.get,
        kernelComputation.controls)

    def viabilityKernelColumns[T](label: T => Boolean, testPoint: T => Vector[Double], control: T => Option[Int], controls: Vector[Double] => Vector[Control]) = (t: T, zone: Zone) =>
      (label(t), control(t)) match {
        case (true, Some(c)) =>
          val p = testPoint(t)
          Some(p ++ intervals(zone) ++ controls(p).apply(c).value).map(_.map(_.toString))
        case (true, None) =>
          val p = testPoint(t)
          Some(p ++ intervals(zone)).map(_.map(_.toString))
        case _ => None
      }

    object Traceable {

      implicit def kernelComputation = new Traceable[KernelComputation, viability.kernel.KernelContent] {
        override def columns(t: KernelComputation) =
          HyperRectangles.viabilityKernelColumns[KernelContent](
            KernelContent.label.get,
            KernelContent.testPoint.get,
            KernelContent.control.get,
            t.controls)
      }

      implicit def basinComputation = new Traceable[BasinComputation, viability.basin.BasinContent] {
        override def columns(t: BasinComputation) =
          (content: viabilitree.viability.basin.BasinContent, zone: Zone) =>
            content.label match {
              case true =>
                def c = (content.testPoint ++ intervals(zone) ++ content.control).map(_.toString)
                Some(c)
              case false => None
            }
      }

      implicit def oracleApproximation = new Traceable[OracleApproximation, OracleApproximationContent] {
        override def columns(t: OracleApproximation) =
          (content: OracleApproximationContent, zone: Zone) =>
            content.label match {
              case true =>
                def c = (content.testPoint ++ intervals(zone)).map(_.toString)
                Some(c)
              case false => None
            }
      }

      implicit def distanceTuple[U] = new Traceable[U, (Double, Path)] {
        override def columns(t: U) =
          (content, zone) => Some(Vector(content._1.toString))
      }

      implicit def tupleTraceable[U, T1, T2](implicit traceable1: Traceable[U, T1], traceable2: Traceable[U, T2]) = new Traceable[U, (T1, T2)] {
        override def columns(t: U) = {
          (content, zone) =>
            (traceable1.columns(t)(content._1, zone), traceable2.columns(t)(content._2, zone)) match {
              case (None, _) => None
              case (_, None) => None
              case (Some(a), Some(b)) => Some(a ++ b)
            }
        }
      }
    }

    trait Traceable[T, C] {
      def columns(t: T): (C, Zone) => Option[Vector[String]]
    }

  }

  def saveHyperRectangles[T, C](t: T)(tree: Tree[C], file: File)(implicit traceable: HyperRectangles.Traceable[T, C]): Unit =
    saveHyperRectangles(tree, traceable.columns(t), file)

  def saveHyperRectangles[T](tree: Tree[T], columns: (T, Zone) => Option[Vector[String]], file: File): Unit = {
    file.delete(true)
    file.parent.createDirectories()
    file.touch()

    //todo add the first line in the .txt file, of the form x1 x2 ... x${dim} min1 max1 ... min${dim} max${dim} control1 ... control${aControl.size}
    //    def header =
    //      (0 until tree.dimension).map(d => s"x$d") ++
    //        (0 until tree.dimension).map(d => s"min$d") ++
    //        (0 until tree.dimension).map(d => s"max$d") ++
    //        (0 until controls.size).map(c => s"control$c")

    //    file << header.mkString(" ")

    tree.leaves.flatMap(l => columns(l.content, l.zone).toVector).foreach {
      cols => file << cols.mkString(" ")
    }
  }

  /* ------------------------- VTK ---------------------- */

  object VTK {

    object Traceable {

      implicit def kernelContentDynamicIsTraceable[T](implicit l: ContainsLabel[T]) = new Traceable[T] {
        def label = l.label
      }

      implicit def oracleAproximationIsTraceable = new Traceable[OracleApproximationContent] {
        def label = OracleApproximationContent.label.get
      }

    }

    trait Traceable[C] {
      def label: C => Boolean
    }
  }

  def saveVTK2D[T](tree: Tree[T], file: File, x: Int = 0, y: Int = 1)(implicit traceable: VTK.Traceable[T]): Unit =
    saveVTK2D(tree, traceable.label, file, x, y)

  def saveVTK2D[T](tree: Tree[T], label: T => Boolean, file: File, x: Int, y: Int): Unit = {
    file.delete(true)
    file.parent.createDirectories()

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
        List(xmax, ymax, 0.0))
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

  def saveVTK3D[T](tree: Tree[T], file: File, x: Int = 0, y: Int = 1, z: Int = 2)(implicit traceable: VTK.Traceable[T]): Unit =
    saveVTK3D(tree, traceable.label, file, x, y, z)

  def saveVTK3D[T](tree: Tree[T], label: T => Boolean, file: File, x: Int, y: Int, z: Int): Unit = {
    file.delete(true)
    file.parent.createDirectories()

    def coords =
      tree.leaves.filter(l => label(l.content)).map {
        l =>
          val intervals = l.zone.region
          assert(intervals.size == 3, s"Dimension of the space should be 3, found ${intervals.size}")
          val ix = intervals(x)
          val iy = intervals(y)
          val iz = intervals(z)
          Seq(ix.min, iy.min, iz.min, ix.span, iy.span, iz.span)
      }

    def points(l: Seq[Double]) = {
      val (x, y, z, dx, dy, dz) = (l(0), l(1), l(2), l(3), l(4), l(5))
      val (xmax, ymax, zmax) = (x + dx, y + dy, z + dz)

      List(
        List(x, y, z),
        List(xmax, y, z),
        List(xmax, ymax, z),
        List(x, ymax, z),
        List(x, y, zmax),
        List(xmax, y, zmax),
        List(xmax, ymax, zmax),
        List(x, ymax, zmax))
    }

    val toWrite = coords.map(points)

    file << """# vtk DataFile Version 2.0
Prof 18 slice 1
ASCII
DATASET UNSTRUCTURED_GRID"""

    file << s"POINTS ${toWrite.size * 8} float"

    toWrite.flatten.foreach {
      p => file << p.mkString(" ")
    }

    file << s"CELLS ${toWrite.size} ${toWrite.size * 9}\n"

    Iterator.iterate(0)(_ + 1).grouped(8).take(toWrite.size).foreach {
      c => file << s"""8 ${c.mkString(" ")}"""
    }

    file << s"CELL_TYPES ${toWrite.size}\n"
    file << Vector.fill(toWrite.size)("12").mkString(" ")
  }

}

